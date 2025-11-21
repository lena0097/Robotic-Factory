package fr.tp.inf112.projects.robotsim.persistence;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple persistence server that accepts serialized requests from clients
 * and stores/loads Canvas objects in the server storage directory.
 */
public class PersistenceServer {

    private final int port;
    private final File storageDir;

    public PersistenceServer(final int port, final File storageDir) {
        this.port = port;
        this.storageDir = storageDir;
        if (!this.storageDir.exists()) {
            this.storageDir.mkdirs();
        }
    }

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("PersistenceServer listening on port " + port + " storing in " + storageDir.getAbsolutePath());
            while (true) {
                final Socket socket = server.accept();
                final Thread t = new Thread(new RequestProcessor(socket, storageDir));
                t.setDaemon(true);
                t.start();
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        final int port = 1957;
        final File storage = new File("server_storage");
        final PersistenceServer srv = new PersistenceServer(port, storage);
        srv.start();
    }
}
