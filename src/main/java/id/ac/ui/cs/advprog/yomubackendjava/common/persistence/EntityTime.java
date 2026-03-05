package id.ac.ui.cs.advprog.yomubackendjava.common.persistence;

import java.time.Instant;

public final class EntityTime {
    private EntityTime() {
    }

    public static Instant now() {
        return Instant.now();
    }
}
