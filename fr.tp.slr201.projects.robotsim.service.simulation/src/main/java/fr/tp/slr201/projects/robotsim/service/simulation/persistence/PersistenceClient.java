package fr.tp.slr201.projects.robotsim.service.simulation.persistence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.tp.inf112.projects.robotsim.model.Factory;

/**
 * Small client to talk to the existing socket-based persistence server.
 */
public class PersistenceClient {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceClient.class);

    private final String host;
    private final int port;
    private final int timeoutMs = 3000;

    public PersistenceClient(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Read a Factory model by id from the persistence server.
     * Returns null if not found or on error.
     */
    public Factory readFactory(final String id) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                final String safeId = id == null ? null : new java.io.File(id).getName();
                LOG.debug("Requesting factory id='{}' from persistence server {}:{}", safeId, host, port);

                oos.flush();
                oos.writeObject(safeId);
                oos.flush();

                final Object resp = ois.readObject();
                if (resp instanceof Factory) {
                    LOG.info("Received factory id='{}' from persistence server", safeId);
                    return (Factory) resp;
                }

                LOG.warn("Persistence server returned unexpected response type: {}", resp == null ? "null" : resp.getClass());
                return null;
            }
            catch (final ClassNotFoundException ex) {
                LOG.error("Error deserializing response from persistence server", ex);
                return null;
            }
        } catch (final IOException e) {
            LOG.error("IO error talking to persistence server {}:{}", host, port, e);
            return null;
        }
    }
}
