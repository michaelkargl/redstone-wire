package at.osa.minecraftplayground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * A redstone chain block that can transmit redstone signals.
 * <p>
 * Supports two types of connections:
 * 1. Traditional adjacent connections (touching blocks)
 * 2. Long-range cable connections via RedstoneChainEntity (up to 24 blocks)
 * <p>
 * Power transmission:
 * - POWER property stores signal strength (0-15, like vanilla redstone)
 * - Acts like redstone wire (indirect power only, no direct power)
 * - Comparators can read the power level
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class RedstoneChainBlock extends Block implements EntityBlock {

    /**
     * Block state property storing redstone power level (0-15).
     * 0 = unpowered, 15 = maximum power.
     */
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    /**
     * Visual and collision shape of the block.
     * A thin vertical pole (3x16x3 pixels) instead of a full cube.
     * Coordinates: (6.5, 0, 6.5) to (9.5, 16, 9.5)
     */
    private static final VoxelShape SHAPE = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);

    /**
     * Constructor for the RedstoneChainBlock.
     * <p>
     * This initializes a new redstone chain block with the given properties.
     * The block is set to not occlude (block) other blocks, allowing things like light to pass through.
     * The default state is registered with a POWER value of 0, meaning the block starts unpowered.
     *
     * @param properties The block properties (like hardness, sound type, etc.)
     */
    public RedstoneChainBlock(Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    /**
     * Defines which properties this block's state can have.
     * <p>
     * In Minecraft, blocks can have various "states" (like whether a door is open or closed).
     * This method tells Minecraft that our RedstoneChainBlock has one state property: POWER.
     * POWER ranges from 0-15, just like regular redstone wire.
     *
     * @param builder The builder object used to register state properties
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    /**
     * Defines the physical shape and collision box of this block.
     * <p>
     * This method returns a VoxelShape that determines:
     * 1. How the block looks visually (its hitbox outline)
     * 2. What parts of the block you can collide with
     * 3. Where you can place other blocks around it
     * <p>
     * Our chain block uses a thin vertical shape (like a chain segment or pole) defined in SHAPE.
     * The coordinates are in pixels: from (6.5, 0, 6.5) to (9.5, 16, 9.5), making it a 3x16x3 pixel column.
     *
     * @param state   The current state of the block
     * @param level   The world/level the block is in
     * @param pos     The position of the block
     * @param context Additional context for shape calculation
     * @return The VoxelShape representing the block's physical bounds
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Tells Minecraft whether this block is capable of emitting redstone signals.
     * <p>
     * This is called by the game engine to determine if this block should be considered
     * when calculating redstone power in circuits. It's like asking "can this block turn on redstone?"
     * <p>
     * We return true only if the block's POWER value is greater than 0, meaning it's currently
     * carrying a redstone signal. This allows other redstone components (like lamps, pistons, etc.)
     * to be powered by our chain block.
     *
     * @param state The current state of the block
     * @return true if the block has power > 0, false otherwise
     */
    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWER) > 0;
    }

    /**
     * Returns the indirect redstone signal strength this block emits.
     * <p>
     * IMPORTANT NOTE: Minecraft's redstone API has a quirk - the 'direction' parameter is BACKWARDS!
     * If something asks for signal from the NORTH, this method receives Direction.SOUTH.
     * This is because Minecraft asks "what signal is coming FROM that direction?" not "TO that direction".
     * <p>
     * This method does the following:
     * 1. First, it checks if this block has a RedstoneChainEntity (block entity)
     * 2. If it does, it returns the signal stored in the entity (which manages wire connections)
     * 3. If not, it falls back to returning the POWER value from the block state
     * <p>
     * The indirect signal (this method) can power things through solid blocks, like how redstone
     * on top of a block can power a piston below it.
     *
     * @param state     The current state of the block
     * @param level     The world/level the block is in
     * @param pos       The position of the block
     * @param direction The direction being queried (NOTE: this is inverted/backwards!)
     * @return The signal strength (0-15) this block emits
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Check if we have a block entity with wire connections
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneChainEntity chain) {
            return chain.getSignal();
        }
        return state.getValue(POWER);
    }

    /**
     * Returns the direct redstone signal strength this block emits.
     * <p>
     * IMPORTANT NOTE: Like getSignal(), the 'direction' parameter is BACKWARDS!
     * See the documentation for getSignal() for details about this quirk.
     * <p>
     * Direct signals are different from indirect signals:
     * - Direct signals (this method) can ONLY power things they're directly touching
     * - Indirect signals (getSignal) can power through solid blocks
     * <p>
     * For example, a redstone torch provides a direct signal, while redstone dust provides
     * an indirect signal. Our RedstoneChainBlock intentionally returns 0 here, meaning it
     * only provides indirect power (like redstone dust). This means:
     * - It CAN power a lamp through a solid block
     * - It CANNOT directly power redstone components placed against it
     * <p>
     * We keep this at 0 to mimic vanilla redstone wire behavior.
     *
     * @param state     The current state of the block
     * @param level     The world/level the block is in
     * @param pos       The position of the block
     * @param direction The direction being queried (NOTE: this is inverted/backwards!)
     * @return Always returns 0 (no direct signal)
     */
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        int indirectPower = 0;
        return indirectPower;
    }

    /**
     * Called when a neighboring block changes (placed, broken, or updated).
     * <p>
     * This is one of the most important methods for redstone components! Whenever a block
     * next to this one changes, Minecraft calls this method to let us react.
     * <p>
     * What happens inside:
     * 1. We first check if we're on the server side (!level.isClientSide) - redstone logic
     * should only run on the server, not the client
     * 2. We check if the neighbor that changed is NOT another RedstoneChainBlock - this prevents
     * feedback loops where chain blocks keep updating each other endlessly
     * 3. We try to get the BlockEntity for this position
     * 4. If we have a RedstoneChainEntity (meaning this block has wire connections):
     * - We call updateSignalInNetwork() which will recalculate power across all connected wires
     * 5. If we don't have a BlockEntity (traditional adjacent-only connections):
     * - We schedule a tick (delayed update) to recalculate power in 1 game tick
     * <p>
     * The delayed tick prevents performance issues when many blocks change at once.
     *
     * @param state         The current state of this block
     * @param level         The world/level the block is in
     * @param pos           The position of this block
     * @param neighborBlock The block that changed nearby
     * @param neighborPos   The position of the block that changed
     * @param movedByPiston Whether this was triggered by a piston
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && !(neighborBlock instanceof RedstoneChainBlock)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneChainEntity chain) {
                // Use the network update for wire-connected blocks
                chain.updateSignalInNetwork();
            } else {
                // Fallback to traditional adjacent block behavior
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    /**
     * Called when this block is placed in the world.
     * <p>
     * This method is triggered whenever a player places this block or it's created through
     * other means (like commands, world generation, etc.).
     * <p>
     * What happens inside:
     * 1. Check if we're on the server (!level.isClientSide) - only do logic server-side
     * 2. Check if the new state is actually different from the old state (!state.is(oldState.getBlock()))
     * - This prevents unnecessary updates when a block replaces itself (like during updates)
     * 3. If both conditions are true, schedule a tick in 1 game tick
     * - This delayed update gives the block time to initialize properly
     * - Then it will check for redstone power from neighbors and update accordingly
     * <p>
     * The delayed tick ensures that when you place a chain block next to powered redstone,
     * it will properly detect and adopt that power level after placement.
     *
     * @param state         The new state of this block
     * @param level         The world/level the block is in
     * @param pos           The position where the block was placed
     * @param oldState      The state that was previously at this position
     * @param movedByPiston Whether this block was moved by a piston
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /**
     * Called when this block's scheduled tick executes.
     * <p>
     * This method is triggered when a scheduled tick (from neighborChanged or onPlace) fires.
     * It handles updating the redstone power for blocks using traditional adjacent connections
     * (not wire connections).
     * <p>
     * What happens inside:
     * 1. First check if this block has a RedstoneChainEntity
     * - If it does, return immediately - the entity handles its own network updates
     * 2. For traditional adjacent connections:
     * a. Call findNetwork() to discover all connected RedstoneChainBlocks
     * - This uses a breadth-first search to find all touching chain blocks
     * b. Call findPower() to determine the maximum power from any neighbor
     * - Checks all neighbors of every block in the network
     * - Returns the highest power level found (0-15)
     * c. Update all blocks in the network to have the same power level
     * - This ensures connected chain blocks share the same power
     * d. If any block changed, notify all neighbors
     * - This propagates the signal to adjacent redstone components
     * <p>
     * This creates behavior similar to vanilla redstone wire, where connected
     * blocks form a network that shares the same power level.
     *
     * @param state  The current state of this block
     * @param level  The server level (world)
     * @param pos    The position of this block
     * @param random A random source (unused here, but required by the method signature)
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Check if we have a block entity - if so, let it handle updates
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneChainEntity) {
            return; // Let the entity handle network updates
        }

        // Traditional adjacent block network
        Set<BlockPos> network = findNetwork(level, pos);
        int power = findPower(level, network);

        boolean changed = false;
        for (BlockPos p : network) {
            BlockState s = level.getBlockState(p);
            if (s.getValue(POWER) != power) {
                level.setBlock(p, s.setValue(POWER, power), 2);
                changed = true;
            }
        }

        if (changed) {
            for (BlockPos p : network) {
                level.updateNeighborsAt(p, this);
            }
        }
    }

    /**
     * Finds all RedstoneChainBlocks connected to the starting position via adjacent blocks.
     * <p>
     * This method implements a breadth-first search (BFS) algorithm to discover all chain blocks
     * that are touching each other (sharing a face). This is used for traditional adjacent
     * connections, not wire connections.
     * <p>
     * How the BFS algorithm works:
     * 1. Create an empty Set to track found blocks (prevents duplicates)
     * 2. Create a Queue with the starting position
     * 3. While there are positions to check:
     *    a. Take a position from the queue
     *    b. Skip if we've already processed it
     *    c. Skip if it's not a RedstoneChainBlock
     *    d. Add it to our network set
     *    e. Check all 6 adjacent positions (up, down, north, south, east, west)
     *    f. Add those positions to the queue for checking
     * 4. Return the set of all connected chain blocks
     * <p>
     * This creates networks of touching chain blocks that will all share the same power level.
     *
     * @param level The world/level to search in
     * @param start The starting position to search from
     * @return A Set of all BlockPos that are part of this connected network
     */
    private Set<BlockPos> findNetwork(Level level, BlockPos start) {
        Set<BlockPos> network = new HashSet<>();  // Visited blocks
        Queue<BlockPos> queue = new LinkedList<>(); // Blocks to check
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // Skip if already visited
            if (network.contains(current)) continue;

            // Skip if not a chain block
            if (!(level.getBlockState(current).getBlock() instanceof RedstoneChainBlock)) continue;

            // Add to network
            network.add(current);

            // Add all neighbors to queue
            for (Direction dir : Direction.values()) {
                queue.add(current.relative(dir));
            }
        }
        return network;
    }

    /**
     * Finds the maximum redstone power from any block adjacent to this network.
     * <p>
     * This method checks all neighbors of every block in the network to find the strongest
     * redstone signal being input into the network. This is how the chain blocks "receive"
     * power from external sources like redstone torches, repeaters, or powered blocks.
     * <p>
     * How it works:
     * 1. Start with max power of 0
     * 2. For each block in our network:
     * a. Check all 6 directions (up, down, north, south, east, west)
     * b. Get the neighbor position in that direction
     * c. Skip if the neighbor is also part of our network (we only want external power)
     * d. Ask that neighbor block how much signal it's providing
     * - level.getSignal() returns the redstone power (0-15) from that block
     * - We pass d.getOpposite() because Minecraft's redstone API is backwards
     * e. Keep track of the maximum power found
     * f. If we find power of 15 (maximum), return early - no need to keep searching
     * 3. Return the maximum power level found
     * <p>
     * This ensures the network adopts the strongest signal from any connected power source.
     *
     * @param level   The world/level to check in
     * @param network The set of positions that make up this redstone network
     * @return The maximum power level (0-15) from any external neighbor
     */
    private int findPower(Level level, Set<BlockPos> network) {
        int max = 0;
        for (BlockPos p : network) {
            for (Direction d : Direction.values()) {
                BlockPos neighbor = p.relative(d);
                if (network.contains(neighbor)) continue;
                max = Math.max(max, level.getSignal(neighbor, d.getOpposite()));
                if (max >= 15) return 15;
            }
        }
        return max;
    }

    // ===== EntityBlock implementation =====

    /**
     * Creates a new BlockEntity for this block.
     * <p>
     * BlockEntities (also called Tile Entities) are special objects that can store data
     * and perform logic for a block. Not all blocks have them - only blocks that need to
     * store extra information or do complex things.
     * <p>
     * Our RedstoneChainBlock uses a BlockEntity (RedstoneChainEntity) to:
     * - Store connections to other chain blocks (for long-range wire connections)
     * - Track the current power level
     * - Handle network updates across connected wires
     * <p>
     * This method is called whenever a RedstoneChainBlock is placed in the world.
     *
     * @param pos   The position where the block entity should be created
     * @param state The state of the block
     * @return A new RedstoneChainEntity instance, or null if creation fails
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneChainEntity(pos, state);
    }

    /**
     * Provides a "ticker" that runs logic every game tick for the BlockEntity.
     * <p>
     * In Minecraft, a "tick" happens 20 times per second (every 0.05 seconds). Some BlockEntities
     * need to do something every tick - like furnaces checking if they should keep smelting,
     * or hoppers checking for items to transfer.
     * <p>
     * This method tells Minecraft what code to run each tick for our RedstoneChainEntity.
     * <p>
     * What happens inside:
     * 1. Check if the BlockEntityType matches our REDSTONE_CHAIN_ENTITY type
     * - This ensures we're only ticking the right kind of entity
     * 2. If it matches, return a reference to RedstoneChainEntity::tick
     * - This is a method reference that points to the tick() method in RedstoneChainEntity
     * - That method will be called every game tick (20 times per second)
     * 3. If it doesn't match, return null (no ticker needed)
     * <p>
     * For our redstone chain, the ticker is used to periodically update the network and
     * synchronize power levels across connected blocks.
     *
     * @param level The world/level the block is in
     * @param state The current state of the block
     * @param type  The type of block entity
     * @return A ticker function, or null if no ticking is needed
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == MinecraftPlayground.REDSTONE_CHAIN_ENTITY.get() ? RedstoneChainEntity::tick : null;
    }

    /**
     * Called when this block is removed or replaced in the world.
     * <p>
     * This is a cleanup method that runs when the block is broken by a player, destroyed by
     * an explosion, or replaced with a different block. It's important for cleaning up
     * resources and preventing memory leaks or orphaned connections.
     * <p>
     * What happens inside:
     * 1. First check if the block is actually being replaced with a different block type
     * (!state.is(newState.getBlock())) - we only clean up if it's truly being removed,
     * not just changing state (like power level)
     * 2. Get the BlockEntity at this position
     * 3. If it's a RedstoneChainEntity (has wire connections):
     * a. Loop through all blocks this one is connected to
     * b. For each connected block, get its BlockEntity
     * c. If that entity is also a RedstoneChainEntity, tell it to remove its connection
     * back to us - this prevents "dangling" connections pointing to a deleted block
     * d. Clear all connections from this entity
     * 4. Call the parent class's onRemove to handle standard cleanup
     * <p>
     * This bidirectional cleanup ensures that when a chain block is removed, all other
     * blocks that were connected to it are properly notified and updated.
     *
     * @param state         The state of this block before removal
     * @param level         The world/level the block is in
     * @param pos           The position of this block
     * @param newState      The new state that will replace this block
     * @param movedByPiston Whether this removal was caused by a piston
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneChainEntity chain) {
                // Remove connections from other chains that point to this one
                for (BlockPos otherPos : new ArrayList<>(chain.getConnections())) {
                    BlockEntity otherBe = level.getBlockEntity(otherPos);
                    if (otherBe instanceof RedstoneChainEntity otherChain) {
                        otherChain.removeConnection(pos);
                    }
                }
                chain.clearConnections();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Tells Minecraft whether this block can provide an analog signal to comparators.
     * <p>
     * Comparators in Minecraft can read analog signals (0-15) from certain blocks.
     * Common examples include:
     * - Chests (signal based on how full they are)
     * - Brewing stands (signal based on brewing progress)
     * - Redstone wire (signal based on power level)
     * <p>
     * By returning true, we tell Minecraft that comparators should be able to read
     * the power level from our RedstoneChainBlock. This allows players to use comparators
     * to detect different power levels in redstone circuits.
     *
     * @param state The current state of the block
     * @return true, indicating this block supports analog comparator output
     */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * Returns the analog signal strength that comparators should read from this block.
     * <p>
     * This method is called when a comparator is placed next to this block and wants to
     * read its analog output. The value returned (0-15) will be used by the comparator
     * in its calculations.
     * <p>
     * We simply return the POWER value from the block's state, which means:
     * - A powered chain block with power 15 will output signal strength 15 to comparators
     * - An unpowered chain block (power 0) will output signal strength 0
     * - Everything in between maintains its exact power level
     * <p>
     * This allows players to build circuits that detect specific power levels using comparators.
     *
     * @param state The current state of the block
     * @param level The world/level the block is in
     * @param pos   The position of the block
     * @return The analog signal strength (0-15) based on the block's POWER value
     */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(POWER);
    }
}
