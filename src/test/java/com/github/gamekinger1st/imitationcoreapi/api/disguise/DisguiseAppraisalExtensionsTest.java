package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisguiseAppraisalExtensionsTest {
    @Test
    void roundTripsBoundedCopiedAppraisalData() {
        DisguiseAppraisalSnapshot snapshot = new DisguiseAppraisalSnapshot(
                18F,
                20F,
                4,
                Optional.of(new TensuraVitals(100D, 80D, 60D, 16D))
        );
        SnapshotExtension extension = DisguiseAppraisalExtensions.create(snapshot);

        assertEquals(Optional.of(snapshot), DisguiseAppraisalExtensions.find(List.of(extension)));
    }
}
