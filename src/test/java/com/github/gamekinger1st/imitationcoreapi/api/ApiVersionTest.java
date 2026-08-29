package com.github.gamekinger1st.imitationcoreapi.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiVersionTest {
    @Test
    void currentApiVersionMatchesThePublishedModApi() {
        assertEquals(new ApiVersion(0, 5, 0), ApiVersion.CURRENT);
    }

    @Test
    void preOneZeroCompatibilityRequiresTheSameMinorVersion() {
        assertTrue(new ApiVersion(0, 1, 0).isBinaryCompatibleWith(new ApiVersion(0, 1, 5)));
        assertFalse(new ApiVersion(0, 1, 0).isBinaryCompatibleWith(new ApiVersion(0, 2, 0)));
    }

    @Test
    void stableCompatibilityRequiresTheSameMajorVersion() {
        assertTrue(new ApiVersion(1, 0, 0).isBinaryCompatibleWith(new ApiVersion(1, 4, 2)));
        assertFalse(new ApiVersion(1, 0, 0).isBinaryCompatibleWith(new ApiVersion(2, 0, 0)));
    }
}
