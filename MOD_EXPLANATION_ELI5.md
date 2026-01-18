# 🔗 Hanging Redstone Wires Mod - Complete Guide
## Explained Like You're 5

---

## 🎯 What Does This Mod Do?

Imagine you have Minecraft chain blocks (those metal chain decorations). Your mod makes them **magical**! You can:

1. Place chain blocks around your world
2. Use a special connector tool to draw **red glowing cables** between them (up to 20 blocks apart!)
3. These cables carry **redstone power** - like invisible electricity wires in the sky!

It's like creating overhead power lines, but for redstone signals. Super cool for building advanced redstone contraptions without ugly redstone dust everywhere!

---

## 📚 The Files Explained

### 1. **Config.java** - The Settings File

**What it does:** This is just example code from the NeoForge template. It's like a settings menu, but your mod doesn't really use it yet.

**ELI5:** Think of this like a toy instruction manual that came in the box but you haven't opened yet. It's there if you need it later!

**Location:** `src/main/java/at/osa/minecraftplayground/Config.java`

---

### 2. **RedstoneChainBlockItem.java** - The Shiny Item

**What it does:** Makes the chain block item glow with an enchanted shimmer when it's in your inventory.

```java
public boolean isFoil(ItemStack stack) {
    return true;  // Always show the enchanted glint!
}
```

**ELI5:** Remember how enchanted diamond swords have that cool shimmering effect? This makes your chain blocks look magical too, so you know they're special!

**Location:** `src/main/java/at/osa/minecraftplayground/RedstoneChainBlockItem.java`

---

### 3. **MinecraftPlayground.java** - The Mod's Brain (Main File)

**What it does:** This is the "boss" file that tells Minecraft about everything in your mod.

**Location:** `src/main/java/at/osa/minecraftplayground/MinecraftPlayground.java`

#### Key Parts:

#### **Registering Stuff** (Lines 48-88)

```java
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
    DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
```

**ELI5:** Think of `DeferredRegister` like a shopping list you give to Minecraft. You write down "I want to add a chain block, a connector item, and a special block entity" and Minecraft says "okay, I'll add those to the game!"

