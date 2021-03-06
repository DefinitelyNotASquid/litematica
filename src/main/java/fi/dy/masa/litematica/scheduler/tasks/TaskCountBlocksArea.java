package fi.dy.masa.litematica.scheduler.tasks;

import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.selection.AreaSelection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class TaskCountBlocksArea extends TaskCountBlocksBase
{
    protected final AreaSelection selection;

    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList);

        this.selection = selection;
        this.addBoxesPerChunks(selection.getAllSubRegionBoxes());
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    protected void countAtPosition(BlockPos pos)
    {
        IBlockState stateClient = this.worldClient.getBlockState(pos).getActualState(this.worldClient, pos);
        this.countsTotal.addTo(stateClient, 1);
    }
}
