package im.kagami.common.time;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Single source of time. Every timestamp must come from here instead of a scattered
 * {@code OffsetDateTime.now()}, which keeps them swappable for a fixed stub in tests.
 */
@Component
public class Clock {

    public OffsetDateTime now() {
        return OffsetDateTime.now();
    }
}
