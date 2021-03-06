package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.init.Blocks;

public class TaskDeleteArea extends TaskFillArea
{
    public TaskDeleteArea(List<Box> boxes, boolean removeEntities)
    {
        super(boxes, Blocks.AIR.getDefaultState(), null, removeEntities);
    }

    @Override
    protected void onStop()
    {
        if (this.finished)
        {
            InfoUtils.showGuiMessage(MessageType.SUCCESS, "litematica.message.area_cleared");
        }
        else
        {
            InfoUtils.showGuiMessage(MessageType.ERROR, "litematica.message.error.area_deletion_aborted");
        }

        if (this.isClientWorld && this.mc.player != null)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback true");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);
    }
}
