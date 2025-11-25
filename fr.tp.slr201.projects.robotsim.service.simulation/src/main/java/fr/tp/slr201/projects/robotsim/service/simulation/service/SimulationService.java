package fr.tp.slr201.projects.robotsim.service.simulation.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.Puck;
import fr.tp.inf112.projects.robotsim.model.ChargingStation;
import fr.tp.inf112.projects.robotsim.model.Conveyor;
import fr.tp.inf112.projects.robotsim.model.Room;
import fr.tp.inf112.projects.robotsim.model.Area;
import fr.tp.inf112.projects.robotsim.model.Machine;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;
import fr.tp.slr201.projects.robotsim.service.simulation.persistence.PersistenceClient;
import fr.tp.slr201.projects.robotsim.service.simulation.dto.ComponentDTO;
import fr.tp.slr201.projects.robotsim.service.simulation.dto.FactoryDTO;
import fr.tp.slr201.projects.robotsim.service.simulation.dto.PositionDTO;
import fr.tp.slr201.projects.robotsim.service.simulation.kafka.KafkaFactoryModelChangeNotifier;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SimulationService {

    private static final Logger LOG = LoggerFactory.getLogger(SimulationService.class);

    private final Map<String, SimulationTask> running = new ConcurrentHashMap<>();

    private final PersistenceClient persistenceClient;

    private final boolean fallbackOnMissing;

    private final KafkaTemplate<String, Factory> kafkaTemplate;
    private final AdminClient adminClient;

    public SimulationService(@Value("${persistence.host:localhost}") final String host,
                             @Value("${persistence.port:1957}") final int port,
                             @Value("${simulation.fallbackOnMissing:true}") final boolean fallbackOnMissing,
                             final KafkaTemplate<String, Factory> kafkaTemplate,
                             final AdminClient adminClient) {
        this.persistenceClient = new PersistenceClient(host, port);
        this.fallbackOnMissing = fallbackOnMissing;
        this.kafkaTemplate = kafkaTemplate;
        this.adminClient = adminClient;
    }

    /**
     * Start simulation for the factory with the given id.
     * Returns true if started successfully.
     */
    public boolean startSimulation(final String id) {
        LOG.info("Request to start simulation for id='{}'", id);

        if (id == null || id.trim().isEmpty()) {
            LOG.warn("Start request with empty id");
            return false;
        }

        if (running.containsKey(id)) {
            LOG.warn("Simulation for id='{}' is already running", id);
            return false;
        }

        Factory factory = persistenceClient.readFactory(id);
        if (factory == null) {
            if (fallbackOnMissing) {
                LOG.warn("No factory model returned for id='{}' – building fallback in-memory factory", id);
                factory = buildFallbackFactory(id);
            } else {
                LOG.warn("No factory model returned for id='{}' and fallback disabled", id);
                return false;
            }
        }

        factory.setId(id);
        // Set Kafka notifier so every model change publishes an event
        try {
            factory.setNotifier(new KafkaFactoryModelChangeNotifier(factory, kafkaTemplate, adminClient));
        } catch (final Exception e) {
            LOG.warn("Unable to configure Kafka notifier for id='{}': {}", id, e.getMessage());
        }

        final SimulationTask task = new SimulationTask(factory);
        running.put(id, task);
        task.start();

        LOG.info("Simulation started for id='{}'", id);
        return true;
    }

    /**
     * Build a minimal in-memory Factory when persistence is unavailable so the
     * simulation endpoints can still be exercised.
     */
    private Factory buildFallbackFactory(final String id) {
        final Factory f = new Factory(120, 80, id == null ? "fallback-factory" : id);
        // Populate with a few components so polling shows motion.
        // Assign stable ids for easier client filtering.
        Puck puck = new Puck(f, new CircularShape(10, 10, 3), "p1");
        puck.setId("puck-1");
        ChargingStation cs = new ChargingStation(f, new RectangularShape(20, 20, 6, 6), "cs1");
        cs.setId("charging-1");
        Conveyor conv = new Conveyor(f, new RectangularShape(40, 15, 14, 3), "conv1");
        conv.setId("conveyor-1");
        final Room room = new Room(f, new RectangularShape(70, 30, 30, 25), "room1");
        room.setId("room-1");
        final Area area = new Area(room, new RectangularShape(75, 35, 10, 10), "area1");
        area.setId("area-1");
        Machine machine = new Machine(area, new RectangularShape(78, 38, 6, 6), "machine1");
        machine.setId("machine-1");
        return f;
    }

    /** List ids of running simulations. */
    public List<String> listRunningIds() {
        return new ArrayList<>(running.keySet());
    }

    /**
     * Return the live factory model for the given id, or null if not running.
     */
    public Factory getFactory(final String id) {
        final SimulationTask task = running.get(id);
        if (task == null) {
            return null;
        }
        return task.getFactory();
    }

    /**
     * Return a JSON-safe DTO of the running Factory, or null if not running.
     */
    public FactoryDTO getFactoryDTO(final String id) {
        final SimulationTask task = running.get(id);
        if (task == null) {
            return null;
        }

        final Factory factory = task.getFactory();
        final FactoryDTO dto = new FactoryDTO();
        dto.setId(factory.getId());
        dto.setWidth(factory.getWidth());
        dto.setHeight(factory.getHeight());

        final List<ComponentDTO> comps = new ArrayList<>();
        for (final fr.tp.inf112.projects.robotsim.model.Component c : factory.getComponents()) {
            final ComponentDTO cd = new ComponentDTO();
            cd.setId(c.getId());
            cd.setName(c.getName());
            cd.setType(c.getClass().getSimpleName());
            cd.setWidth(c.getWidth());
            cd.setHeight(c.getHeight());
            if (c.getPosition() != null) {
                cd.setPosition(new PositionDTO(c.getPosition().getxCoordinate(), c.getPosition().getyCoordinate()));
            }
            comps.add(cd);
        }

        dto.setComponents(comps);
        return dto;
    }

    /**
     * Stop the simulation with the given id. Returns true if it was running.
     */
    public boolean stopSimulation(final String id) {
        LOG.info("Request to stop simulation for id='{}'", id);
        final SimulationTask task = running.remove(id);
        if (task == null) {
            LOG.warn("No running simulation found for id='{}'", id);
            return false;
        }
        task.stop();
        LOG.info("Simulation stopped for id='{}'", id);
        return true;
    }
}
