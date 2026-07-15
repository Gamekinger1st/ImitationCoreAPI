package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenImitatorMenuPayloadTest {
    @Test
    void carriesAConcreteMenuRequest() {
        assertEquals(ImitatorMenuRequest.SELECT_TRANSFORM_FORM, new OpenImitatorMenuPayload(ImitatorMenuRequest.SELECT_TRANSFORM_FORM).request());
    }

    @Test
    void rejectsAnEmptyMenuRequest() {
        assertThrows(IllegalArgumentException.class, () -> new OpenImitatorMenuPayload(ImitatorMenuRequest.NONE));
    }
}
