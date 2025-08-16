package fr.lostaria.wakeapi.services;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class SshService {

    @Value("${minecraft.host}")
    private String host;

    @Value("${ssh.port:22}")
    private int port;

    @Value("${ssh.username}")
    private String username;

    @Value("${ssh.privateKeyPath}")
    private String privateKeyPath;

    private static final int COMMAND_TIMEOUT_MILLIS = 30_000;

    public void execOrThrow(String command) throws IOException {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());

            ssh.connect(host, port);

            KeyProvider kp = ssh.loadKeys(privateKeyPath);
            ssh.authPublickey(username, kp);

            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec(command);
                cmd.join(COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                Integer exit = cmd.getExitStatus();
                String stderr = new String(cmd.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

                if (exit == null) throw new IOException("SSH command timed out");
                if (exit != 0) throw new IOException("Remote exit=" + exit + " stderr=" + stderr);
            }
        } catch (IOException e) {
            throw new IOException("SSH exec failed on " + host + ": " + e.getMessage(), e);
        }
    }
}
