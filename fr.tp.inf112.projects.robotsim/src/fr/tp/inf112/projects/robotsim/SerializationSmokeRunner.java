package fr.tp.inf112.projects.robotsim;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.Puck;
import fr.tp.inf112.projects.robotsim.model.ChargingStation;
import fr.tp.inf112.projects.robotsim.model.Conveyor;
import fr.tp.inf112.projects.robotsim.model.Room;
import fr.tp.inf112.projects.robotsim.model.Area;
import fr.tp.inf112.projects.robotsim.model.Machine;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

/**
 * Standalone (no JUnit) runner to quickly validate JSON roundtrip serialization in Eclipse
 * without adding JUnit to the simulator project.
 */
public class SerializationSmokeRunner {

    private static ObjectMapper buildMapper() {
    final BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("fr.tp.inf112.projects.robotsim")
        // Allow canvas model implementations (e.g., RGBColor)
        .allowIfSubType("fr.tp.inf112.projects.canvas")
        // Allow common JDK collection implementations (e.g., java.util.ArrayList)
        .allowIfSubType("java.util")
        .build();
        final ObjectMapper mapper = new ObjectMapper();
        // Be tolerant to incremental model evolution: ignore unknown JSON properties globally
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    private static Factory buildSampleFactory() {
        final Factory f = new Factory(120, 80, "sample-factory");
        new Puck(f, new CircularShape(10, 10, 3), "p1");
        new ChargingStation(f, new RectangularShape(20, 20, 4, 4), "cs1");
        new Conveyor(f, new RectangularShape(40, 15, 10, 2), "conv1");
        final Room room = new Room(f, new RectangularShape(60, 30, 30, 25), "room1");
        final Area area = new Area(room, new RectangularShape(65, 35, 10, 10), "area1");
        new Machine(area, new RectangularShape(67, 37, 6, 6), "machine1");
        return f;
    }

    public static void main(String[] args) throws Exception {
        final ObjectMapper mapper = buildMapper();

        final Factory original = buildSampleFactory();
        final String json = mapper.writeValueAsString(original);
        System.out.println("Serialized JSON length: " + json.length());

        final Factory roundTrip = mapper.readValue(json, Factory.class);
        System.out.println("Roundtrip components: " + (roundTrip == null ? -1 : roundTrip.getComponents().size()));

        if (roundTrip == null || roundTrip.getComponents().size() != original.getComponents().size()) {
            throw new IllegalStateException("Roundtrip mismatch");
        }

        // Heuristic: ensure some positions are serialized (should contain xCoordinate from positions)
        int occurrences = 0;
        int idx = -1;
        final String needle = "\"xCoordinate\"";
        while ((idx = json.indexOf(needle, idx + 1)) != -1) {
            occurrences++;
        }
        if (occurrences <= 0) {
            throw new IllegalStateException("No xCoordinate occurrences found in JSON");
        }

        System.out.println("[Smoke] Serialization test passed.");
    }
}
