package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import com.github.gamekinger1st.imitationcoreapi.api.adapter.ImitationAdapter;

public interface SnapshotCaptureAdapter extends ImitationAdapter {
    void capture(SnapshotCaptureContext context, IdentitySnapshot.Builder builder);
}