**NeoForge Concept:** According to [NeoForge's Registry documentation](https://docs.neoforged.net/docs/concepts/registries/), DeferredRegister is the modern way to register content to the game. It defers (delays) the actual registration until the right time in the mod loading process.

---

#### **The Chain Block** (Lines 64-70)

```java
public static final DeferredBlock<RedstoneChainBlock> REDSTONE_CHAIN_BLOCK = BLOCKS.register(
    "redstone_chain",
    () -> new RedstoneChainBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
```

**ELI5:** This creates your special chain block! It's like telling Minecraft "Hey, I invented a new type of block called 'redstone_chain' and here's how to build one."

**Technical Details:**
- `"redstone_chain"` is the registry name (becomes `minecraftplayground:redstone_chain`)
- The lambda `() -> new RedstoneChainBlock(...)` is a supplier - it tells Minecraft HOW to create the block when needed
- `BlockBehaviour.Properties.of()` sets up basic block properties (hardness, sound, etc.)

---

#### **The Connector Tool** (Lines 73-75)

```java
public static final DeferredItem<RedstoneChainConnector> REDSTONE_CHAIN_CONNECTOR = ITEMS.register(
    "redstone_chain_connector",
    () -> new RedstoneChainConnector(new Item.Properties().stacksTo(64)));
```

**ELI5:** This is your magic wand for connecting chains! You can stack up to 64 of them.

**Technical Details:**
- `stacksTo(64)` means you can have up to 64 in one inventory slot
- If you wanted it to be like buckets (non-stackable), you'd use `stacksTo(1)`

---

#### **The Block Entity** (Lines 78-80)

```java
public static final Supplier<BlockEntityType<RedstoneChainEntity>> REDSTONE_CHAIN_ENTITY =
    BLOCK_ENTITY_TYPES.register(
        "redstone_chain_entity",
        () -> BlockEntityType.Builder.of(RedstoneChainEntity::new, REDSTONE_CHAIN_BLOCK.get()).build(null));
```

**ELI5:** Regular blocks are "dumb" - they just sit there. Block Entities are "smart" blocks that can remember stuff! Your chain block needs to remember which other chains it's connected to, so it needs a Block Entity brain.

**NeoForge Concept:** See [NeoForge's Block Entities documentation](https://docs.neoforged.net/docs/blockentities/) for more details.

**Technical Details:**
- `BlockEntityType.Builder.of(RedstoneChainEntity::new, REDSTONE_CHAIN_BLOCK.get())` creates a factory
- The first parameter is how to create the entity
- The second parameter specifies which blocks can have this entity
- `build(null)` finalizes the builder (null means default data fixer)

---

#### **Data Component** (Lines 83-88)

```java
public static final Supplier<DataComponentType<CompoundTag>> LINK_DATA = DATA_COMPONENT_TYPES.register(
    "link_data",
    () -> DataComponentType.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
            .build());
```

**ELI5:** Your connector tool needs to remember the first chain block you clicked. This is like giving the item a sticky note to write on!

**Technical Details:**
- `.persistent(CompoundTag.CODEC)` means it saves when you quit the game
- `.networkSynchronized(ByteBufCodecs.COMPOUND_TAG)` means it tells the server and client about changes
- `CompoundTag` is Minecraft's way of storing complex data (like JSON)
- This is the NEW Minecraft 1.21 way - it replaced the old NBT system!

---

### 4. **MinecraftPlaygroundClient.java** - The Graphics Department

**What it does:** This file ONLY runs on the player's computer (not on servers). It tells Minecraft how to draw the cables.

**Location:** `src/main/java/at/osa/minecraftplayground/MinecraftPlaygroundClient.java`

```java
@Mod(value = MinecraftPlayground.MODID, dist = Dist.CLIENT)
public class MinecraftPlaygroundClient {

    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            MinecraftPlayground.REDSTONE_CHAIN_ENTITY.get(),
            RedstoneChainRenderer::new
        );
    }
}
```

**ELI5:** Imagine Minecraft is a movie. The server handles the story (what happens), but the client handles the special effects (what you see). This file says "Hey client, when you see a chain block, use the RedstoneChainRenderer to draw cool cables between them!"

**NeoForge Concept:** According to [NeoForge's BlockEntityRenderer docs](https://docs.neoforged.net/docs/1.21.1/blockentities/ber/), you register renderers using the `EntityRenderersEvent.RegisterRenderers` event on the mod event bus, which is exactly what your code does!

**Technical Details:**
- `dist = Dist.CLIENT` means this class ONLY loads on clients, never on dedicated servers
- This prevents server crashes when it tries to access client-only rendering code
- `RedstoneChainRenderer::new` is a method reference - shorthand for `context -> new RedstoneChainRenderer(context)`

---

### 5. **RedstoneChainBlock.java** - The Smart Block

**What it does:** This is the actual chain block that can carry redstone power. It's like redstone wire, but vertical!

**Location:** `src/main/java/at/osa/minecraftplayground/RedstoneChainBlock.java`

#### Key Concepts:

#### **Block Shape** (Lines 84-87)

```java
private static final VoxelShape SHAPE = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);

@Override
public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
}
```

**ELI5:** This tells Minecraft how big the block is. Instead of a full 16x16x16 cube, it's a skinny pole (3 pixels wide).

**Technical Details:**
- `Block.box(x1, y1, z1, x2, y2, z2)` creates a rectangular box
- Coordinates are in pixels (0-16 scale)
- `6.5, 0, 6.5` to `9.5, 16, 9.5` = 3x16x3 pixel vertical pole
- This affects both collision (what you can walk through) and selection box (what you click)

---

#### **Redstone Power** (Lines 102-105)

```java
@Override
public boolean isSignalSource(BlockState state) {
    return state.getValue(POWER) > 0;
}
```

**ELI5:** Minecraft asks "Can this block power redstone stuff?" Your block says "Yes, if my POWER value is greater than 0!"

**Technical Details:**
- This is checked by Minecraft's redstone system to determine if the block can emit signals
- Without this returning true, the block would never power anything
- POWER is a BlockState property ranging from 0-15 (like vanilla redstone wire)

---

#### **The Backwards Direction Quirk** (Lines 107-136)

```java
@Override
public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
    // NOTE: direction parameter is BACKWARDS!
    BlockEntity be = level.getBlockEntity(pos);
    if (be instanceof RedstoneChainEntity chain) {
        return chain.getSignal();
    }
    return state.getValue(POWER);
}
```

**ELI5:** Here's a weird Minecraft thing - when a block to the NORTH asks for your signal, Minecraft actually passes Direction.SOUTH! It's backwards!

**Why is it backwards?**
It's like someone asking "What's behind me?" instead of "What's in front of you?" The answer is the same, but the question is phrased weirdly. Minecraft asks "what signal is coming FROM that direction" rather than "what signal are you sending TO that direction."

**Technical Details:**
- This is an indirect signal (also called "weak power")
- It can power things through solid blocks
- If a chain block has POWER=15, it will return 15 to anyone who queries it

---

#### **Direct Signal** (Lines 162-166)

```java
@Override
public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
    int indirectPower = 0;
    return indirectPower;
}
```

**ELI5:** This is different from indirect signals. Your block intentionally returns 0 here to mimic vanilla redstone wire behavior.

**Technical Details:**
- Direct signals (also called "strong power") can ONLY power things directly touching
- Indirect signals can power through solid blocks
- By returning 0 for direct but >0 for indirect, chain blocks act like redstone dust
- This means: ✅ Can power a lamp through a block, ❌ Cannot directly power comparators

---

#### **Neighbor Updates** (Lines 194-206)

```java
@Override
public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                           BlockPos neighborPos, boolean movedByPiston) {
    if (!level.isClientSide && !(neighborBlock instanceof RedstoneChainBlock)) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneChainEntity chain) {
            chain.updateSignalInNetwork();
        } else {
            level.scheduleTick(pos, this, 1);
        }
    }
}
```

**ELI5:** When a block next to this one changes, Minecraft calls this. It's like your neighbor knocking on your door to say "Hey, I changed! You might want to update too!"

**Technical Details:**
- `!level.isClientSide` ensures this only runs on the server (no client-side logic)
- `!(neighborBlock instanceof RedstoneChainBlock)` prevents feedback loops between chain blocks
- `updateSignalInNetwork()` immediately recalculates for wire-connected networks
- `scheduleTick(pos, this, 1)` delays the update by 1 tick for adjacent-only networks
- The 1-tick delay prevents cascading updates from causing lag

---

#### **Scheduled Tick** (Lines 267-293)

```java
@Override
public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
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
```

**ELI5:** This is called 1 tick after `scheduleTick()` was called. It handles updating adjacent-only chain networks (blocks touching each other without cables).

**Technical Details:**
- If the block has a RedstoneChainEntity, it handles its own updates (early return)
- Otherwise, use traditional network logic:
  1. Find all touching chain blocks (`findNetwork`)
  2. Find the max power from any neighbor (`findPower`)
  3. Update all blocks in the network to that power
  4. Notify neighbors if anything changed
- Flag `2` in `setBlock` means "update neighbors but don't send to client yet"

---

#### **Network Finding** (Lines 320-334)

```java
private Set<BlockPos> findNetwork(Level level, BlockPos start) {
    Set<BlockPos> network = new HashSet<>();
    Queue<BlockPos> queue = new LinkedList<>();
    queue.add(start);

    while (!queue.isEmpty()) {
        BlockPos p = queue.poll();
        if (network.contains(p)) continue;
        if (!(level.getBlockState(p).getBlock() instanceof RedstoneChainBlock)) continue;
        network.add(p);
        for (Direction d : Direction.values()) {
            queue.add(p.relative(d));
        }
    }
    return network;
}
```

**ELI5:** This uses a "breadth-first search" algorithm. Imagine you're standing in a maze and you want to find all connected rooms.

**The Algorithm:**
1. Start in your room
2. Check all doors (all 6 directions: up, down, north, south, east, west)
3. Mark those rooms as "visited"
4. From each new room, check THEIR doors
5. Keep going until you've found everything!

**Technical Details:**
- **Time Complexity:** O(n) where n is the number of connected blocks
- **Space Complexity:** O(n) for the queue and set
- Uses a `HashSet` for O(1) lookup to check if already visited
- Uses a `LinkedList` as a FIFO queue (First In, First Out)
- This is a classic BFS (Breadth-First Search) implementation

---

#### **Power Finding** (Lines 363-373)

```java
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
```

**ELI5:** This checks every neighbor of every block in the network to find the strongest redstone signal being input.

**Technical Details:**
- Only checks external neighbors (skips blocks within the network)
- Uses `d.getOpposite()` because of Minecraft's backwards direction API
- Early returns if power 15 is found (optimization - no need to keep searching)
- **Time Complexity:** O(n × 6) = O(n) where n is network size

---

#### **Block Entity Methods** (Lines 396-431)

```java
@Override
public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new RedstoneChainEntity(pos, state);
}

@Override
public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                BlockEntityType<T> type) {
    return type == MinecraftPlayground.REDSTONE_CHAIN_ENTITY.get() ? RedstoneChainEntity::tick : null;
}
```

**ELI5:**
- `newBlockEntity`: Creates the "brain" for the block
- `getTicker`: Tells Minecraft to call the entity's `tick()` method every game tick (20x per second)

**Technical Details:**
- The ticker only runs if the BlockEntityType matches (safety check)
- `RedstoneChainEntity::tick` is a method reference to the static tick method
- Tickers run every tick, so expensive operations should be throttled

---

#### **Cleanup on Removal** (Lines 463-478)

```java
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
```

**ELI5:** When you break a chain block, this tells all the blocks it was connected to "hey, I'm gone, remove your cable to me!"

**Technical Details:**
- `!state.is(newState.getBlock())` checks if the block is actually being removed (vs just changing state)
- `new ArrayList<>(chain.getConnections())` creates a copy to avoid ConcurrentModificationException
- This ensures bidirectional cleanup - both ends of each cable are notified
- Prevents "dangling" connections to non-existent blocks

---

#### **Comparator Support** (Lines 497-523)

```java
@Override
public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
}

@Override
public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    return state.getValue(POWER);
}
```

**ELI5:** Comparators can read the exact power level from your chain blocks, just like they can from redstone wire!

**Technical Details:**
- Comparators use analog signals (0-15) instead of just on/off
- This allows for complex circuits that detect specific power levels
- Examples of other analog blocks: chests (fullness), brewing stands (progress), cake (bites eaten)

---

### 6. **RedstoneChainRenderer.java** - The Cable Drawer

**What it does:** This draws the beautiful red cables between chain blocks.

**Location:** `src/main/java/at/osa/minecraftplayground/RedstoneChainRenderer.java`

#### **The Render Type** (Lines 25-38)

```java
public static final RenderType CABLE_RENDER_TYPE = RenderType.create(
    "redstone_cable_render",
    DefaultVertexFormat.NEW_ENTITY,
    VertexFormat.Mode.TRIANGLES,
    256,
    false,
    true,
    RenderType.CompositeState.builder()
        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader))
        .setCullState(new RenderStateShard.CullStateShard(false))
        .setLightmapState(new RenderStateShard.LightmapStateShard(true))
        .setOverlayState(new RenderStateShard.OverlayStateShard(true))
        .createCompositeState(false)
);
```

**ELI5:** This sets up the "paint brush" for drawing cables.

**Technical Details:**
- `DefaultVertexFormat.NEW_ENTITY` - Each vertex has position, color, UV, lightmap, normal, overlay
- `VertexFormat.Mode.TRIANGLES` - We're drawing triangles (standard 3D primitive)
- `256` - Buffer size (how many vertices can be batched)
- `getPositionColorLightmapShader` - Which shader program to use
- `setCullState(false)` - **IMPORTANT!** Draw both sides of faces (no backface culling)
- `setLightmapState(true)` - Respect Minecraft's lighting (darker in caves, brighter in daylight)
- `setOverlayState(true)` - Support the red damage overlay when entities are hurt

**Why disable culling?**
Normally, only the "outside" of faces are drawn (culling). But cables are thin and can be viewed from any angle, so we need both sides visible!

---

#### **The Main Render Loop** (Lines 50-64)

```java
@Override
public void render(RedstoneChainEntity entity, float partialTicks, PoseStack stack,
                   MultiBufferSource buffer, int packedLight, int packedOverlay) {
    BlockPos blockPos = entity.getBlockPos();
    int power = entity.getSignal();

    for (BlockPos connection : entity.getConnections()) {
        // Only render if this block's position is "less than" the connection
        if (blockPos.compareTo(connection) < 0) {
            Vec3 start = new Vec3(0.5, 0.5, 0.5);
            Vec3 end = Vec3.atCenterOf(connection).subtract(Vec3.atCenterOf(blockPos)).add(0.5, 0.5, 0.5);
            renderCurvedCuboid(stack, buffer, start, end, packedLight, packedOverlay, power);
        }
    }
}
```

**ELI5:** For each cable connection, draw it - but only from one end to avoid drawing the same cable twice!

**Technical Details:**
- `blockPos.compareTo(connection) < 0` ensures each cable is rendered exactly once
- If block A is at (100, 64, 100) and block B is at (110, 64, 100):
  - Block A's renderer draws the cable (100 < 110)
  - Block B's renderer skips it (110 > 100)
- `Vec3.atCenterOf(...)` gets the center point of a block (adds 0.5 to each coordinate)
- The `end` calculation converts from world coordinates to relative coordinates

---

#### **Power-Based Coloring** (Lines 70-94)

```java
public static void renderCurvedCuboid(PoseStack poseStack, MultiBufferSource buffer,
                                      Vec3 from, Vec3 to, int light, int overlay, int power) {
    int segments = 10;
    float thickness = 0.03F;

    // Power-based coloring
    float red = power > 0 ? 0.9f + (power / 15.0f) * 0.1f : 0.3f;
    float green = 0.0f;
    float blue = 0.0f;

    for (int i = 0; i < segments; i++) {
        float t1 = i / (float) segments;
        float t2 = (i + 1) / (float) segments;

        Vec3 p1 = interpolateCurved(from, to, t1);
        Vec3 p2 = interpolateCurved(from, to, t2);

        drawThickSegment(builder, matrix, normal, p1, p2, thickness, light, overlay, red, green, blue);
    }
}
```

**ELI5:** The cable is split into 10 segments, and each segment is drawn as a small tube. The color changes based on power level!

**Technical Details:**
- **Color Calculation:**
  - Unpowered (power = 0): RGB(0.3, 0, 0) = dark red
  - Powered (power = 15): RGB(1.0, 0, 0) = bright red
  - Formula: `0.9 + (15/15) × 0.1 = 0.9 + 0.1 = 1.0`
- **Segments:** 10 segments is enough for smooth curves without too many vertices
- **Thickness:** 0.03 blocks = about 2 pixels (subtle but visible)
- Each segment connects `t1` to `t2` where `t` ranges from 0.0 to 1.0

---

#### **The Curve Effect** (Lines 97-104)

```java
private static Vec3 interpolateCurved(Vec3 from, Vec3 to, float t) {
    Vec3 linear = from.lerp(to, t);
    if (Math.abs(from.x - to.x) < 0.001 && Math.abs(from.z - to.z) < 0.001) {
        return linear;
    }
    double curve = Math.sin(t * Math.PI) * -0.4;
    return new Vec3(linear.x, linear.y + curve, linear.z);
}
```

**ELI5:** Real power lines sag in the middle because of gravity! This code makes your cables look realistic.

**The Math:**
```
curve = sin(t × π) × -0.4
```

As `t` goes from 0 to 1:
- **t = 0:** sin(0) = 0 → no sag at start
- **t = 0.25:** sin(π/4) ≈ 0.707 → -0.28 blocks down
- **t = 0.5:** sin(π/2) = 1 → -0.4 blocks down (maximum sag!)
- **t = 0.75:** sin(3π/4) ≈ 0.707 → -0.28 blocks down
- **t = 1:** sin(π) = 0 → no sag at end

**Special Case:** If the cable is perfectly vertical (same X and Z), skip the curve (no gravity effect needed).

**Real Physics:** This approximates a catenary curve (the natural shape of a hanging chain)! A true catenary uses hyperbolic cosine, but sine is close enough and way faster to compute.

---

#### **Drawing the Cable Tube** (Lines 106-236)

```java
private static void drawThickSegment(VertexConsumer builder, Matrix4f matrix, Matrix3f normal,
                                     Vec3 p1, Vec3 p2, float thickness, int light, int overlay,
                                     float r, float g, float b) {
    Vec3 dir = p2.subtract(p1).normalize();
    Vec3 up = Math.abs(dir.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
    Vec3 right = dir.cross(up).normalize().scale(thickness);
    Vec3 forward = dir.cross(right).normalize().scale(thickness);

    // Create 8 corners of a rectangular tube
    Vec3[] corners = new Vec3[]{
        p1.add(right).add(forward),           // 0
        p1.add(right).subtract(forward),      // 1
        p1.subtract(right).subtract(forward), // 2
        p1.subtract(right).add(forward),      // 3
        p2.add(right).add(forward),           // 4
        p2.add(right).subtract(forward),      // 5
        p2.subtract(right).subtract(forward), // 6
        p2.subtract(right).add(forward),      // 7
    };

    // Define 6 faces (each face = 4 corners)
    int[][] faces = {
        {0, 1, 2, 3}, // bottom (p1 end)
        {7, 6, 5, 4}, // top (p2 end)
        {0, 4, 5, 1}, // right side
        {1, 5, 6, 2}, // front side
        {2, 6, 7, 3}, // left side
        {3, 7, 4, 0}, // back side
    };

    // Render each face with double-sided rendering...
}
```

**ELI5:** Drawing 3D objects is like building with LEGO bricks!

**The Algorithm:**

1. **Calculate orientation vectors:**
   - `dir` = direction from p1 to p2
   - `up` = a vector pointing "up" (or sideways if dir is vertical)
   - `right` = perpendicular to dir and up (cross product!)
   - `forward` = perpendicular to dir and right

2. **Create 8 corners:**
   - 4 corners at p1 (start of segment)
   - 4 corners at p2 (end of segment)
   - Each corner is offset by ±right and ±forward

3. **Define 6 faces:**
   - Each face connects 4 corners
   - Winding order is counter-clockwise when viewed from outside

4. **Draw each face:**
   - Split each 4-corner face into 2 triangles
   - Triangle 1: corners [0, 1, 2]
   - Triangle 2: corners [0, 2, 3]
   - Draw each triangle TWICE (once for front, once for back)

**Why draw twice?**
Without backface culling, we need to render both sides of each triangle so it's visible from any angle!

---

#### **Cross Product Magic**

```java
Vec3 right = dir.cross(up).normalize().scale(thickness);
```

**What's a cross product?**
Given two vectors, the cross product creates a third vector that's perpendicular to both!

**Visual:**
```
      up
       ↑
       |
       |
dir ———→

right = dir × up points OUT OF THE PAGE! (⊙)
```

This is how we create a coordinate system oriented along the cable direction!

---

### 7. **RedstoneChainEntity.java** - The Connection Manager

**What it does:** This is the "brain" that remembers connections, builds networks, and distributes power.

**Location:** `src/main/java/at/osa/minecraftplayground/RedstoneChainEntity.java`

This is the most complex file, so let's break it down thoroughly!

---

#### **Data Storage** (Lines 33-63)

```java
private final List<BlockPos> connections = new ArrayList<>();
private final Set<BlockPos> network = new HashSet<>();
private boolean networkDirty = true;
private static final int MAX_CONNECTION_DISTANCE = 24;
private static final int UPDATE_INTERVAL = 20;
private int ticksSinceLastUpdate = 0;
private boolean isUpdating = false;
private int ticksWithoutInput = 0;
private static final int SIGNAL_LOSS_DELAY = 1;
private int cachedInputSignal = 0;
private static final int MAX_CONNECTIONS = 3;
```

**ELI5 Breakdown:**

| Variable | What It Is | Analogy |
|----------|------------|---------|
| `connections` | Your direct friends (who you're connected to with cables) | Your phone contacts |
| `network` | Your entire friend group (includes friends-of-friends) | Your extended social network |
| `networkDirty` | "My friend group might have changed, I should recount" | Needing to update your Facebook friends list |
| `MAX_CONNECTION_DISTANCE` | Max 24 blocks apart | Can only call people within 24 miles |
| `UPDATE_INTERVAL` | Update every 20 ticks (1 second) | Check your messages once per second |
| `isUpdating` | Am I currently updating? | "Do not disturb" mode |
| `ticksWithoutInput` | How long since I received power? | Days since you last ate |
| `SIGNAL_LOSS_DELAY` | Wait 1 tick before clearing signal | Grace period before canceling your meal |
| `cachedInputSignal` | Last known power level | What you last remember eating |
| `MAX_CONNECTIONS` | Max 3 cables per block | You can only have 3 phone lines |

---

#### **Adding Connections** (Lines 116-139)

```java
public void addConnection(BlockPos target) {
    if (connections.contains(target)) {
        return;  // Already connected
    }
    if (connections.size() >= MAX_CONNECTIONS) {
        return;  // Too many cables
    }

    if (worldPosition.distSqr(target) > MAX_CONNECTION_DISTANCE * MAX_CONNECTION_DISTANCE) {
        return;  // Too far away
    }

    connections.add(target);
    networkDirty = true;
    setChanged();
    syncToClient();

    if (level != null) {
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof RedstoneChainEntity other) {
            mergeWithOtherNetwork(other);
        }
    }
}
```

**ELI5:** When you connect two chains with the connector tool, this runs!

**Validation Checks:**
1. ✅ Not already connected
2. ✅ Less than 3 connections already
3. ✅ Within 24 blocks

**After Adding:**
1. Mark network as dirty (needs rebuild)
2. `setChanged()` - Tell Minecraft to save this data to disk
3. `syncToClient()` - Tell the player's game client to render the cable
4. `mergeWithOtherNetwork()` - Combine networks efficiently

**Technical Detail:** Uses `distSqr()` (squared distance) instead of `dist()` to avoid expensive square root calculation!

---

#### **Removing Connections** (Lines 161-174)

```java
public void removeConnection(BlockPos target) {
    if (connections.remove(target)) {
        networkDirty = true;
        setChanged();
        syncToClient();
        invalidateNetwork();
        if (level != null) {
            BlockEntity be = level.getBlockEntity(target);
            if (be instanceof RedstoneChainEntity cable) {
                cable.invalidateNetwork();
            }
        }
    }
}
```

**ELI5:** When a cable is broken (block destroyed, or connection removed), this cleans up!

**Cleanup Steps:**
1. Remove from connections list
2. Mark network dirty
3. Save to disk
4. Sync to client (cable disappears)
5. Invalidate THIS network
6. Invalidate TARGET's network too

**Why invalidate both?**
When you break a connection, you might split one network into TWO separate networks! Both need to rebuild.

---

#### **Network Invalidation** (Lines 204-227)

```java
public void invalidateNetwork() {
    if (level == null) return;

    Set<BlockPos> visited = new HashSet<>();
    Queue<BlockPos> queue = new LinkedList<>();
    queue.add(this.worldPosition);

    while (!queue.isEmpty()) {
        BlockPos current = queue.poll();
        if (!visited.add(current)) continue;

        BlockEntity be = level.getBlockEntity(current);
        if (be instanceof RedstoneChainEntity chain) {
            chain.networkDirty = true;
            chain.setChanged();

            for (BlockPos neighbor : chain.getConnectedChains()) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
    }
}
```

**ELI5:** This finds ALL chain blocks connected to this one and tells them "your network is outdated, rebuild it!"

**Algorithm:** Breadth-First Search (BFS) again!

**Why do this?**
Imagine this network:
```
A — B — C — D
```

If you break the B—C connection:
```
A — B     C — D
```

Now there are TWO networks: {A, B} and {C, D}. But they all still think they're in the same network! `invalidateNetwork()` tells all 4 blocks to rebuild, and they'll discover the split.

---

#### **Clearing All Connections** (Lines 249-265)

```java
public void clearConnections() {
    List<BlockPos> oldConnections = new ArrayList<>(connections);
    connections.clear();
    networkDirty = true;
    setChanged();
    syncToClient();
    invalidateNetwork();

    if (level != null) {
        for (BlockPos target : oldConnections) {
            BlockEntity be = level.getBlockEntity(target);
            if (be instanceof RedstoneChainEntity chain) {
                chain.invalidateNetwork();
            }
        }
    }
}
```

**ELI5:** Called when the block is destroyed. It's like deleting your account - everyone who was friends with you needs to update their friend list!

**Technical Detail:** Makes a copy of `connections` before clearing to avoid `ConcurrentModificationException` while iterating.

---

#### **Getting Connected Chains** (Lines 286-297)

```java
public List<BlockPos> getConnectedChains() {
    List<BlockPos> result = new ArrayList<>();
    if (level == null) return result;

    for (BlockPos pos : connections) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneChainEntity && !result.contains(pos)) {
            result.add(pos);
        }
    }
    return result;
}
```

**ELI5:** Returns only the valid connections (blocks that still exist and are still chain blocks).

**Why validate?**
The `connections` list might contain stale data (blocks that were removed). This filters to only working connections.

---

#### **Network Rebuilding** (Lines 324-347)

```java
public void rebuildNetwork() {
    if (level == null) return;

    Set<BlockPos> visited = new HashSet<>();
    Queue<BlockPos> queue = new LinkedList<>();
    queue.add(worldPosition);

    while (!queue.isEmpty()) {
        BlockPos current = queue.poll();
        if (!visited.add(current)) continue;

        BlockEntity be = level.getBlockEntity(current);
        if (be instanceof RedstoneChainEntity chain) {
            for (BlockPos neighbor : chain.getConnectedChains()) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
    }

    network.clear();
    network.addAll(visited);
}
```

**ELI5:** This is like mapping out your entire friend network on social media!

**The Algorithm (BFS):**

```
Step 1: Start with yourself
Queue: [A]
Visited: {}

Step 2: Process A, add its connections
Queue: [B, C]
Visited: {A}

Step 3: Process B, add its connections
Queue: [C, D]
Visited: {A, B}

Step 4: Process C (already visited from A)
Queue: [D]
Visited: {A, B, C}

Step 5: Process D
Queue: []
Visited: {A, B, C, D}

Done! Network = {A, B, C, D}
```

**Complexity:**
- **Time:** O(n + e) where n = blocks, e = connections
- **Space:** O(n) for the visited set

---

#### **Computing Network Input Power** (Lines 378-398)

```java
private int computeNetworkInputPower() {
    if (level == null) return 0;

    int maxInput = 0;
    for (BlockPos pos : network) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            Block block = neighborState.getBlock();

            if (block instanceof RedstoneChainBlock) continue;
            if (block instanceof RedStoneWireBlock) continue;

            int signal = level.getSignal(neighborPos, dir.getOpposite());
            if (signal > 0) {
                maxInput = Math.max(maxInput, signal);
            }
        }
    }
    return maxInput;
}
```

**ELI5:** Imagine every block in your network is a house with 6 doors. This opens every door and asks "Are you giving me power?"

**Visual Example:**
```
Network blocks: {A, B, C}

A's neighbors:
  ↑ Air (signal = 0)
  ↓ Stone (signal = 0)
  → Lever (signal = 15) ← FOUND POWER!
  ← B (skip, part of network)
  etc.

B's neighbors:
  ↑ Redstone Torch (signal = 15) ← FOUND POWER!
  ...

C's neighbors:
  (all signal = 0)

Maximum = 15
```

**Special Skips:**
- `RedstoneChainBlock` - Don't check other network members
- `RedStoneWireBlock` - Don't connect to vanilla redstone (prevents loops)

---

#### **Applying Signal to Network** (Lines 428-440)

```java
private void applySignalToNetwork(int signal) {
    if (level == null) return;

    for (BlockPos pos : network) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof RedstoneChainBlock) {
            int old = state.getValue(RedstoneChainBlock.POWER);
            if (old != signal) {
                level.setBlock(pos, state.setValue(RedstoneChainBlock.POWER, signal), 3);
            }
        }
    }
}
```

**ELI5:** Sets every block in the network to the same power level.

**The Flag `3`:**
- Bit 0 (1): Send to clients (visual update)
- Bit 1 (2): Update neighbors (redstone components react)
- 1 + 2 = 3

**Optimization:** Only updates blocks that actually changed (`old != signal`).

---

#### **The Main Update Loop** (Lines 477-505)

```java
public void updateSignalInNetwork() {
    if (isUpdating) {
        return;  // Prevent recursive calls
    }
    isUpdating = true;
    try {
        if (networkDirty) {
            rebuildNetwork();
            networkDirty = false;
        }

        int input = computeNetworkInputPower();

        if (input > 0) {
            cachedInputSignal = input;
            ticksWithoutInput = 0;
        } else {
            ticksWithoutInput++;
            if (ticksWithoutInput >= SIGNAL_LOSS_DELAY) {
                cachedInputSignal = 0;
            }
        }

        applySignalToNetwork(cachedInputSignal);

    } finally {
        isUpdating = false;
    }
}
```

**ELI5:** This is the "heartbeat" that keeps the network alive and synchronized!

**Flow Chart:**
```
┌─────────────────────┐
│ Already updating?   │ Yes → Exit (prevent loops)
└──────┬──────────────┘
       │ No
       ▼
┌─────────────────────┐
│ Network dirty?      │ Yes → Rebuild network
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Check all neighbors │
│ for power input     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Input > 0?          │ Yes → Update cache, reset timer
└──────┬──────────────┘
       │ No
       ▼
┌─────────────────────┐
│ Increment timer     │
│ Timer >= delay?     │ Yes → Clear cached signal
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Apply cached signal │
│ to all blocks       │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Clear updating flag │
└─────────────────────┘
```

**Critical Features:**

1. **Feedback Loop Protection:**
   ```
   Block A updates → tells Block B to update
   Block B updates → tells Block A to update
   Block A: "I'm already updating!" → STOP
   ```
   Without `isUpdating`, this would loop forever!

2. **Signal Delay:**
   ```
   Tick 1: Power input = 15 → cache = 15
   Tick 2: Power input = 0 → cache = 15 (grace period)
   Tick 3: Power input = 0 → cache = 0 (now clear it)
   ```
   This prevents flickering when signals briefly turn off.

3. **Finally Block:**
   Ensures `isUpdating = false` even if an exception occurs (prevents permanent deadlock).

---

#### **Network Merging** (Lines 535-555)

```java
private void mergeWithOtherNetwork(RedstoneChainEntity other) {
    if (level == null) return;

    Set<BlockPos> network1 = new HashSet<>(this.network);
    Set<BlockPos> network2 = new HashSet<>(other.network);

    Set<BlockPos> larger = network1.size() >= network2.size() ? network1 : network2;
    Set<BlockPos> smaller = network1.size() < network2.size() ? network1 : network2;

    Set<BlockPos> merged = new HashSet<>(larger);
    merged.addAll(smaller);

    for (BlockPos pos : merged) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneChainEntity chain) {
            chain.network.clear();
            chain.network.addAll(merged);
            chain.networkDirty = false;
        }
    }
}
```

**ELI5:** When you connect two chains that are already in separate networks, this combines them efficiently!

**Example:**
```
Network A: {block1, block2, block3}
Network B: {block4, block5}

You connect block3 to block4.

Merged: {block1, block2, block3, block4, block5}
```

**Optimization:** Start with the larger network (less copying).

**Why not just rebuild?**
- Merging: O(n + m) - just copy both sets
- Rebuilding: O((n+m)²) in worst case - traverse entire graph again

---

#### **The Ticker** (Lines 585-594)

```java
public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
    if (!(be instanceof RedstoneChainEntity chain)) return;
    if (level.isClientSide) return;

    chain.ticksSinceLastUpdate++;
    if (chain.ticksSinceLastUpdate >= UPDATE_INTERVAL) {
        chain.ticksSinceLastUpdate = 0;
        chain.updateSignalInNetwork();
    }
}
```

**ELI5:** Called 20 times per second. Every 20 ticks (1 second), it updates the network.

**Why periodic updates?**
- **Primary:** Event-driven (neighborChanged)
- **Backup:** Periodic (every 1 second)

The periodic update catches edge cases where neighbor updates might be missed.

---

#### **Serialization (Saving/Loading)** (Lines 677-788)

```java
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    ListTag list = new ListTag();
    for (BlockPos pos : connections) {
        CompoundTag posTag = new CompoundTag();
        posTag.putInt("x", pos.getX());
        posTag.putInt("y", pos.getY());
        posTag.putInt("z", pos.getZ());
        list.add(posTag);
    }
    tag.put("Connections", list);
}

@Override
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    connections.clear();
    ListTag list = tag.getList("Connections", Tag.TAG_COMPOUND);
    for (Tag t : list) {
        CompoundTag posTag = (CompoundTag) t;
        connections.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
    }
    networkDirty = true;
}
```

**ELI5:** This saves your cable connections to the hard drive so they persist when you reload the world!

**NBT Structure:**
```json
{
  "Connections": [
    {"x": 100, "y": 64, "z": 200},
    {"x": 110, "y": 65, "z": 205},
    {"x": 95, "y": 60, "z": 195}
  ]
}
```

**Why not save the network?**
- Connections are fundamental (what you created)
- Network is derived (calculated from connections)
- Saves disk space
- Prevents stale data

---

#### **Client Synchronization** (Lines 640-788)

```java
private void syncToClient() {
    if (level != null && !level.isClientSide) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }
}

@Override
public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
}

@Override
public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    CompoundTag tag = new CompoundTag();
    saveAdditional(tag, registries);
    return tag;
}
```

**ELI5:** In multiplayer, the server has the "real" data. Clients need to be told about changes so they can render cables correctly.

**The Flow:**
```
Server: Cable added → syncToClient()
Server: Prepare packet → getUpdatePacket()
Server: Fill packet data → getUpdateTag()
   ↓
Network: Send packet
   ↓
Client: Receive packet → Update local copy
Client: Renderer sees updated connections
Client: Draw new cable!
```

---

### 8. **RedstoneChainConnector.java** - The Connection Tool

**What it does:** This is the item you use to connect chains together.

**Location:** `src/main/java/at/osa/minecraftplayground/RedstoneChainConnector.java`

---

#### **Constants** (Lines 27-28)

```java
private static final int MAX_CONNECTION_DISTANCE = 20;
private static final int MAX_CONNECTIONS_PER_CHAIN = 5;
```

**Note:** These differ from the entity's constants! The entity uses 24 blocks and 3 connections, but the connector uses 20 blocks and 5 connections. This creates a slight inconsistency in your mod.

---

#### **Clearing Saved Position** (Lines 35-58)

```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (player.isShiftKeyDown()) {
        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrDefault(MinecraftPlayground.LINK_DATA, new CompoundTag());
            if (tag.contains("LinkX")) {
                CompoundTag newTag = tag.copy();
                newTag.remove("LinkX");
                newTag.remove("LinkY");
                newTag.remove("LinkZ");
                stack.set(MinecraftPlayground.LINK_DATA, newTag.isEmpty() ? null : newTag);

                player.displayClientMessage(
                    Component.translatable("item.minecraftplayground.chain_connector.cleared")
                        .withStyle(ChatFormatting.YELLOW),
                    true
                );
            }
        }
        return InteractionResultHolder.success(stack);
    }

    return InteractionResultHolder.pass(stack);
}
```

**ELI5:** Shift-right-click in the air to clear the saved position (like canceling a connection).

**Technical Details:**
- `use()` is called when you right-click with the item (not on a block)
- `!level.isClientSide` ensures it only runs on the server
- `displayClientMessage(..., true)` shows the message above the hotbar (action bar)
- Returns `SUCCESS` to prevent other actions, `PASS` to allow them

---

#### **Connecting Blocks** (Lines 81-174)

```java
private InteractionResult handleShiftClick(Level level, Player player, BlockPos clickedPos,
                                           RedstoneChainEntity chain, ItemStack stack) {
    CompoundTag tag = stack.getOrDefault(MinecraftPlayground.LINK_DATA, new CompoundTag());

    // First check: Does this chain already have max connections?
    if (!level.isClientSide && chain.getConnections().size() >= MAX_CONNECTIONS_PER_CHAIN) {
        player.displayClientMessage(
            Component.translatable("item.minecraftplayground.chain_connector.max_connections")
                .withStyle(ChatFormatting.RED),
            true
        );
        return InteractionResult.FAIL;
    }

    if (tag.contains("LinkX")) {
        // SECOND CLICK - Complete the connection
        BlockPos startPos = new BlockPos(tag.getInt("LinkX"), tag.getInt("LinkY"), tag.getInt("LinkZ"));

        // Clear saved position
        stack.set(MinecraftPlayground.LINK_DATA, null);

        if (!startPos.equals(clickedPos)) {
            double distanceSq = startPos.distSqr(clickedPos);
            if (distanceSq > MAX_CONNECTION_DISTANCE * MAX_CONNECTION_DISTANCE) {
                // Too far!
                player.displayClientMessage(...);
                return InteractionResult.FAIL;
            }

            BlockEntity startBe = level.getBlockEntity(startPos);
            if (startBe instanceof RedstoneChainEntity startChain) {
                if (!level.isClientSide) {
                    // Create bidirectional connection
                    startChain.addConnection(clickedPos);
                    startChain.setChanged();
                    level.sendBlockUpdated(startPos, ...);

                    chain.addConnection(startPos);
                    chain.setChanged();
                    level.sendBlockUpdated(clickedPos, ...);

                    player.displayClientMessage(..., true);

                    // Consume one connector item
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
    } else {
        // FIRST CLICK - Save this position
        if (!level.isClientSide) {
            CompoundTag newTag = new CompoundTag();
            newTag.putInt("LinkX", clickedPos.getX());
            newTag.putInt("LinkY", clickedPos.getY());
            newTag.putInt("LinkZ", clickedPos.getZ());
            stack.set(MinecraftPlayground.LINK_DATA, newTag);

            player.displayClientMessage(...);
        }
        return InteractionResult.SUCCESS;
    }

    return InteractionResult.FAIL;
}
```

**ELI5:** This is like connecting two phones with a cable!

**First Click:**
1. Save the block's position to the item's NBT data
2. Show message: "Saved point: (100, 64, 200)"

**Second Click:**
1. Read the saved position from NBT
2. Check if blocks are too far apart
3. Create bidirectional connection (A→B and B→A)
4. Sync both blocks to clients
5. Show success message
6. Consume one connector item (unless creative mode)

**Validation:**
- ✅ Both blocks are chain blocks
- ✅ Not connecting to self
- ✅ Within 20 blocks
- ✅ Neither block has max connections

---

#### **Tooltip** (Lines 209-222)

```java
@Override
public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    CompoundTag tag = stack.getOrDefault(MinecraftPlayground.LINK_DATA, new CompoundTag());
    if (tag.contains("LinkX")) {
        BlockPos pos = new BlockPos(tag.getInt("LinkX"), tag.getInt("LinkY"), tag.getInt("LinkZ"));
        tooltip.add(Component.translatable("item.minecraftplayground.chain_connector.saved_point",
                pos.toShortString()).withStyle(ChatFormatting.GRAY));
    } else {
        tooltip.add(Component.translatable("item.minecraftplayground.chain_connector.no_saved_point")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
    tooltip.add(Component.translatable("item.minecraftplayground.chain_connector.usage")
            .withStyle(ChatFormatting.DARK_GRAY));
}
```

**ELI5:** When you hover over the connector in your inventory, this shows helpful text!

**Display:**
```
Redstone Chain Connector
  Saved point: [100, 64, 200]  ← Gray text
  <Usage instructions>         ← Dark gray text
```

---

## 🔄 How It All Works Together

### Scenario 1: Placing Your First Chain Block

**What You Do:** Place a `Redstone Chain Block` in the world.

**What Happens:**

1. **MinecraftPlayground.java (Line 64-66):** The DeferredRegister provides the block instance
2. **RedstoneChainBlock.java (Line 232-235):** `onPlace()` is called, schedules a tick in 1 game tick
3. **RedstoneChainBlock.java (Line 396-399):** `newBlockEntity()` creates a `RedstoneChainEntity`
4. **RedstoneChainEntity.java (Line 75-77):** Constructor initializes the entity
5. **One tick later (Line 267-293):** `tick()` is called, network is empty, block stays at POWER = 0

---

### Scenario 2: Connecting Two Chain Blocks

**Setup:**
- Chain Block A at (100, 64, 100)
- Chain Block B at (110, 64, 100)

**What You Do:**
1. Hold Redstone Chain Connector
2. Shift-click Block A
3. Shift-click Block B

**First Click (Block A):**
1. **RedstoneChainConnector.java (Line 62):** `useOn()` is called
2. **Line 74-75:** Detects shift-click, calls `handleShiftClick()`
3. **Line 142-170:** Creates CompoundTag, saves X=100, Y=64, Z=100, shows message

**Second Click (Block B):**
1. **RedstoneChainConnector.java (Line 93-140):** Reads saved position (100, 64, 100), checks distance: 10 blocks ✅
2. **Line 116-124:** Calls `startChain.addConnection(B's position)` and `chain.addConnection(A's position)`
3. **RedstoneChainEntity.java (Line 116-139):** Both blocks add connections, mark dirty, sync to client, merge networks
4. **Line 535-555:** `mergeWithOtherNetwork()` combines both networks
5. **RedstoneChainRenderer.java (Line 50-64):** Next frame, renderer draws cable from A to B

---

### Scenario 3: Powering the Network

**Setup:**
- Chain Block A and B connected
- You place a Lever next to Block B and flip it ON

**What Happens:**

1. **Minecraft Core:** Lever updates, calls `updateNeighborsAt()`
2. **RedstoneChainBlock.java (Line 194-206):** Block B receives `neighborChanged()`, calls `chain.updateSignalInNetwork()`
3. **RedstoneChainEntity.java (Line 477-505):**
   - `computeNetworkInputPower()` finds Lever signal = 15
   - `cachedInputSignal = 15`
   - `applySignalToNetwork(15)` sets both blocks to POWER = 15
4. **RedstoneChainRenderer.java (Line 70-94):** Cable re-rendered, color changes to bright red!

---

### Scenario 4: Breaking a Connection

**Setup:**
- Chain blocks A—B—C connected, all POWER = 15
- You break Block B

**What Happens:**

1. **RedstoneChainBlock.java (Line 463-478):** `onRemove()` called
2. Tells Block A and C to remove their connections to B
3. **RedstoneChainEntity.java (Line 161-174):** Both remove connections, invalidate networks
4. **Line 204-227:** BFS marks all connected blocks as dirty
5. **Next Update:** Networks rebuild separately: {A} and {C}
6. Both lose power (lever was next to B which is gone)

---

## 🎓 NeoForge Concepts Used

1. **DeferredRegister** - Modern lazy registration ([docs](https://docs.neoforged.net/docs/concepts/registries/))
2. **Block Entities** - Smart blocks with data storage ([docs](https://docs.neoforged.net/docs/blockentities/))
3. **BlockEntityRenderer** - Custom rendering ([docs](https://docs.neoforged.net/docs/1.21.1/blockentities/ber/))
4. **Data Components** - New 1.21 item data system
5. **Client-Side Code Separation** - `dist = Dist.CLIENT`
6. **Network Synchronization** - Server-client data sync
7. **Redstone Integration** - Signal source methods

---

## 🎨 Cool Technical Details

### 1. The Sagging Cable Math

```java
double curve = Math.sin(t * Math.PI) * -0.4;
```

Creates a perfect symmetric sag! As `t` goes 0→1:
- t=0: no sag (0)
- t=0.5: max sag (-0.4 blocks)
- t=1: no sag (0)

Real physics uses catenary curves, but sine is close enough and faster!

---

### 2. Preventing Feedback Loops

```java
if (isUpdating) return;
isUpdating = true;
try { ... } finally { isUpdating = false; }
```

Without this, blocks would infinitely update each other!

---

### 3. Double-Sided Rendering

```java
.setCullState(new RenderStateShard.CullStateShard(false))
```

Draws both front and back faces so cables are visible from any angle!

---

### 4. Cross Product for Perpendicular Vectors

```java
Vec3 right = dir.cross(up).normalize();
```

Creates a coordinate system oriented along the cable direction!

---

### 5. Breadth-First Search Complexity

**Time:** O(V + E) where V = vertices, E = edges
**Space:** O(V)

Used for finding all connected blocks efficiently!

---

## 🐛 Potential Issues & Improvements

### Issue 1: Constant Mismatch
Entity uses 24 blocks/3 connections, connector uses 20 blocks/5 connections. Inconsistent!

### Issue 2: No Cable Breaking
Cables stretch infinitely even if blocks move apart.

### Issue 3: Performance with Large Networks
100-block network checks 600 neighbors every second.

### Issue 4: No Visual Feedback
Can't tell when at max connections without trying to add more.

---

## 🚀 Ideas for Future Enhancements

1. **Different Cable Types** (copper, gold, redstone)
2. **Power Loss Over Distance** (realistic decay)
3. **Animated Sparks** (particle effects on powered cables)
4. **Sound Effects** (clink when connecting)
5. **Cable Textures** (instead of solid colors)
6. **Config Options** (max distance, sag amount, etc.)
7. **Right-Click to Remove** (GUI for connection management)
8. **Multi-Block Structures** (tall posts like telephone poles)

---

## 📊 Complete Class Hierarchy

```
MinecraftPlayground (Main Mod Class)
├── Registers all content
└── Listens to mod events

MinecraftPlaygroundClient (Client-Only)
├── Registers renderers
└── Client setup

Blocks:
└── RedstoneChainBlock
    ├── Redstone logic
    ├── Creates block entities
    └── Adjacent connections

Items:
├── RedstoneChainBlockItem (shimmer effect)
└── RedstoneChainConnector (connection tool)

Block Entities:
└── RedstoneChainEntity
    ├── Stores connections
    ├── Manages networks
    ├── Calculates power
    └── Syncs to clients

Renderers:
└── RedstoneChainRenderer
    ├── Draws curved cables
    └── Power-based coloring
```

---

## 🎓 Key Algorithms Used

1. **Breadth-First Search (BFS)** - Find connected blocks, O(V+E)
2. **Network Flow** - Power distribution, O(V)
3. **Sine Wave Interpolation** - Cable sagging, O(1)
4. **Cross Product** - 3D geometry, O(1)
5. **Delayed Update** - Prevent flickering, O(1)

---

## 📖 Summary

Your **Hanging Redstone Wires** mod demonstrates:

✅ Modern NeoForge practices
✅ Advanced rendering techniques
✅ Complex game logic with BFS
✅ Proper redstone integration
✅ Client-server synchronization
✅ Professional polish

**Congratulations on building such a sophisticated mod!** 🎉

---

## 🔗 Sources

- [NeoForge BlockEntityRenderer Documentation](https://docs.neoforged.net/docs/1.21.1/blockentities/ber/)
- [NeoForge Block Entities Documentation](https://docs.neoforged.net/docs/blockentities/)
- [NeoForge Registries Documentation](https://docs.neoforged.net/docs/concepts/registries/)
- [McJty's NeoForge Tutorial](https://www.mcjty.eu/docs/1.20.4_neo/ep2)
