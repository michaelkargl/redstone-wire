package tests;

import net.minecraft.gametest.framework.GameTestHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SpecFlow implements ISpecFlow {
    private static final Logger LOGGER = LogManager.getLogger();
    private final GameTestHelper helper;
    private int tick = 0;

    public SpecFlow(GameTestHelper helper) {
        this.helper = helper;
    }

    public ISpecFlow given(String description, Runnable action, int runAtTick) {
        this.helper.runAtTickTime(runAtTick, () -> {
            LOGGER.info("({}) GIVEN: {}", runAtTick, description);
            action.run();
        });
        this.tick = runAtTick;
        return this;
    }

    public ISpecFlow given(String description, Runnable action) {
        return given(description, action, tick + 1);
    }

    public ISpecFlow when(String description, Runnable action, int runAtTick) {
        this.helper.runAtTickTime(runAtTick, () -> {
            LOGGER.info("({}) WHEN: {}", runAtTick, description);
            action.run();
        });
        this.tick = runAtTick;
        return this;
    }

    public ISpecFlow when(String description, Runnable action) {
        return when(description, action, tick + 1);
    }

    public ISpecFlow then(String description, Runnable action, int runAtTick) {
        this.helper.runAtTickTime(runAtTick, () -> {
            LOGGER.info("({}) THEN: {}", runAtTick, description);
            action.run();
        });
        this.tick = runAtTick;
        return this;
    }

    public ISpecFlow then(String description, Runnable action) {
        return then(description, action, tick + 1);
    }

    public ISpecFlow and(String description, Runnable action, int runAtTick) {
        this.helper.runAtTickTime(runAtTick, () -> {
            LOGGER.info("({})   AND: {}", runAtTick, description);
            action.run();
        });
        this.tick = runAtTick;
        return this;
    }

    public ISpecFlow and(String description) {
        return and(description, () -> {});
    }

    public ISpecFlow and(String description, Runnable action) {
        return and(description, action, tick + 1);
    }
}
