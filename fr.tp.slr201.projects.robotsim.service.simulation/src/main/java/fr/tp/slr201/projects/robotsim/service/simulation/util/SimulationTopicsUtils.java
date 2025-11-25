package fr.tp.slr201.projects.robotsim.service.simulation.util;

public final class SimulationTopicsUtils {
    private SimulationTopicsUtils() {}

    private static final String TOPIC_PREFIX = "simulation-topic-";

    public static String getTopicName(final String factoryId) {
        return TOPIC_PREFIX + factoryId;
    }
}
