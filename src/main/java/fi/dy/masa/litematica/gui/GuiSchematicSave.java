package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiSchematicSave extends GuiSchematicSaveBase implements ICompletionListener
{
    private final SelectionManager selectionManager;

    public GuiSchematicSave()
    {
        this(null);
    }

    public GuiSchematicSave(@Nullable LitematicaSchematic schematic)
    {
        super(schematic);

        if (schematic != null)
        {
            this.title = I18n.format("litematica.gui.title.save_schematic_from_memory");
        }
        else
        {
            this.title = I18n.format("litematica.gui.title.create_schematic_from_selection");
        }

        this.selectionManager = DataManager.getSelectionManager();

        AreaSelection area = this.selectionManager.getCurrentSelection();

        if (area != null)
        {
            this.defaultText = area.getName();
        }
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_save";
    }

    @Override
    public File getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected IButtonActionListener<ButtonGeneric> createButtonListener(ButtonType type)
    {
        return new ButtonListener(type, this.selectionManager, this);
    }

    @Override
    public void onTaskCompleted()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.isCallingFromMinecraftThread())
        {
            this.refreshList();
        }
        else
        {
            mc.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    GuiSchematicSave.this.refreshList();
                }
            });
        }
    }

    private void refreshList()
    {
        GuiScreen gui = this.mc.currentScreen;

        if (gui == this)
        {
            this.getListWidget().refreshEntries();
            this.getListWidget().clearSchematicMetadataCache();
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiSchematicSave gui;
        private final SelectionManager selectionManager;
        private final ButtonType type;

        public ButtonListener(ButtonType type, SelectionManager selectionManager, GuiSchematicSave gui)
        {
            this.type = type;
            this.selectionManager = selectionManager;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == ButtonType.SAVE)
            {
                File dir = this.gui.getListWidget().getCurrentDirectory();
                String fileName = this.gui.getTextFieldText();

                if (dir.isDirectory() == false)
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_directory", dir.getAbsolutePath());
                    return;
                }

                if (fileName.isEmpty())
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_schematic_name", fileName);
                    return;
                }

                // Saving a schematic from memory
                if (this.gui.schematic != null)
                {
                    LitematicaSchematic schematic = this.gui.schematic;
                    schematic.getMetadata().setTimeModified(System.currentTimeMillis());

                    if (schematic.writeToFile(dir, fileName, GuiScreen.isShiftKeyDown()))
                    {
                        this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", fileName);
                        this.gui.getListWidget().refreshEntries();
                    }
                }
                else
                {
                    AreaSelection area = this.selectionManager.getCurrentSelection();

                    if (area != null)
                    {
                        boolean overwrite = GuiScreen.isShiftKeyDown();
                        String fileNameTmp = fileName;

                        // The file name extension gets added in the schematic write method, so need to add it here for the check
                        if (fileNameTmp.endsWith(LitematicaSchematic.FILE_EXTENSION) == false)
                        {
                            fileNameTmp += LitematicaSchematic.FILE_EXTENSION;
                        }

                        if (FileUtils.canWriteToFile(dir, fileNameTmp, overwrite) == false)
                        {
                            this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exists", fileNameTmp);
                            return;
                        }

                        String author = this.gui.mc.player.getName();
                        boolean takeEntities = this.gui.checkboxIgnoreEntities.isChecked() == false;
                        LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, author);
                        TaskSaveSchematic task = new TaskSaveSchematic(dir, fileName, schematic, area, takeEntities, overwrite);
                        task.setCompletionListener(this.gui);
                        TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
                        this.gui.addMessage(MessageType.INFO, "litematica.message.schematic_save_task_created");
                    }
                    else
                    {
                        this.gui.addMessage(MessageType.ERROR, "litematica.message.error.schematic_save_no_area_selected");
                    }
                }
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }
    }

    public static class InMemorySchematicCreator implements IStringConsumer
    {
        private final AreaSelection area;
        private final Minecraft mc;

        public InMemorySchematicCreator(AreaSelection area)
        {
            this.area = area;
            this.mc = Minecraft.getMinecraft();
        }

        @Override
        public void setString(String string)
        {
            boolean takeEntities = true; // TODO
            String author = this.mc.player.getName();
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(this.area, author);

            if (schematic != null)
            {
                schematic.getMetadata().setName(string);
                TaskSaveSchematic task = new TaskSaveSchematic(schematic, this.area, takeEntities);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
            }
        }
    }
}
