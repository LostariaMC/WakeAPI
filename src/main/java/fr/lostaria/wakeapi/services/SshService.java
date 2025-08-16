package fr.lostaria.wakeapi.services;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class SshService {

    private static final Logger log = LoggerFactory.getLogger(SshService.class);

    @Value("${minecraft.host}")
    private String host;

    @Value("${ssh.port:22}")
    private int port;

    @Value("${ssh.username}")
    private String username;

    @Value("${ssh.privateKeyPath}")
    private String privateKeyPath;

    private static final int COMMAND_TIMEOUT_MILLIS = 30_000;
    private static final int MAX_LOG_BYTES = 4_096;

    public void execOrThrow(String command) throws IOException {
        execOrThrow(new String[]{ command });
    }

    public void execOrThrow(String... commands) throws IOException {
        log.info("SSH: connecting to {}@{}:{}", username, host, port);
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(host, port);
            log.debug("SSH: TCP connection established");

            KeyProvider kp = ssh.loadKeys(privateKeyPath);
            ssh.authPublickey(username, kp);
            log.info("SSH: authenticated as {} (key loaded)", username);

            int idx = 0;
            for (String command : commands) {
                idx++;
                long t0 = System.nanoTime();
                log.info("SSH[{} / {}]: exec -> {}", idx, commands.length, command);

                try (Session session = ssh.startSession()) {
                    Session.Command cmd = session.exec(command);
                    cmd.join(COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                    Integer exit = cmd.getExitStatus();
                    String stdout = readLimited(cmd.getInputStream().readAllBytes());
                    String stderr = readLimited(cmd.getErrorStream().readAllBytes());
                    long dtMs = (System.nanoTime() - t0) / 1_000_000;

                    if (exit == null) {
                        log.error("SSH[{}]: TIMEOUT after {} ms | stderr: {}", idx, dtMs, stderr);
                        throw new IOException("SSH command timed out: " + command);
                    }

                    if (exit != 0) {
                        log.error("SSH[{}]: exit={} in {} ms | stderr: {}", idx, exit, dtMs, stderr);
                        throw new IOException("Remote exit=" + exit + " for '" + command + "' stderr=" + stderr);
                    }

                    log.info("SSH[{}]: OK ({} ms)", idx, dtMs);
                    if (!stdout.isEmpty()) log.debug("SSH[{}] stdout: {}", idx, stdout);
                    if (!stderr.isEmpty()) log.debug("SSH[{}] stderr: {}", idx, stderr);
                }
            }

            log.info("SSH: all commands executed successfully on {}", host);
        } catch (IOException e) {
            log.error("SSH: failure on {}@{}:{} -> {}", username, host, port, e.getMessage());
            throw new IOException("SSH exec failed on " + host + ": " + e.getMessage(), e);
        }
    }

    private String readLimited(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        int len = Math.min(bytes.length, MAX_LOG_BYTES);
        String s = new String(bytes, 0, len, StandardCharsets.UTF_8).replaceAll("\\s+$", "");
        if (bytes.length > MAX_LOG_BYTES) s += " …(truncated)…";
        return s;
    }
}
