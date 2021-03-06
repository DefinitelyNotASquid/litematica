package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorNormal;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class WidgetAreaSelectionEntry extends WidgetDirectoryEntry
{
    private final SelectionManager selectionManager;
    private final WidgetAreaSelectionBrowser parent;
    private int buttonsStartX;

    public WidgetAreaSelectionEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            DirectoryEntry entry, int listIndex, SelectionManager selectionManager, Minecraft mc,
            WidgetAreaSelectionBrowser parent, IFileBrowserIconProvider iconProvider)
    {
        super(x, y, width, height, zLevel, isOdd, entry, listIndex, mc, parent, iconProvider);

        this.selectionManager = selectionManager;
        this.parent = parent;

        int posX = x + width - 2;
        int posY = y + 1;

        // Note: These are placed from right to left

        if (entry.getType() == DirectoryEntryType.FILE && FileType.fromFile(entry.getFullPath()) == FileType.JSON)
        {
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.REMOVE);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.RENAME);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.COPY);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.CONFIGURE);
        }

        this.buttonsStartX = posX;
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = I18n.format(type.getLabelKey());
        int len = Math.max(this.mc.fontRenderer.getStringWidth(label) + 10, 20);
        x -= len;
        this.addButton(new ButtonGeneric(x, y, len, 20, label), new ButtonListener(type, this.selectionManager, this));

        return x - 2;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        if (this.entry.getType() == DirectoryEntryType.FILE && FileType.fromFile(this.entry.getFullPath()) == FileType.JSON)
        {
            selected = this.entry.getFullPath().getAbsolutePath().equals(this.selectionManager.getCurrentNormalSelectionId());
            super.render(mouseX, mouseY, selected);
        }
        else
        {
            super.render(mouseX, mouseY, selected);
        }
    }

    @Override
    protected String getDisplayName()
    {
        if (this.entry.getType() == DirectoryEntryType.FILE && FileType.fromFile(this.entry.getFullPath()) == FileType.JSON)
        {
            AreaSelection selection = this.selectionManager.getOrLoadSelectionReadOnly(this.getDirectoryEntry().getFullPath().getAbsolutePath());
            String prefix = this.entry.getDisplayNamePrefix();
            return selection != null ? (prefix != null ? prefix + selection.getName() : selection.getName()) : "<error>";
        }

        return super.getDisplayName();
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        List<String> text = new ArrayList<>();
        AreaSelection selection = this.selectionManager.getOrLoadSelectionReadOnly(this.getDirectoryEntry().getFullPath().getAbsolutePath());

        if (selection != null)
        {
            String str;
            BlockPos o = selection.getExplicitOrigin();

            if (o == null)
            {
                o = selection.getEffectiveOrigin();
                str = I18n.format("litematica.gui.label.origin.auto");
            }
            else
            {
                str = I18n.format("litematica.gui.label.origin.manual");
            }

            String strOrigin = String.format("x: %d, y: %d, z: %d (%s)", o.getX(), o.getY(), o.getZ(), str);
            text.add(I18n.format("litematica.gui.label.area_selection_origin", strOrigin));

            int count = selection.getAllSubRegionBoxes().size();
            text.add(I18n.format("litematica.gui.label.area_selection_box_count", count));
        }

        int offset = 12;

        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - offset, this.height))
        {
            RenderUtils.drawHoverText(mouseX, mouseY, text);
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final WidgetAreaSelectionEntry widget;
        private final SelectionManager selectionManager;
        private final ButtonType type;

        public ButtonListener(ButtonType type, SelectionManager selectionManager, WidgetAreaSelectionEntry widget)
        {
            this.type = type;
            this.selectionManager = selectionManager;
            this.widget = widget;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            String selectionId = this.widget.getDirectoryEntry().getFullPath().getAbsolutePath();

            if (this.type == ButtonType.RENAME)
            {
                String title = "litematica.gui.title.rename_area_selection";
                AreaSelection selection = this.selectionManager.getSelection(selectionId);
                String name = selection != null ? selection.getName() : "<error>";
                SelectionRenamer renamer = new SelectionRenamer(this.selectionManager, this.widget, false);
                this.widget.mc.displayGuiScreen(new GuiTextInputFeedback(160, title, name, this.widget.parent.getSelectionManagerGui(), renamer));
            }
            else if (this.type == ButtonType.COPY)
            {
                AreaSelection selection = this.selectionManager.getSelection(selectionId);

                if (selection != null)
                {
                    String title = I18n.format("litematica.gui.title.copy_area_selection", selection.getName());
                    SelectionRenamer renamer = new SelectionRenamer(this.selectionManager, this.widget, true);
                    this.widget.mc.displayGuiScreen(new GuiTextInputFeedback(160, title, selection.getName(), this.widget.parent.getSelectionManagerGui(), renamer));
                }
                else
                {
                    this.widget.parent.getSelectionManagerGui().addMessage(MessageType.ERROR, "litematica.error.area_selection.failed_to_load");
                }
            }
            else if (this.type == ButtonType.REMOVE)
            {
                this.selectionManager.removeSelection(selectionId);
            }
            else if (this.type == ButtonType.CONFIGURE)
            {
                AreaSelection selection = this.selectionManager.getOrLoadSelection(selectionId);

                if (selection != null)
                {
                    GuiAreaSelectionEditorNormal gui = new GuiAreaSelectionEditorNormal(selection);
                    gui.setParent(this.widget.mc.currentScreen);
                    gui.setSelectionId(selectionId);
                    this.widget.mc.displayGuiScreen(gui);
                }
            }

            this.widget.parent.refreshEntries();
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            RENAME          ("litematica.gui.button.rename"),
            COPY            ("litematica.gui.button.copy"),
            CONFIGURE       ("litematica.gui.button.configure"),
            REMOVE          (TextFormatting.RED.toString() + "-");

            private final String labelKey;

            private ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }
        }
    }

    private static class SelectionRenamer implements IStringConsumerFeedback
    {
        private final WidgetAreaSelectionEntry widget;
        private final SelectionManager selectionManager;
        private final boolean copy;

        public SelectionRenamer(SelectionManager selectionManager, WidgetAreaSelectionEntry widget, boolean copy)
        {
            this.widget = widget;
            this.selectionManager = selectionManager;
            this.copy = copy;
        }

        @Override
        public boolean setString(String string)
        {
            String selectionId = this.widget.getDirectoryEntry().getFullPath().getAbsolutePath();
            return this.selectionManager.renameSelection(this.widget.getDirectoryEntry().getDirectory(), selectionId, string, this.copy, this.widget.parent.getSelectionManagerGui());
        }
    }
}
