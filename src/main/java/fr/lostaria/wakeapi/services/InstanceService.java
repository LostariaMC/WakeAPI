package fr.lostaria.wakeapi.services;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class InstanceService {

    private SshService sshService;

    public InstanceService(SshService sshService) {
        this.sshService = sshService;
    }

    public void clearBuildServerData() throws IOException {
        sshService.execOrThrow("rm -rf /srv/MinecraftServer/dev/special/Construction/*");
    }

    public void endProxy() throws IOException {
        sshService.execOrThrow("sudo systemctl stop mcproxy");
    }
}
