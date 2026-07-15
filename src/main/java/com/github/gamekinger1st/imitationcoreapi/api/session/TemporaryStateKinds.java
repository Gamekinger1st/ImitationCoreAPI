package com.github.gamekinger1st.imitationcoreapi.api.session;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.minecraft.resources.ResourceLocation;

public final class TemporaryStateKinds {
    public static final ResourceLocation BORROWED_SKILL = id("borrowed_skill");
    public static final ResourceLocation SKILL_COPY_POLICY = id("skill_copy_policy");
    public static final ResourceLocation OWNER_SKILL_SUPPRESSION = id("owner_skill_suppression");
    public static final ResourceLocation FORM_PROGRESSION = id("form_progression");
    public static final ResourceLocation ITEM = id("temporary_item");
    public static final ResourceLocation STAT = id("temporary_stat");
    public static final ResourceLocation PHYSICAL_FORM = id("physical_form");
    public static final ResourceLocation EFFECT = id("temporary_effect");
    public static final ResourceLocation INVENTORY = id("temporary_inventory");
    public static final ResourceLocation BLOCK = id("temporary_block");
    public static final ResourceLocation PROFILE = id("temporary_profile");
    public static final ResourceLocation TEAM = id("temporary_team");
    public static final ResourceLocation REPLICA_ENTITY = id("replica_entity");
    public static final ResourceLocation APPLICATION_MARKER = id("application_marker");
    public static final ResourceLocation UNASSIGNED_HANDLER = id("unassigned_handler");

    private TemporaryStateKinds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, path);
    }
}
