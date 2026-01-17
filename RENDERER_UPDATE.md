# Redstone Chain Renderer Update

## Changes Made

The RedstoneChainRenderer has been updated to use the reference implementation from OverheadRedstoneWires, which provides better rendering performance and compatibility.

## Key Improvements

### 1. Custom RenderType
- Now uses a custom `CHAIN_RENDER_TYPE` with `NEW_ENTITY` vertex format
- Supports position, color, and lightmap data
- Uses `PositionColorLightmapShader` for proper lighting
- Culling disabled to render chains from all viewing angles

### 2. Rendering Method
The renderer now uses the reference approach:
- **Vertex method**: Uses `addVertex()` with proper method chaining
- **Color**: Applied via `setColor()` instead of vertex attributes
- **Normals**: Calculated per-face and applied via `setNormal()`
- **Lighting**: Applied via `setLight()` using packed light values
- **Overlay**: Applied via `setOverlay()` for proper highlighting

### 3. Chain Appearance
- **Segments**: 12 segments per chain connection
- **Thickness**: 0.015 units (thin like real chains)
- **Sag**: Catenary curve with 0.4 amplitude for realistic hanging
- **Color**: 
  - Powered (power > 0): Red tint (0.6-1.0 red, 0.1 green, 0.1 blue)
  - Unpowered: Gray (0.3, 0.3, 0.3)

### 4. Geometry
Each chain segment is rendered as a rectangular cuboid with:
- 8 vertices (corners)
- 6 faces (top, bottom, front, back, left, right)
- Proper normal calculation for lighting
- Direction-aligned to follow the chain path

## Technical Details

### RenderType Configuration
```java
RenderType.create(
    "redstone_chain_render",
    DefaultVertexFormat.NEW_ENTITY,
    VertexFormat.Mode.TRIANGLES,
    256,  // buffer size
    false, // no normals
    true,  // needs sorting
    RenderType.CompositeState.builder()
        .setShaderState(PositionColorLightmapShader)
        .setCullState(disabled)
        .setLightmapState(enabled)
        .setOverlayState(enabled)
        .createCompositeState(false)
)
```

### Vertex Building
```java
builder.addVertex(matrix, x, y, z)
    .setColor(r, g, b, a)
    .setUv(u, v)
    .setOverlay(overlay)
    .setLight(light)
    .setNormal(nx, ny, nz)
```

## Differences from Reference

1. **Power-based coloring**: Added redstone power detection to change chain color
2. **Duplicate prevention**: Only renders from lower BlockPos to prevent double-rendering
3. **Entity type**: Uses `RedstoneChainEntity` instead of `RedstoneCableEntity`

## Testing

To test the renderer:
1. Run the client: `./gradlew runClient`
2. Place two redstone chain blocks
3. Connect them with the chain connector
4. Apply redstone power to one chain
5. Observe:
   - Chain renders between blocks with realistic sag
   - Chain turns red when powered
   - Proper lighting and shading
   - No z-fighting or visual glitches

## Performance

- Renders at 128 block view distance
- 12 segments per chain = 72 vertices per connection
- Culling disabled for proper visibility from all angles
- Buffer size: 256 (can handle multiple connections efficiently)

## Future Enhancements

Potential improvements:
- Add animated chain links (rotating/swaying)
- Variable thickness based on power level
- Particle effects when power changes
- Sound effects for power transmission
- Texture mapping for chain link detail
