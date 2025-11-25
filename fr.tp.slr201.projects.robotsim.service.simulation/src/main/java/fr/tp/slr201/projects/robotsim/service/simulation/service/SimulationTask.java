package fr.tp.slr201.projects.robotsim.service.simulation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.tp.inf112.projects.robotsim.model.Factory;

/**
 * Wrapper for a running simulation instance.
 */
public class SimulationTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SimulationTask.class);

    private final Factory factory;
    private volatile boolean running = false;
    private Thread thread;

    public SimulationTask(final Factory factory) {
        this.factory = factory;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this, "simulation-" + (factory.getId() == null ? System.identityHashCode(factory) : factory.getId()));
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        // ask factory to stop so component threads will exit their loops
        try {
            factory.stopSimulation();
        } catch (final Exception e) {
            LOG.warn("Error stopping factory simulation", e);
        }

        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public Factory getFactory() {
        return factory;
    }

    @Override
    public void run() {
        try {
            LOG.info("Starting simulation for factory id='{}'", factory.getId());
            // Use the model's startSimulation to spawn component threads
            factory.startSimulation();

            // Simple manual motion: move first component (if any) diagonally and bounce.
            int dx = 1;
            int dy = 1;

            // Keep task alive while factory indicates simulation is started and while not stopped
            while (running && factory.isSimulationStarted()) {
                try {
                    Thread.sleep(200);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // Manual movement to demonstrate position changes without relying on component-specific logic.
                try {
                    if (!factory.getComponents().isEmpty()) {
                        final fr.tp.inf112.projects.robotsim.model.Component c = factory.getComponents().get(0);
                        if (c.getPosition() != null) {
                            int x = c.getPosition().getxCoordinate();
                            int y = c.getPosition().getyCoordinate();
                            // Bounce on factory bounds
                            if (x + dx < 0 || x + dx > factory.getWidth() - c.getWidth()) {
                                dx = -dx;
                            }
                            if (y + dy < 0 || y + dy > factory.getHeight() - c.getHeight()) {
                                dy = -dy;
                            }
                            c.getPosition().setxCoordinate(x + dx);
                            c.getPosition().setyCoordinate(y + dy);
                            // Notify observers so Kafka events are published
                            try { factory.notifyObservers(); } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception moveEx) {
                    LOG.debug("Movement step failed: {}", moveEx.getMessage());
                }
            }
        } catch (final Exception e) {
            LOG.error("Unhandled error while running simulation task", e);
        } finally {
            LOG.info("Simulation task stopping for factory id='{}'", factory.getId());
            try { factory.stopSimulation(); } catch (final Exception ignored) {}
            running = false;
        }
    }
}
