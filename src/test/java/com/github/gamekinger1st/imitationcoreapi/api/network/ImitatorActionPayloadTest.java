package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImitatorActionPayloadTest {
    @Test
    void boundsClientControlledFormSlotValues() {
        assertDoesNotThrow(() -> new CommitImitatorRecordPayload(255));
        assertDoesNotThrow(() -> new SelectImitatorFormPayload(255));
        assertThrows(IllegalArgumentException.class, () -> new CommitImitatorRecordPayload(256));
        assertThrows(IllegalArgumentException.class, () -> new SelectImitatorFormPayload(-1));
    }

    @Test
    void boundsActionFeedback() {
        assertDoesNotThrow(() -> new ImitatorActionFeedbackPayload(ImitatorAction.RECORD, true, "Recorded"));
        assertThrows(IllegalArgumentException.class, () -> new ImitatorActionFeedbackPayload(ImitatorAction.RECORD, false, "x".repeat(ImitatorActionFeedbackPayload.MAX_MESSAGE_LENGTH + 1)));
    }
}
