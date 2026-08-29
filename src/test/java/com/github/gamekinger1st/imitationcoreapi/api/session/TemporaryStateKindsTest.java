package com.github.gamekinger1st.imitationcoreapi.api.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TemporaryStateKindsTest {
    @Test
    void doesNotExposeRealCopiedItemWorkflowKinds() {
        assertThrows(NoSuchFieldException.class, () -> TemporaryStateKinds.class.getField("ITEM"));
        assertThrows(NoSuchFieldException.class, () -> TemporaryStateKinds.class.getField("INVENTORY"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryItemMarker"));
    }
}
