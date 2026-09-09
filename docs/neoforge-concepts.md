# NeoForge Concepts

A ground-up introduction to NeoForge modding, written for developers coming from
C#/F#. This is the **conceptual** half of the documentation — it explains the
platform, not this mod. For how Redstone Wire itself is put together, read
[Architecture](architecture.md) afterwards.

> Everything here is general NeoForge knowledge and stays true across mod
> versions. Where an API changed recently, the version is called out inline.

## 🎓 Essential NeoForge Concepts for C#/F# Developers

### Java vs C# Quick Reference

As a C#/F# developer, here are the key differences you'll encounter:

| Concept | C# | Java |
|---------|----|----- |
| **Properties** | `public int Value { get; set; }` | Explicit getters/setters: `getValue()`, `setValue()` |
| **Null Safety** | Nullable reference types (`string?`) | Everything nullable by default (use `@Nullable` annotations) |
| **Generics** | Full reification | Type erasure at runtime (use `<?>` wildcards) |
| **Lambda Syntax** | `x => x * 2` | `x -> x * 2` (same!) |
| **Method References** | `Method` | `Class::method` |
| **Collections** | LINQ (`list.Where(x => x > 5)`) | Streams (`list.stream().filter(x -> x > 5)`) |
| **Events** | Built-in `event` keyword | Manual event bus pattern |
| **Interfaces** | Can't have implementations (pre-C#8) | Can have default methods |
| **Package Management** | NuGet | Gradle/Maven |

**Key Java Gotcha:** No properties! You'll see a lot of `getBlockState()`, `setChanged()`, etc.

---

### Minecraft Architecture Fundamentals

#### 1. **Client-Server Architecture (Critical!)**

Minecraft always runs in a client-server architecture, even in singleplayer:

```
Singleplayer:        Multiplayer:
┌─────────────┐      ┌─────────────┐     ┌─────────────┐
│   Client    │      │   Client    │     │  Dedicated  │
│  (Renders)  │      │  (Renders)  │────▶│   Server    │
│             │      │             │     │  (Logic)    │
│  Integrated │      └─────────────┘     └─────────────┘
│   Server    │               │                 │
│  (Logic)    │               └─────────┬───────┘
└─────────────┘                         │
                                   (Network)
```

**Critical Rules:**
- `level.isClientSide` is your friend - check it everywhere!
  - `true` = Client (rendering, particles, sounds)
  - `false` = Server (logic, data, validation)
- **Server is authoritative** - all logic runs there
- **Client is dumb** - only renders what server tells it
- Client-only code must be in classes marked `@Mod(dist = Dist.CLIENT)`
- NEVER import client-only classes (like `Minecraft.getInstance()`) in server code

**C# Analogy:** Like ASP.NET MVC where controller logic (server) is separate from Razor views (client).

---

#### 2. **The Mod Loading Lifecycle**

NeoForge mods load in phases (similar to .NET application startup):

```
1. CONSTRUCT (Constructor)
   ├─ Register DeferredRegisters
   ├─ Register event listeners
   └─ Register configs

2. COMMON SETUP (FMLCommonSetupEvent)
   ├─ Runs on both client and server
   └─ Cross-mod communication setup

3. CLIENT SETUP (FMLClientSetupEvent)
   ├─ ONLY on client
   ├─ Register renderers
   ├─ Register key bindings
   └─ Register screens/GUIs

4. SERVER SETUP (FMLDedicatedServerSetupEvent)
   └─ ONLY on dedicated servers

5. COMPLETE (FMLLoadCompleteEvent)
   └─ Everything is ready
```

**Your mod example:**
```java
@Mod(RedstoneWire.MODID)
public class RedstoneWire {
    public RedstoneWire(IEventBus modEventBus, ModContainer modContainer) {
        // CONSTRUCT phase - register everything
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // COMMON SETUP phase - post-registration init
    }
}
```

---

#### 3. **Event-Driven Programming**

