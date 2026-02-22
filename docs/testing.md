# Game Tests

This directory contains game tests for the Redstone Wire mod.
Game tests are used to verify the behavior of the mod in a Minecraft world.
They represent the end-to-end behavior of the mod (how the mod react in a real minecraft instance).

## Create a new test

1. Create a new `@GameTestHolder("redstone_wire")` annotated class in the `tests` package
1. Create a `@GameTest()` annotated method
1. Start the dev minecraft instance
1. Run these commands in the minecraft chat:
   ```sh
   # create a new test structure to build your test scenario in
   # example: redstoneinputblocktests.redstonesignaltest
   /test create <lowercase_class_name>.<lowercase_test_name>
   # Export the test structure
   # You should be able to find the nbt file in `run/saves/<world>/generated/minecraft/structures`
   In the structure block interface, press the right "Save" button
   
   # copy the nbt file to the `resources` directory
   cp <path_to_nbt_file> src/main/resources/data/redstone_wire/structure
   ```
   
## Run the tests

### From the command line

1. Run `run_tests.sh`

## Ingame

Start the dev minecraft instance and either

```sh
/test run all
/test run <lowercase_class_name>.<lowercase_test_name>`
/test runclosest
or just press the button on the command block
```