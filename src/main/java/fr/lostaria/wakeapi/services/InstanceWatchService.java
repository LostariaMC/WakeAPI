package fr.lostaria.wakeapi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class InstanceWatchService {

    private static final Logger log = LoggerFactory.getLogger(InstanceWatchService.class);

    private final TaskScheduler scheduler;
    private final MinecraftService minecraftService;
    private final OvhApiService ovhApiService;

    private static final Duration INITIAL_DELAY = Duration.ofHours(1);
    private static final Duration RECHECK_WHEN_PLAYERS = Duration.ofMinutes(30);
    private static final Duration RECHECK_ON_ERROR = Duration.ofMinutes(5);

    private final AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

    public InstanceWatchService(TaskScheduler scheduler, MinecraftService minecraftService, OvhApiService ovhApiService) {
        this.scheduler = scheduler;
        this.minecraftService = minecraftService;
        this.ovhApiService = ovhApiService;
    }

    public void startWatchAfterOneHour() {
        cancel();
        schedule(this::checkAndMaybeShelve, INITIAL_DELAY);
        log.info("InstanceWatch: démarrage de la surveillance dans {}", INITIAL_DELAY);
    }

    public void cancel() {
        ScheduledFuture<?> f = futureRef.getAndSet(null);
        if (f != null) f.cancel(false);
    }

    private void schedule(Runnable task, Duration delay) {
        ScheduledFuture<?> f = scheduler.schedule(task, Instant.now().plus(delay));
        futureRef.set(f);
    }

    private void checkAndMaybeShelve() {
        try {
            boolean instanceOnline = ovhApiService.getInstanceStatus().isRunning() || ovhApiService.getInstanceStatus().isStarting();
            if (!instanceOnline) {
                cancel();
                return;
            }

            boolean minecraftOnline = minecraftService.isServerOnline();
            if (!minecraftOnline) {
                log.info("InstanceWatch: Minecraft OFFLINE — shelve de l’instance");
                ovhApiService.shelveInstance();
                cancel();
                return;
            }

            int players = minecraftService.getOnlinePlayersCount();
            log.info("InstanceWatch: Minecraft ONLINE, joueurs connectés = {}", players);

            if (players <= 0) {
                log.info("InstanceWatch: 0 joueur — shelve de l’instance");
                ovhApiService.shelveInstance();
                cancel();
            } else {
                log.info("InstanceWatch: {} joueur(s) — re-check dans {}", players, RECHECK_WHEN_PLAYERS);
                schedule(this::checkAndMaybeShelve, RECHECK_WHEN_PLAYERS);
            }
        } catch (Exception e) {
            log.warn("InstanceWatch: erreur pendant le check — {}", e.getMessage(), e);
            schedule(this::checkAndMaybeShelve, RECHECK_ON_ERROR);
        }
    }
}
