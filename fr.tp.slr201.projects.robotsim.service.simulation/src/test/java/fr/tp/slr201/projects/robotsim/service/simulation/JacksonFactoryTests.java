package fr.tp.slr201.projects.robotsim.service.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.Puck;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;

import fr.tp.inf112.projects.robotsim.model.Conveyor;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;
import fr.tp.inf112.projects.robotsim.model.ChargingStation;

import fr.tp.inf112.projects.robotsim.remote.RemoteSimulatorController;

public class JacksonFactoryTests {

    private static ObjectMapper mapper;

    @BeforeAll
    public static void setupMapper() {
        mapper = new ObjectMapper();
        final BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("fr.tp.inf112.projects.robotsim")
                .allowIfSubType("fr.tp.slr201.projects.robotsim.service.simulation")
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }

    private Factory createSampleFactory() {
        final Factory f = new Factory(80, 60, "test-factory");
        new Puck(f, new CircularShape(10, 10, 2), "p1");
        new Conveyor(f, new RectangularShape(20, 20, 4, 2), "c1");
        new ChargingStation(f, new RectangularShape(30, 30, 2, 2), "cs1");
        return f;
    }

    @Test
    public void roundtripFactorySerialization() throws Exception {
        final Factory f = createSampleFactory();
        final String json = mapper.writeValueAsString(f);
        assertNotNull(json);

        final Factory des = mapper.readValue(json, Factory.class);
        assertNotNull(des);
        assertEquals(f.getComponents().size(), des.getComponents().size());
    }

    private static HttpServer httpServer;

    @BeforeAll
    public static void startServer() throws Exception {
        // no-op here; server created per test when needed
    }

    @AfterAll
    public static void stopServer() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    public void remoteControllerFetchesFactory() throws Exception {
        final Factory f = createSampleFactory();
        final String payload = mapper.writeValueAsString(f);

        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/simulation/test-factory", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                final byte[] bytes = payload.getBytes("UTF-8");
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();

        final int port = httpServer.getAddress().getPort();
        final String baseUrl = "http://localhost:" + port;

        final RemoteSimulatorController controller = new RemoteSimulatorController(baseUrl, mapper);
        final Factory fetched = controller.fetchFactory("test-factory");
        assertNotNull(fetched);
        assertEquals(f.getComponents().size(), fetched.getComponents().size());

        httpServer.stop(0);
        httpServer = null;
    }
}
