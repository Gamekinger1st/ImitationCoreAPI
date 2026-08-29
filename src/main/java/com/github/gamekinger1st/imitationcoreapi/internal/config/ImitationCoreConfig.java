package com.github.gamekinger1st.imitationcoreapi.internal.config;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;

public final class ImitationCoreConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    private static final ModConfigSpec.ConfigValue<String> DEFAULT_CHAT_CHANNEL;
    private static final ModConfigSpec.DoubleValue LOCAL_CHAT_RANGE;
    private static final ModConfigSpec.IntValue CHAT_RATE_LIMIT;
    private static final ModConfigSpec.IntValue CHAT_RATE_WINDOW_SECONDS;
    private static final ModConfigSpec.BooleanValue LOG_CHAT_CONTENT;
    private static final ModConfigSpec.BooleanValue BYPASS_CHAT_RESTRICTIONS;
    private static final ModConfigSpec.BooleanValue MOB_TARGETING_ENABLED;
    private static final ModConfigSpec.IntValue MOB_TARGETING_RECONCILIATION_INTERVAL;
    private static final ModConfigSpec.DoubleValue MOB_TARGETING_RECONCILIATION_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("chat");
        DEFAULT_CHAT_CHANNEL = builder.define("default_channel", "global");
        LOCAL_CHAT_RANGE = builder.defineInRange("local_range", 128D, 1D, 4096D);
        CHAT_RATE_LIMIT = builder.defineInRange("rate_limit_messages", 6, 1, 1000);
        CHAT_RATE_WINDOW_SECONDS = builder.defineInRange("rate_limit_window_seconds", 10, 1, 3600);
        LOG_CHAT_CONTENT = builder.define("log_message_content", false);
        builder.pop();
        builder.push("mob_targeting");
        MOB_TARGETING_ENABLED = builder.define("enabled", true);
        MOB_TARGETING_RECONCILIATION_INTERVAL = builder.defineInRange("reconciliation_interval_ticks", 10, 1, 200);
        MOB_TARGETING_RECONCILIATION_RANGE = builder.defineInRange("reconciliation_range", 32D, 4D, 256D);
        builder.pop();
        SPEC = builder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.push("chat");
        BYPASS_CHAT_RESTRICTIONS = clientBuilder.define("bypass_account_chat_restrictions", false);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }

    private ImitationCoreConfig() {
    }

    public static ResourceLocation defaultChatChannel() {
        String configured = value(SPEC, DEFAULT_CHAT_CHANNEL).strip().toLowerCase(Locale.ROOT);
        return switch (configured) {
            case "local" -> ChatChannels.LOCAL;
            case "global" -> ChatChannels.GLOBAL;
            default -> ChatChannels.GLOBAL;
        };
    }

    public static boolean mobTargetingEnabled() {
        return value(SPEC, MOB_TARGETING_ENABLED);
    }

    public static double localChatRange() {
        return value(SPEC, LOCAL_CHAT_RANGE);
    }

    public static int chatRateLimit() {
        return value(SPEC, CHAT_RATE_LIMIT);
    }

    public static long chatRateWindowMillis() {
        return value(SPEC, CHAT_RATE_WINDOW_SECONDS) * 1000L;
    }

    public static boolean logChatContent() {
        return value(SPEC, LOG_CHAT_CONTENT);
    }

    public static boolean bypassChatRestrictions() {
        return value(CLIENT_SPEC, BYPASS_CHAT_RESTRICTIONS);
    }

    public static int mobTargetingReconciliationInterval() {
        return value(SPEC, MOB_TARGETING_RECONCILIATION_INTERVAL);
    }

    public static double mobTargetingReconciliationRange() {
        return value(SPEC, MOB_TARGETING_RECONCILIATION_RANGE);
    }

    private static <T> T value(ModConfigSpec spec, ModConfigSpec.ConfigValue<T> configured) {
        return spec.isLoaded() ? configured.get() : configured.getDefault();
    }
}
