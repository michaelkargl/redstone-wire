# RedstoneDetectorBlock GameTests

This directory contains GameTests for the RedstoneDetectorBlock functionality.

## Test Coverage

The test suite (`RedstoneDetectorTests.java`) includes the following test scenarios:

1. **testRedstoneSignalDetection**: Verifies that the detector correctly detects a redstone block signal (strength 15)
2. **testNoRedstoneSignal**: Verifies that the detector correctly reports no signal when no power source is present
3. **testRedstoneWireSignalStrength**: Tests detection of weakened signals through redstone wire
4. **testLeverPowerSource**: Tests detection of signals from a lever power source
5. **testMultipleRedstoneSources**: Verifies correct behavior when multiple redstone sources are adjacent
6. **testSignalRemoval**: Tests that the detector correctly updates when a signal source is removed

## Running the Tests

### Option 1: Run all GameTests via Gradle

Run all registered GameTests using the GameTestServer:

```bash
./gradlew runGameTestServer
```

This will:
- Launch a headless Minecraft server
- Execute all tests annotated with `@GameTest`
- Report results to the console
- Exit when complete

### Option 2: Run tests in-game

1. Start the client or server:
   ```bash
   ./gradlew runClient
   # or
   ./gradlew runServer
   ```

2. In-game, use the `/test` command to run tests:
   ```
   /test runall          # Run all tests
   /test run <testname>  # Run a specific test
   /test runset <class>  # Run all tests in a class
   ```

### Option 3: Run via IDE

1. Configure a run configuration for `runGameTestServer` in your IDE
2. Execute the run configuration
3. View test results in the console output

## Test Structure Templates

The tests use empty structure templates located in:
- `src/main/resources/data/minecraftplayground/structures/empty3x3x3.nbt`
- `src/main/resources/data/minecraftplayground/structures/empty5x5x5.nbt`

These templates provide empty test arenas where blocks can be placed programmatically during test execution.

## Debugging Tests

To debug a failing test:

1. Enable debug logging in the run configuration
2. Check the test output for assertion failures
3. Use the in-game `/test` command to run individual tests and observe behavior
4. Review server logs in `run/logs/latest.log`

## Expected Behavior

The RedstoneDetectorBlock should:
- Log redstone signal strength changes to the server log
- Send messages to all players indicating the signal strength detected
- Respond to neighbor block updates (redstone blocks, levers, wires, etc.)
- Report signal strength values from 0 (no signal) to 15 (maximum signal)
