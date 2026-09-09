# Usage Guide

How to actually use the mod in-game. For how it works internally, see
[Architecture](architecture.md).

---

## The blocks

All three are in the **Redstone Wire** creative tab (there are no crafting
recipes yet — creative or `/give` only).

| Block | `/give` id | What it does |
| --- | --- | --- |
| Redstone Input | `redstone_wire:redstone_input` | Picks up a vanilla redstone signal and sends it into the network |
| Redstone Connector | `redstone_wire:redstone_connector` | Relay post — carries the signal onward |
| Redstone Output | `redstone_wire:redstone_output` | Emits the signal back out as vanilla redstone |

Each has a small antenna on top; that is where cables attach. All three face the
horizontal direction you were looking when you placed them.

---

## Linking blocks

You link with **plain redstone dust** — there is no special tool.

1. **Right-click a Connector** while holding redstone.
   → `Selected <pos> as source for connection`
2. **Right-click the other block** (a Connector, an Input, or an Output) with the
   same redstone.
   → `Connected <pos> to <pos>`

A cable is drawn between them and the link works in both directions.

The redstone is **not consumed** — it just carries the first-clicked position
until you complete the pair. If you wander off mid-link the position stays on
that stack; clicking a Connector again overwrites it.

> **Order matters for Input and Output blocks.** Input and Output blocks can only
> complete a link, never start one. Click the Connector *first*. If you click an
> Input or Output first you get `Right-click a ConnectorBlock first to select it`.

Break either block and the link is removed cleanly from both ends.

---

## A minimal working setup

```
   lever                                                        lamp
     │                                                            │
  ┌──┴───┐        ┌───────────┐        ┌───────────┐        ┌─────┴──┐
  │INPUT │~~~~~~~~│ CONNECTOR │~~~~~~~~│ CONNECTOR │~~~~~~~~│ OUTPUT │
  └──────┘        └───────────┘        └───────────┘        └────────┘
             ~~~ = cable, created with redstone dust
```

1. Place an **Input** block and put a lever, button, or redstone dust next to it.
2. Place one or more **Connector** blocks between the endpoints.
3. Place an **Output** block where you want the signal to come out, with a lamp,
   piston, or dust next to it.
4. Link them: Connector → Input, Connector → Connector, Connector → Output.
5. Flip the lever.

You do not need any Connectors at all — Input linked straight to Output works.
Connectors exist so you can branch and route.

---

## What the network does

**No signal loss, no distance limit.** Whatever power level goes into the Input
comes out of every Output on the network, unchanged. Power 7 in, power 7 out,
whether the hop is 3 blocks or 300.

**One input, many outputs.** Every Output reachable from the Input gets set on
every change. This is how you fan one lever out to a dozen doors.

**Reading the level.** A comparator pointed at an Input block reads its current
power level. Note that an Input block does **not** power its neighbours — it only
consumes. If you want a signal out, that is what the Output block is for.

---

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `Right-click a ConnectorBlock first to select it` | You started the link from an Input/Output. Click a Connector first. |
| `Saved position is not a ConnectorBlock` | The Connector you selected was broken or replaced before you finished. Click a live Connector again. |
| Output never fires | Check the Input is actually reading power — point a comparator at it. Then check the Output is genuinely linked (the cable should be visible). |
| Cable hangs to a strange height | Known cosmetic issue — Input/Output cables attach lower than Connector cables. See [Rendering](rendering.md). |
| Nothing renders | Cables are drawn by a block entity renderer; make sure you are not looking at it from beyond your render distance. |

---

## Current limitations

These are unfinished, not intentional:

- No maximum link distance and no cap on links per block — you can link across
  the world and build networks that are unpleasant to look at.
- Nothing prevents linking a block to itself.
- Cables are always dark red; they do not light up when powered.
