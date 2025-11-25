package fr.tp.inf112.projects.robotsim.remote;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.tp.inf112.projects.robotsim.model.Factory;

/**
 * Remote simulator controller: start/stop remote simulation and poll the microservice for the
 * live Factory model. Consumers can register a listener to receive Factory updates.
 */
public class RemoteSimulatorController {

    public interface Listener {
        void onFactoryUpdate(Factory factory);
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private Listener listener;

    private Thread pollingThread;
    private final AtomicBoolean polling = new AtomicBoolean(false);

    public RemoteSimulatorController(final String baseUrl, final ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = objectMapper;
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    public boolean startRemoteSimulation(final String id) throws IOException, InterruptedException {
        final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/simulation/start?id=" + id))
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        final HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() / 100 == 2 && "true".equalsIgnoreCase(resp.body().trim());
    }

    public boolean stopRemoteSimulation(final String id) throws IOException, InterruptedException {
        final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/simulation/stop?id=" + id))
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        final HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() / 100 == 2 && "true".equalsIgnoreCase(resp.body().trim());
    }

    public Factory fetchFactory(final String id) throws IOException, InterruptedException {
        final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/simulation/" + id))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        final HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            return null;
        }

        final String body = resp.body();
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        return objectMapper.readValue(body, Factory.class);
    }

    public void startPolling(final String id, final long intervalMs) {
        if (polling.get()) {
            return;
        }

        polling.set(true);
        pollingThread = new Thread(() -> {
            while (polling.get()) {
                try {
                    final Factory f = fetchFactory(id);
                    if (f != null && listener != null) {
                        listener.onFactoryUpdate(f);
                    }
                } catch (final Exception e) {
                    // ignore — listener can handle stale state
                }

                try {
                    Thread.sleep(intervalMs);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "RemoteSimulatorController-poller");

        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    public void stopPolling() {
        polling.set(false);
        if (pollingThread != null) {
            pollingThread.interrupt();
            pollingThread = null;
        }
    }
}
