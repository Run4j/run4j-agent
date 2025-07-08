package uz.maniac4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@ApplicationScoped
public class InstanceService {
    @Inject
    DataSource dataSource;

    private static final Logger LOG = Logger.getLogger(InstanceService.class);

    public Object startInstance(StartInstanceRequest request) throws Exception {
        // 1. Provision DB
        String dbName = "inst_" + request.instanceId;
        String dbUser = "user_" + request.instanceId;
        String dbPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        provisionDatabase(dbName, dbUser, dbPassword);

        // 2. Build Podman command
        String containerName = "run4j-" + request.instanceId;
        String podmanCmd = String.format(
                "podman run --rm --name %s --memory=%s --cpus=%s -v %s:/data " +
                        "-e DB_URL=jdbc:postgresql://localhost:5432/%s -e DB_USER=%s -e DB_PASSWORD=%s " +
                        "-d openjdk:21-jdk java -jar %s",
                containerName,
                request.memoryLimit,
                request.cpuLimit,
                request.userVolume,
                dbName,
                dbUser,
                dbPassword,
                request.jarPath
        );
        LOG.infof("Starting container with command: %s", podmanCmd);
        executeCommand(podmanCmd);
        return new StartInstanceResponse(containerName, dbName, dbUser, dbPassword);
    }

    public Object stopInstance(StopInstanceRequest request) throws Exception {
        String containerName = "run4j-" + request.instanceId;
        String podmanCmd = String.format("podman rm -f %s", containerName);
        LOG.infof("Stopping/removing container: %s", containerName);
        executeCommand(podmanCmd);
        // Optionally: drop DB/user here
        return new SimpleResponse("Stopped and removed: " + containerName);
    }

    public Object getStatus(String id) throws Exception {
        String containerName = "run4j-" + id;
        String podmanCmd = String.format("podman inspect -f '{{.State.Status}}' %s", containerName);
        String status = executeCommandWithOutput(podmanCmd);
        return new StatusResponse(containerName, status.trim());
    }

    private void provisionDatabase(String dbName, String dbUser, String dbPassword) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE " + dbName);
            stmt.executeUpdate(String.format("CREATE USER %s WITH PASSWORD '%s'", dbUser, dbPassword));
            stmt.executeUpdate(String.format("GRANT ALL PRIVILEGES ON DATABASE %s TO %s", dbName, dbUser));
            LOG.infof("Provisioned DB: %s, user: %s", dbName, dbUser);
        }
    }

    private void executeCommand(String command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command);
        }
    }

    private String executeCommandWithOutput(String command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command + "\nOutput: " + output);
        }
        return output;
    }

    // --- Response DTOs ---
    public static class StartInstanceResponse {
        public String containerName;
        public String dbName;
        public String dbUser;
        public String dbPassword;
        public StartInstanceResponse(String containerName, String dbName, String dbUser, String dbPassword) {
            this.containerName = containerName;
            this.dbName = dbName;
            this.dbUser = dbUser;
            this.dbPassword = dbPassword;
        }
    }
    public static class SimpleResponse {
        public String message;
        public SimpleResponse(String message) { this.message = message; }
    }
    public static class StatusResponse {
        public String containerName;
        public String status;
        public StatusResponse(String containerName, String status) {
            this.containerName = containerName;
            this.status = status;
        }
    }
} 