NeoForge uses an **event bus system** (like C# events but more powerful):

**Two Event Buses:**

1. **Mod Event Bus** - Mod-specific events (registration, setup)
   ```java
   modEventBus.addListener(this::commonSetup);
   ```

2. **Forge Event Bus** - Game events (player login, block break, etc.)
   ```java
   NeoForge.EVENT_BUS.register(this);

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
       // Handle event
   }
   ```

**C# Analogy:**
```csharp
// C# events
button.Click += OnButtonClick;

// Java NeoForge events
NeoForge.EVENT_BUS.register(this);
@SubscribeEvent
public void onBlockBreak(BlockEvent.BreakEvent event) { }
```

**Important:** Events can be **canceled** to prevent default behavior!
```java
@SubscribeEvent
public void onBlockBreak(BlockEvent.BreakEvent event) {
    if (someCondition) {
        event.setCanceled(true); // Block won't break
    }
}
```

---

#### 4. **The Registry System (DeferredRegister)**

Minecraft has a **global registry** for all game objects (blocks, items, entities, etc.). NeoForge provides `DeferredRegister` for type-safe registration.

**The Old Way (pre-1.19.2):**
```java
@ObjectHolder("modid:my_block")
public static Block MY_BLOCK; // Magic injection!
```

**The Modern Way (NeoForge 1.21):**
```java
public static final DeferredRegister.Blocks BLOCKS =
    DeferredRegister.createBlocks(MODID);

public static final DeferredBlock<MyBlock> MY_BLOCK =
    BLOCKS.register("my_block", () -> new MyBlock(Properties.of()));

// In constructor:
BLOCKS.register(modEventBus);
```

**C# Analogy:** Like dependency injection with `IServiceCollection`:
```csharp
// C#
services.AddSingleton<IMyService, MyService>();

// Java NeoForge
BLOCKS.register("my_block", () -> new MyBlock());
```

**Why Deferred?**
- Registries lock after mod loading
- Deferred registration happens at the right time automatically
- Type-safe (compile-time checks)
- No null references

**Registry Naming:**
- Registry name: `"my_block"` → Full ID: `"modid:my_block"`
- Must be lowercase, use underscores (not camelCase!)

---

#### 5. **Blocks vs Block States vs Block Entities**

This is confusing at first! Three different concepts:

**Block** (The class definition)
```java
public class MyBlock extends Block {
    // Defines BEHAVIOR for all instances
    // Like a C# class definition
}
```
- Singleton (one instance for all blocks of this type)
- Defines what the block CAN do
- **C# Analogy:** Like a `class Button` definition

**BlockState** (The specific configuration)
```java
BlockState state = block.defaultBlockState()
    .setValue(POWER, 15)
    .setValue(FACING, Direction.NORTH);
```
- Immutable data object
- Stores properties (power level, facing direction, etc.)
- Different states of the SAME block
- **C# Analogy:** Like an immutable `record` or value tuple

**Block Entity** (Tile Entity - Complex data storage)
```java
public class MyBlockEntity extends BlockEntity {
    private List<BlockPos> connections = new ArrayList<>();
    // Stores complex mutable data that doesn't fit in BlockState
}
```
- Mutable data container
- One instance per block position
- Stores complex data (inventories, connections, NBT)
- **C# Analogy:** Like an instance of a class attached to each block

**When to use what?**

| Use Case | Solution |
|----------|----------|
| Simple property (0-15 power) | BlockState property |
| Rotation/Facing | BlockState property |
| Complex data (list of connections) | Block Entity |
| Inventory (chest) | Block Entity |
| Per-tick logic | Block Entity with ticker |
| Visual-only differences | BlockState (use models) |

**Example from your mod:**
```java
// BlockState stores power (0-15)
state.setValue(POWER, 15)

// BlockEntity stores connections (complex list)
blockEntity.getConnections() // Returns List<BlockPos>
```

---

#### 6. **NBT and Data Components**

**NBT (Named Binary Tag)** is Minecraft's serialization format (like JSON but binary):

```java
// Old way (still used for block entities)
CompoundTag tag = new CompoundTag();
tag.putInt("x", 100);
tag.putInt("y", 64);
tag.putString("name", "Test");

// Reading
int x = tag.getInt("x");
```

**C# Analogy:**
```csharp
// NBT is like BinaryFormatter or JSON
var data = new Dictionary<string, object> {
    {"x", 100},
    {"y", 64},
    {"name", "Test"}
};
```

**Data Components (NEW in 1.21!)**

Replaces item NBT with a type-safe system:

```java
// OLD (pre-1.21): Item NBT
CompoundTag nbt = stack.getOrCreateTag();
nbt.putInt("value", 42);

// NEW (1.21+): Data Components
public static final Supplier<DataComponentType<CompoundTag>> LINK_DATA =
    DATA_COMPONENT_TYPES.register("link_data",
        () -> DataComponentType.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)  // Saves to disk
            .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)  // Syncs to client
            .build());

// Usage
stack.set(LINK_DATA, tag);  // Type-safe!
CompoundTag tag = stack.get(LINK_DATA);
```

**Benefits:**
- Type-safe (no more `getOrCreateTag()` everywhere)
- Automatic network sync
- Better performance
- Cleaner API

---

#### 7. **The Rendering Pipeline**

Minecraft uses **OpenGL** for rendering (via LWJGL). The pipeline:

```
┌─────────────────┐
│  Game Logic     │ (Server + Client)
└────────┬────────┘
         │
    ┌────▼─────┐
    │  Client  │
    └────┬─────┘
         │
┌────────▼────────────┐
│ Block Model System  │ (JSON models for static blocks)
└────────┬────────────┘
         │
┌────────▼────────────┐
│ BlockEntityRenderer │ (Custom rendering - your cables!)
│  (BER)              │
└────────┬────────────┘
         │
┌────────▼────────────┐
│ PoseStack + Buffers │ (Matrix transformations)
└────────┬────────────┘
         │
┌────────▼────────────┐
│  Vertex Consumer    │ (Add vertices with position/color/UV)
└────────┬────────────┘
         │
┌────────▼────────────┐
│   RenderType        │ (Shader + render state)
└────────┬────────────┘
         │
┌────────▼────────────┐
│      OpenGL         │ (GPU draws triangles)
└─────────────────────┘
```

> **Changed in 1.21.9.** The old single-method `render(...)` BER was replaced by a
> two-phase model, and `RenderType.create(...)` gave way to `RenderPipeline`.
> Older tutorials you find online will still show the pre-1.21.9 shape.

**The two-phase BER (current):**

```java
public class MyRenderer implements BlockEntityRenderer<MyBlockEntity, MyRenderState> {

    // Phase 1 — runs on the server-synced block entity. Copy out plain values.
    @Override
    public void extractRenderState(MyBlockEntity entity, MyRenderState state,
                                   float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        state.connections = entity.getConnections();
    }

    // Phase 2 — runs on the render thread. Touch only `state`, never the entity.
    @Override
    public void submit(MyRenderState state, PoseStack stack,
                       SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        nodeCollector.submitCustomGeometry(stack, MY_RENDER_TYPE,
                (pose, builder) -> { /* emit vertices */ });
    }
}
```

**Why the split?** Extraction reads game state; submission runs later, off-thread,
against an immutable snapshot. Reaching back into the block entity from `submit`
is a data race — this is the single most common way to crash a modern BER.

See [Rendering](rendering.md) for this mod's actual implementation.

**Key Concepts:**

1. **PoseStack** - Transformation matrix stack (like OpenGL's glPushMatrix/glPopMatrix)
   ```java
   stack.pushPose();    // Save state
   stack.translate(x, y, z);  // Move
   stack.scale(2, 2, 2);      // Scale
   // ... render ...
   stack.popPose();     // Restore state
   ```

2. **VertexConsumer** - Where you add vertices
   ```java
   builder.addVertex(matrix, x, y, z)
       .setColor(r, g, b, a)
       .setUv(u, v)
       .setLight(packedLight)
       .setNormal(nx, ny, nz);
   ```

3. **RenderPipeline + RenderType** - Defines shader and render state. Since
   1.21.9 the shader/format/cull settings live on a `RenderPipeline` that must be
   registered via `RegisterRenderPipelinesEvent`; `RenderType` then wraps it.
   ```java
   public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
       .withLocation(Identifier.fromNamespaceAndPath(MODID, "pipeline/cable"))
       .withVertexShader("core/rendertype_leash")
       .withFragmentShader("core/rendertype_leash")
       .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
       .build();

   public static final RenderType RENDER = RenderType.create("my_render_type",
       RenderSetup.builder(PIPELINE).useLightmap().createRenderSetup());
   ```

**C# Analogy:** Like Unity's rendering pipeline or WPF's visual tree rendering.

---

#### 8. **Codecs and Serialization**

Minecraft 1.20+ uses **Codecs** for serialization (from Mojang's DataFixerUpper library):

```java
// Define how to serialize/deserialize
public static final Codec<MyData> CODEC = RecordCodecBuilder.create(instance ->
    instance.group(
        Codec.INT.fieldOf("x").forGetter(MyData::getX),
        Codec.INT.fieldOf("y").forGetter(MyData::getY),
        Codec.STRING.fieldOf("name").forGetter(MyData::getName)
    ).apply(instance, MyData::new)
);

// Used in Data Components
DataComponentType.<MyData>builder()
    .persistent(MyData.CODEC)  // Uses codec for saving
    .build();
```

**C# Analogy:** Like System.Text.Json with `JsonSerializer` or custom `JsonConverter<T>`.

**Why Codecs?**
- Type-safe serialization
- Automatic validation
- Built-in error handling
- Works with JSON, NBT, network packets

---

#### 9. **Packets and Network Communication**

When server needs to tell client something custom (beyond automatic sync):

**SimpleChannel (Traditional):**
```java
SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(...);

// Register packet
CHANNEL.registerMessage(id, MyPacket.class,
    MyPacket::encode,  // Serialize
    MyPacket::decode,  // Deserialize
    MyPacket::handle   // Handle on receiving side
);

// Send to client
CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MyPacket(...));
```

**Your mod uses automatic sync:**
```java
level.sendBlockUpdated(pos, state, state, 3);  // Flag 3 = update clients
```

This triggers:
1. `getUpdatePacket()` on server
2. Packet sent to clients
3. `getUpdateTag()` provides data
4. Client receives and updates

**C# Analogy:** Like SignalR or gRPC for client-server communication.

---

#### 10. **Tickers - Per-Tick Logic**

Blocks can run logic every game tick (20 ticks = 1 second):

```java
@Override
public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
    return type == MY_ENTITY.get() ? MyEntity::tick : null;
}

// In your block entity:
public static <T extends BlockEntity> void tick(Level level, BlockPos pos,
                                                 BlockState state, T be) {
    if (!(be instanceof MyEntity entity)) return;
    if (level.isClientSide) return;  // Server-side only!

    // Run every tick (20 times per second)
}
```

**Performance Warning:** Tickers run every tick! Use sparingly:
- Throttle expensive operations (check every 20 ticks, not every tick)
- Early return if nothing to do
- Avoid in multiple block entities if possible

---

#### 11. **Common Gotchas for C# Developers**

1. **Everything is mutable by default**
   ```java
   List<String> list = new ArrayList<>();
   someMethod(list);  // Method can modify your list!

   // To prevent: Make a copy
   someMethod(new ArrayList<>(list));
   ```

2. **No LINQ - Use Streams**
   ```csharp
   // C#
   var result = list.Where(x => x > 5).Select(x => x * 2).ToList();

   // Java
   List<Integer> result = list.stream()
       .filter(x -> x > 5)
       .map(x -> x * 2)
       .collect(Collectors.toList());
   ```

3. **Checked Exceptions**
   ```java
   try {
       Files.readAllLines(path);  // Must catch IOException
   } catch (IOException e) {
       // Handle
   }
   ```

4. **No operator overloading**
   ```csharp
   // C#: Vector3 a = b + c;

   // Java: Vec3 a = b.add(c);  // Must use methods
   ```

5. **No extension methods**
   ```csharp
   // C#: string.IsNullOrEmpty(str)

   // Java: StringUtils.isEmpty(str)  // Static utility classes everywhere
   ```

6. **Package-private is default**
   ```java
   class MyClass { }  // Package-private! Not public!
   public class MyClass { }  // Now it's public
   ```

---

### Quick Reference Card

**Common Patterns You'll See:**

```java
// 1. Registration
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
public static final DeferredBlock<MyBlock> MY_BLOCK = BLOCKS.register("my_block", MyBlock::new);

// 2. Block with properties
public static final IntegerProperty POWER = BlockStateProperties.POWER;
this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));

// 3. Block entity creation
@Override
public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new MyBlockEntity(pos, state);
}

// 4. Event subscription
@SubscribeEvent
public void onEvent(SomeEvent event) {
    if (!level.isClientSide) {  // Server-side check
        // Logic here
    }
}

// 5. NBT saving/loading
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putInt("myValue", this.myValue);
}

@Override
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    this.myValue = tag.getInt("myValue");
}

// 6. Client sync
private void syncToClient() {
    if (level != null && !level.isClientSide) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }
}
```

---

### Essential Documentation Links

- **NeoForge Docs:** https://docs.neoforged.net/
- **Minecraft Wiki (Technical):** https://minecraft.wiki/
- **Java 21 Docs:** https://docs.oracle.com/en/java/javase/21/
- **NeoForge Discord:** https://discord.neoforged.net/
- **McJty's Tutorial Series:** https://www.mcjty.eu/docs/1.20.4_neo/

---


## Next

You now have the platform. Read the [Usage Guide](usage.md) to see what this mod
does from a player's seat, then [Architecture](architecture.md) for how it is
built.
