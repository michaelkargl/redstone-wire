# Redstone Chain System - Usage Guide

## Overview
The Redstone Chain system allows you to transmit redstone signals over long distances (up to 24 blocks between chain blocks) without signal loss. The chains render visually between connected blocks and sag realistically like real chains.

## Basic Components

### 1. Redstone Chain Block
- A vertical chain-like block that can receive and transmit redstone signals
- Can connect to up to 3 other chain blocks using wire connections
- Supports both:
  - **Adjacent redstone** (traditional neighbor connections)
  - **Long-range wire connections** (up to 24 blocks away)

### 2. Redstone Chain Connector
- Item used to create wire connections between chain blocks
- Uses a lead/rope texture in inventory
- Can be stacked (64 per stack)
- Consumes one item per connection created

---

## How to Input Redstone Power to a Chain Block

### Method 1: Direct Adjacent Input (Traditional)
Place any redstone power source next to a chain block:
- **Redstone torch** next to the chain
- **Lever** attached to a block next to the chain
- **Redstone block** next to the chain
- **Redstone dust** powering the block below/next to the chain
- **Repeater** pointing at the chain
- **Comparator** pointing at the chain
- **Observer** facing the chain
- **Button/Pressure Plate** connected via redstone dust

Example setup:
```
[Lever] → [Chain Block] → Signal propagates through network
```

### Method 2: Powered Block Below
Place a chain block on top of a powered solid block:
```
[Redstone Torch]
[Solid Block]
[Chain Block] ← Gets powered from below
```

### Method 3: Redstone Wire Input
Connect redstone dust to a block adjacent to the chain:
```
[Redstone Dust] → [Solid Block] → [Chain Block] (adjacent)
```

---

## How to Output Redstone Power from a Chain Block

### Method 1: Direct Output to Adjacent Blocks
The chain block acts as a redstone power source when powered. You can:
- Power redstone dust placed next to it
- Power adjacent blocks (which then power other redstone components)
- Trigger redstone lamps, pistons, doors, etc.

Example:
```
[Chain Block (powered)] → [Redstone Lamp] ← Lights up!
[Chain Block (powered)] → [Piston] ← Extends!
```

### Method 2: Repeater/Comparator Output
Connect a repeater or comparator to the chain block:
```
[Chain Block (powered)] → [Repeater] → [Your Circuit]
```

### Method 3: Redstone Dust
Place redstone dust next to the chain block:
```
[Chain Block (powered)] → [Redstone Dust] → [Your Circuit]
```

---

## How to Create Wire Connections

### Step-by-Step Connection Process:

1. **Place two or more Redstone Chain blocks** (up to 24 blocks apart)
   ```
   [Chain Block A]
          ↕ (up to 24 blocks)
   [Chain Block B]
   ```

2. **Get a Redstone Chain Connector item**

3. **Shift-click the first chain block**
   - You'll see a message: "First point saved: (x, y, z)"
   - The connector item now remembers this position
   - Check the tooltip to see the saved position

4. **Shift-click the second chain block**
   - You'll see: "Connected (x1, y1, z1) to (x2, y2, z2) (N blocks)"
   - A visible chain will render between the two blocks
   - One connector item is consumed (unless in creative mode)
   - Both blocks are now part of the same redstone network

5. **Optional: Connect more chains**
   - Each chain block can have up to 3 connections
   - You can create complex networks

6. **To clear a saved position**
   - Shift-right-click in the air (not on a block)
   - Message: "Connection cleared"

---

## Complete Example Setups

### Example 1: Simple Long-Distance Signal
```
[Lever] → [Chain A] ~~~chain~~~ [Chain B] → [Redstone Lamp]
                    (20 blocks apart)
```
1. Place Chain A and Chain B 20 blocks apart
2. Use connector to link them
3. Place lever next to Chain A
4. Place redstone lamp next to Chain B
5. Flip the lever → Lamp lights up instantly!

### Example 2: Vertical Signal Transmission
```
        [Chain Top] (y=100)
            |||
        (chain wire)
            |||
      [Chain Bottom] (y=70)
           ↓
    [Redstone Lamp]
```
1. Place chains at different heights
2. Connect with connector item
3. Power the top chain from any source
4. Bottom chain outputs the signal

### Example 3: Hub Network (3 connections per chain)
```
        [Chain North]
              |
    [Chain West]─[Chain Hub]─[Chain East]
              |
        [Chain South]
```
Note: Each chain can only have 3 connections maximum!

---

## Signal Properties

### Power Level
- The chain network **preserves the full signal strength** (0-15)
- No signal loss over distance (unlike redstone dust which loses 1 per block)
- If multiple inputs provide different power levels, the **maximum** is used

### Update Speed
- Chains update every 20 ticks (1 second) by default
- When neighbors change, the update is immediate via `neighborChanged()`
- Network recalculation happens automatically

### Network Behavior
- All connected chains form a single network
- The network finds the strongest input signal from ANY connected source
- All chains in the network output that same signal strength
- Breaking one connection splits the network into separate networks

---

## Advanced Usage

### Splitting Networks
To disconnect chains:
- Currently, you need to break the chain block to remove connections
- Breaking a chain block automatically removes it from all connected chains' connection lists

### Maximum Distance
- **24 blocks** between any two connected chains
- Measured as straight-line distance (Euclidean distance)
- Error message appears if you try to connect chains too far apart

### Troubleshooting

**Chain not receiving power:**
- Check if the power source is adjacent or powering an adjacent block
- Verify the chain block exists (not destroyed)
- Wait up to 1 second for the network to update

**Chain not outputting power:**
- Verify the network is receiving input from somewhere
- Check that output components are directly adjacent
- Try placing a repeater to boost/redirect the signal

**Cannot create connection:**
- "Max distance: 20 blocks" → Chains are too far apart
- "Maximum connections (3)" → One chain already has 3 connections
- Make sure you're shift-clicking on actual chain blocks

**Chains not rendering:**
- This is a client-side render issue
- Try re-logging or moving away and back
- Verify the block entity exists (F3 debug screen)

---

## Technical Details

### Block Properties
- **Block State:** `power` (0-15) - Current redstone signal strength
- **Block Entity:** Stores connection list, network cache
- **Collision Box:** Small vertical cylinder (like vanilla chain)

### Comparator Support
- Chain blocks provide analog output to comparators
- Comparator reads the current power level (0-15)

### Network Algorithm
- Uses BFS (Breadth-First Search) to find all connected chains
- Merges networks when new connections are created
- Invalidates and rebuilds when connections are removed

---

## Recipe Ideas (for future implementation)

While recipes aren't implemented yet, here are some suggestions:

**Redstone Chain Block:**
```
  [Chain]
[Redstone]
  [Chain]
```

**Redstone Chain Connector:**
```
[String][String][String]
         [Lead]
```

---

## Summary Commands

**To power a chain:**
- Place redstone torch/lever/block next to it
- Power a block adjacent to it with redstone dust
- Connect it to another powered chain in the network

**To get power from a chain:**
- Place redstone dust/lamp/piston/door next to it
- Use a repeater/comparator pointing away from it
- Connect it to another chain that feeds your circuit

**To connect chains:**
1. Shift-click first chain with connector
2. Shift-click second chain with connector
3. Visual chain appears, one connector consumed
