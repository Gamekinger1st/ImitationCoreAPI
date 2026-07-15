package com.github.gamekinger1st.imitationcoreapi.api.adapter;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.resources.ResourceLocation;

public interface ImitationAdapter {
    ResourceLocation id();

    AdapterKind kind();

    CompatibilityAssessment assess(IdentitySnapshot snapshot);
}
