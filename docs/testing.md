# Game Tests

This directory contains game tests for the Redstone Wire mod.
Game tests are used to verify the behavior of the mod in a Minecraft world.
They represent the end-to-end behavior of the mod (how the mod react in a real minecraft instance).

## Create a new test

Minecraft 1.21.5 removed the old `@GameTest` and `@GameTestHolder` annotations.
A test now consists of a Java function, a registered function ID, a test-instance
JSON file, and an NBT structure.

1. Add a `public static void` method accepting a `GameTestHelper` to a class in the `tests` package.
2. Register that method in `RedstoneWire.TEST_FUNCTIONS`. Use a stable, lowercase ID.
3. Add `data/redstone_wire/test_instance/<id>.json`. A minimal function-based test looks like:
   ```json
   {
     "type": "minecraft:function",
     "function": "redstone_wire:<id>",
     "environment": "minecraft:default",
     "structure": "redstone_wire:<id>",
     "max_ticks": 100
   }
   ```
4. Start the dev Minecraft client.
5. Run these commands in Minecraft chat:
   ```sh
   # create a new test structure to build your test scenario in
   # example: redstoneinputblocktests.redstonesignaltest
   /test create redstone_wire:<id>

   # after building the test scene, export the nearest test
   /test exportclosest
   ```
   Minecraft prints the exported file path in chat. Keep the resulting NBT file at
   `src/main/resources/data/redstone_wire/structure/<id>.nbt`.
   
## Run the tests

### From the command line

Run `gradlew runGameTestServer`, or use IntelliJ's `GameTestServer` run configuration.

## Ingame

Start the dev minecraft instance and either

```sh
/test runall
/test run redstone_wire:<id>
/test runclosest
or just press the button on the command block
```