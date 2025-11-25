package fr.tp.inf112.projects.robotsim;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import fr.tp.inf112.projects.robotsim.model.*;
import fr.tp.inf112.projects.robotsim.model.shapes.*;

/**
 * Lightweight verification without JUnit. Run as a Java Application.
 * Verifies Jackson JSON roundtrip and basic content heuristics.
 */
public class TestRobotSimSerializationJSON {

    private final ObjectMapper objectMapper;

    public TestRobotSimSerializationJSON() {
        final BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("fr.tp.inf112.projects.robotsim")
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }

    private Factory buildSampleFactory() {
        final Factory f = new Factory(120, 80, "sample-factory");
        new Puck(f, new CircularShape(10, 10, 3), "p1");
        new ChargingStation(f, new RectangularShape(20, 20, 4, 4), "cs1");
        new Conveyor(f, new RectangularShape(40, 15, 10, 2), "conv1");
        final Room room = new Room(f, new RectangularShape(60, 30, 30, 25), "room1");
        final Area area = new Area(room, new RectangularShape(65, 35, 10, 10), "area1");
        new Machine(area, new RectangularShape(67, 37, 6, 6), "machine1");
        return f;
    }

    private void roundTripSerialization() throws Exception {
        final Factory original = buildSampleFactory();
        final String json = objectMapper.writeValueAsString(original);
        if (json == null || json.isEmpty()) {
            throw new IllegalStateException("JSON is null/empty");
        }
        final Factory roundTrip = objectMapper.readValue(json, Factory.class);
        if (roundTrip == null) {
            throw new IllegalStateException("Roundtrip factory is null");
        }
        if (original.getComponents().size() != roundTrip.getComponents().size()) {
            throw new IllegalStateException("Component count mismatch after roundtrip");
        }
    }

    private void derivedIgnoredCoordinates() throws Exception {
        final Factory original = buildSampleFactory();
        final String json = objectMapper.writeValueAsString(original);
        int occurrences = 0; int idx = -1; final String needle = "\"xCoordinate\"";
        while ((idx = json.indexOf(needle, idx + 1)) != -1) { occurrences++; }
        if (occurrences <= 0) {
            throw new IllegalStateException("No xCoordinate occurrences found in JSON");
        }
    }

    public static void main(String[] args) throws Exception {
        final TestRobotSimSerializationJSON t = new TestRobotSimSerializationJSON();
        t.roundTripSerialization();
        t.derivedIgnoredCoordinates();
        System.out.println("[Manual Runner] Serialization checks passed.");
    }
}
