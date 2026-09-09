# Game Tests

Game tests verify the behavior of the mod in a real Minecraft world — they are
end-to-end tests, not unit tests. The mod has no unit tests; everything
interesting about it involves the game's block, redstone, and tick machinery.

Test code lives in `src/main/java/tests/` — **not** `src/test/java`. GameTest
functions have to be registered by the mod at runtime, so they must ship in the
main source set.

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
   
## Writing the test body

Tests use a small `SpecFlow` given/when/then helper (`tests/SpecFlow.java`) that
schedules each step on a game tick — most assertions need time to pass before
redstone settles:

```java
new SpecFlow(helper)
    .given("A high power lever in off position", () -> TestHelpers.assertLeverIsOff(helper, leverPos))
    .when("Toggling the low power lever",        () -> TestHelpers.pullLever(helper, lowLeverPos))
    .then("The output signal is low",            () -> TestHelpers.assertRedstoneWire(helper, outPos, 7), 10)
    .then("Test succeeds", helper::succeed);
```

Each step advances one tick by default; pass an explicit tick number as the last
argument when you need to wait longer. `TestHelpers` holds the shared assertions
(`pullLever`, `assertLeverIsOn`, `assertRedstoneWire`, `assertRedstoneLampIsLit`).

The `max_ticks` in the test-instance JSON must exceed the highest tick your flow
schedules, or the test times out before its last assertion runs.

## Run the tests

### From the command line

```sh
./run_tests.sh          # all tests
./run_tests.sh clean    # clean build, then all tests
./run_tests.sh build    # build the mod, then all tests
```

That wraps `./gradlew runGameTestServer`, which you can also call directly, or
run via IntelliJ's `GameTestServer` run configuration.

### In CI

`.github/workflows/build.yml` runs `./run_tests.sh all` on every push and pull
request, after the build job. Test logs are uploaded as an artifact on both
success and failure.

## Ingame

Start the dev minecraft instance and either

```sh
/test runall
/test run redstone_wire:<id>
/test runclosest
or just press the button on the command block
```

## Debugging a failing test

1. Run the single failing test in-game with `/test run redstone_wire:<id>` and
   watch it play out — the structure is placed in the world and you can see it.
2. `SpecFlow` logs every given/when/then step with its tick number, so the last
   line before the failure tells you how far it got.
3. Check `run/logs/latest.log` for the full server-side output.
4. If the test fails only in `runGameTestServer` but passes in-game, suspect a
   client/server split — the headless server never runs client code.