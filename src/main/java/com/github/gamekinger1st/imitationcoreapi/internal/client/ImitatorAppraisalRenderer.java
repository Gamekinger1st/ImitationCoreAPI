package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public final class ImitatorAppraisalRenderer {
    public static final ResourceLocation ENTITY_ANALYSIS = ResourceLocation.fromNamespaceAndPath("tensura", "textures/player_hud/analysis/analysis_overlay.png");

    private ImitatorAppraisalRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font, ClientDisguiseState state, DisguiseAppraisalSnapshot appraisal, LivingEntity target, boolean leftSide) {
        if (graphics == null || font == null || state == null || appraisal == null || target == null) {
            return;
        }
        DisguiseAppraisalSnapshot live = new DisguiseAppraisalSnapshot(
                Math.max(0F, target.getHealth()),
                Math.max(0F, target.getMaxHealth()),
                Math.max(0, target.getArmorValue()),
                ImitationApi.tensuraStates().captureVitals(target).or(() -> appraisal.tensuraVitals())
        );
        drawCentered(graphics, font, state.displayName(), leftSide ? 8 : 9, 12, 78);
        int lineY = 30;
        drawLine(graphics, font, Component.translatable("appraisal.imitationcoreapi.health", Math.round(live.health()), Math.round(live.maxHealth())), lineY);
        live.tensuraVitals().ifPresentOrElse(vitals -> {
            drawLine(graphics, font, Component.translatable("appraisal.imitationcoreapi.spiritual_health", Math.round(vitals.spiritualHealth())), 41);
            drawLine(graphics, font, Component.translatable("appraisal.imitationcoreapi.armor", live.armorValue()), 52);
            drawLine(graphics, font, Component.translatable("appraisal.imitationcoreapi.ep", number(vitals.ep())), 63);
            drawLine(graphics, font, Component.translatable("appraisal.imitationcoreapi.magicule", number(vitals.magicule())), 74);
            drawLine(graphics, font, Component.translatable("appraisal.imitationcoreapi.aura", number(vitals.aura())), 85);
        }, () -> drawLine(graphics, font, Component.translatable("appraisal.imitationcoreapi.armor", live.armorValue()), 41));
    }

    private static void drawCentered(GuiGraphics graphics, Font font, String value, int x, int y, int width) {
        String text = value.length() > 18 ? value.substring(0, 17) + "…" : value;
        graphics.drawString(font, text, x + Math.max(0, (width - font.width(text)) / 2), y, 0xFFFFFF);
    }

    private static void drawLine(GuiGraphics graphics, Font font, Component text, int y) {
        graphics.drawString(font, text, 7, y, 0xFFFFFF);
    }

    private static String number(double value) {
        if (value >= 1_000_000_000D) {
            return String.format(java.util.Locale.ROOT, "%.1fB", value / 1_000_000_000D);
        }
        if (value >= 1_000_000D) {
            return String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000D);
        }
        if (value >= 1_000D) {
            return String.format(java.util.Locale.ROOT, "%.1fK", value / 1_000D);
        }
        return Long.toString(Math.round(value));
    }
}
