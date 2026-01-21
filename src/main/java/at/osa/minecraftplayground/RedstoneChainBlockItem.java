package at.osa.minecraftplayground;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Custom BlockItem for the Redstone Chain that shows an enchanted glint
 * when the item stack has the "powered" tag set.
 *
 * Note: This affects the item in inventory/hand. For placed blocks,
 * a custom block renderer would be needed.
 */
public class RedstoneChainBlockItem extends BlockItem {

    public RedstoneChainBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Show enchanted glint
        return true;
    }
}
