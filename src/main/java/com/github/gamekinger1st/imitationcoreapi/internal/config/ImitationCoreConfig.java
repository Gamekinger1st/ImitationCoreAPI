package com.github.gamekinger1st.imitationcoreapi.internal.config;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;

public final class ImitationCoreConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<String> DEFAULT_CHAT_CHANNEL;
    private static final ModConfigSpec.BooleanValue MOB_TARGETING_ENABLED;
    private static final ModConfigSpec.IntValue MOB_TARGETING_RECONCILIATION_INTERVAL;
    private static final ModConfigSpec.DoubleValue MOB_TARGETING_RECONCILIATION_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("chat");
        DEFAULT_CHAT_CHANNEL = builder.define("default_channel", "global");
        builder.pop();
        builder.push("mob_targeting");
        MOB_TARGETING_ENABLED = builder.define("enabled", true);
        MOB_TARGETING_RECONCILIATION_INTERVAL = builder.defineInRange("reconciliation_interval_ticks", 10, 1, 200);
        MOB_TARGETING_RECONCILIATION_RANGE = builder.defineInRange("reconciliation_range", 32D, 4D, 256D);
        builder.pop();
        SPEC = builder.build();
    }

    private ImitationCoreConfig() {
    }

    public static ResourceLocation defaultChatChannel() {
        String configured = DEFAULT_CHAT_CHANNEL.get().strip().toLowerCase(Locale.ROOT);
        return switch (configured) {
            case "local" -> ChatChannels.LOCAL;
            case "global" -> ChatChannels.GLOBAL;
            default -> ChatChannels.GLOBAL;
        };
    }

    public static boolean mobTargetingEnabled() {
        return MOB_TARGETING_ENABLED.get();
    }

    public static int mobTargetingReconciliationInterval() {
        return MOB_TARGETING_RECONCILIATION_INTERVAL.get();
    }

    public static double mobTargetingReconciliationRange() {
        return MOB_TARGETING_RECONCILIATION_RANGE.get();
    }
}
