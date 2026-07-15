package com.github.gamekinger1st.imitationcoreapi.api.diagnostic;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitationDiagnosticBusTest {
    @Test
    void postsDiagnosticsToRegisteredListeners() {
        ImitationDiagnosticBus bus = new ImitationDiagnosticBus();
        List<ImitationDiagnostic> seen = new ArrayList<>();
        ImitationDiagnosticRegistration registration = bus.register(seen::add);
        ImitationDiagnostic diagnostic = new ImitationDiagnostic(
                ImitationDiagnosticCategory.INVALID_SNAPSHOT,
                ImitationDiagnosticSeverity.WARNING,
                "bad snapshot",
                Optional.of(UUID.randomUUID()),
                Optional.empty(),
                Optional.of(UUID.randomUUID()),
                Optional.of(ResourceLocation.withDefaultNamespace("zombie")),
                12L
        );

        bus.post(diagnostic);
        assertEquals(List.of(diagnostic), seen);

        assertTrue(registration.unregister());
        bus.post(diagnostic);
        assertEquals(1, seen.size());
    }

    @Test
    void boundsMessagesAndRejectsNegativeTime() {
        String longMessage = "x".repeat(600);
        ImitationDiagnostic diagnostic = new ImitationDiagnostic(
                ImitationDiagnosticCategory.ACTION_REJECTED,
                ImitationDiagnosticSeverity.WARNING,
                longMessage,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                1L
        );

        assertEquals(512, diagnostic.message().length());
        assertThrows(IllegalArgumentException.class, () -> new ImitationDiagnostic(
                ImitationDiagnosticCategory.ACTION_REJECTED,
                ImitationDiagnosticSeverity.WARNING,
                "bad",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                -1L
        ));
    }

    @Test
    void classifiesCommonRejectionMessages() {
        assertEquals(ImitationDiagnosticCategory.NO_SELECTED_FORM, ImitationDiagnostics.classify("No form slot is selected"));
        assertEquals(ImitationDiagnosticCategory.MISSING_TENSURA_BRIDGE, ImitationDiagnostics.classify("Perfect Form requires compatible Tensura state for both forms"));
        assertEquals(ImitationDiagnosticCategory.REPLICA_UNSUPPORTED, ImitationDiagnostics.classify("The selected form cannot produce a living replica"));
        assertEquals(ImitationDiagnosticCategory.CLIENT_PROTOCOL_MISMATCH, ImitationDiagnostics.classify("The Imitation Core chat protocol has not been negotiated"));
    }
}
