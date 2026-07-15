package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.OptionalLong;

public record ImitatorTransformDurationPolicy(int durationMinutes) {
    public static final int TICKS_PER_MINUTE = 60 * 20;
    public static final ImitatorTransformDurationPolicy UNLIMITED = new ImitatorTransformDurationPolicy(0);

    public ImitatorTransformDurationPolicy {
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("Transform duration minutes cannot be negative");
        }
    }

    public static ImitatorTransformDurationPolicy unlimited() {
        return UNLIMITED;
    }

    public static ImitatorTransformDurationPolicy minutes(int durationMinutes) {
        return durationMinutes == 0 ? UNLIMITED : new ImitatorTransformDurationPolicy(durationMinutes);
    }

    public boolean unlimitedDuration() {
        return durationMinutes == 0;
    }

    public long durationTicks() {
        return (long) durationMinutes * TICKS_PER_MINUTE;
    }

    public OptionalLong expiresFrom(long gameTime) {
        if (gameTime < 0L) {
            throw new IllegalArgumentException("Game time cannot be negative");
        }
        if (unlimitedDuration()) {
            return OptionalLong.empty();
        }
        long durationTicks = durationTicks();
        if (Long.MAX_VALUE - gameTime < durationTicks) {
            throw new IllegalArgumentException("Transform duration deadline exceeds supported game time");
        }
        return OptionalLong.of(gameTime + durationTicks);
    }
}
