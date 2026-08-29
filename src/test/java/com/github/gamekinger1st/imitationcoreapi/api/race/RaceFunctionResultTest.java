package com.github.gamekinger1st.imitationcoreapi.api.race;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceFunctionResultTest {
    @Test
    void retainsOptionalModReturnTypesWithoutPretendingTheyAreCoreData() {
        Map<String, Integer> value = Map.of("requirement", 10);
        RaceFunctionResult result = RaceFunctionResult.rawValue(value);

        assertTrue(result.handled());
        assertEquals(value, result.rawValue().orElseThrow());
    }
}
