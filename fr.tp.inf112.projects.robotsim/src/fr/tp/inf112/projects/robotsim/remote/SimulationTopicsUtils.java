package fr.tp.inf112.projects.robotsim.remote;

public final class SimulationTopicsUtils {
    private static final String TOPIC_PREFIX = "simulation-topic-";

    private SimulationTopicsUtils() {}

    public static String getTopicName(final String factoryId) {
        return TOPIC_PREFIX + factoryId;
    }
}
