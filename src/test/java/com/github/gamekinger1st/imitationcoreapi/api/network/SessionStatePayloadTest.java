package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionStatePayloadTest {
    @Test
    void boundsAddonCompatibilityReasonsBeforeNetworkSync() {
        SessionStatePayload payload = new SessionStatePayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                TransformationScope.GAMEPLAY,
                TransformationState.ACTIVE,
                CompatibilityLevel.FALLBACK,
                1L,
                IntStream.range(0, 20).mapToObj(index -> "reason " + index).toList()
        );

        assertEquals(8, payload.reasons().size());
    }
}
