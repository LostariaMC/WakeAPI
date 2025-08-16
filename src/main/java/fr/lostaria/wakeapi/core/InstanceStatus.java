package fr.lostaria.wakeapi.core;

import java.util.EnumSet;
import java.util.Locale;

public enum InstanceStatus {
    ACTIVE,
    SHELVED_OFFLOADED,
    UNSHELVING,
    SHELVING,
    UNKNOWN;

    private static final EnumSet<InstanceStatus> RUNNING = EnumSet.of(ACTIVE);
    private static final EnumSet<InstanceStatus> STOPPED = EnumSet.of(SHELVED_OFFLOADED);
    private static final EnumSet<InstanceStatus> STARTING = EnumSet.of(UNSHELVING);
    private static final EnumSet<InstanceStatus> STOPPING = EnumSet.of(SHELVING);

    public static InstanceStatus from(String value) {
        if (value == null) return UNKNOWN;
        try {
            return InstanceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }

    public boolean isRunning() { return RUNNING.contains(this); }
    public boolean isStopped() { return STOPPED.contains(this); }
    public boolean isStarting() { return STARTING.contains(this); }
    public boolean isStopping() { return STOPPING.contains(this); }
}
