package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class ImitatorAppraisalRenderer {
    private static final ResourceLocation ENTITY_ANALYSIS = ResourceLocation.fromNamespaceAndPath("tensura", "textures/player_hud/analysis/analysis_overlay.png");

    private ImitatorAppraisalRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font, float opacity, float scale, float x, float y, boolean leftSide, ClientDisguiseState state, DisguiseAppraisalSnapshot appraisal) {
        if (graphics == null || font == null || state == null || appraisal == null) {
            return;
        }
        float safeScale = Math.clamp(scale, 0.05F, 5F);
        float safeOpacity = Math.clamp(opacity, 0F, 1F);
        var pose = graphics.pose();
        pose.pushPose();
        pose.scale(safeScale, safeScale, 1F);
        pose.translate(leftSide ? x / safeScale : (x - 95F * (safeScale - 1F)) / safeScale, y / safeScale, 0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, safeOpacity);
        if (leftSide) {
            graphics.blit(ENTITY_ANALYSIS, 0, 0, 0, 0, 95, 156);
        } else {
            pose.pushPose();
            pose.translate(95F, 0F, 0F);
            pose.scale(-1F, 1F, 1F);
            graphics.blit(ENTITY_ANALYSIS, 0, 0, 0, 0, 95, 156);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        drawCentered(graphics, font, state.displayName(), leftSide ? 8 : 9, 12, 78);
        int lineY = 30;
        drawLine(graphics, font, "HP: " + Math.round(appraisal.health()), lineY);
        appraisal.tensuraVitals().ifPresentOrElse(vitals -> {
            drawLine(graphics, font, "SHP: " + Math.round(vitals.spiritualHealth()), 41);
            drawLine(graphics, font, "Armor: " + appraisal.armorValue(), 52);
            drawLine(graphics, font, "EP: " + number(vitals.ep()), 63);
            drawLine(graphics, font, "MP: " + number(vitals.magicule()), 74);
            drawLine(graphics, font, "Aura: " + number(vitals.aura()), 85);
        }, () -> drawLine(graphics, font, "Armor: " + appraisal.armorValue(), 41));
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        pose.popPose();
    }

    private static void drawCentered(GuiGraphics graphics, Font font, String value, int x, int y, int width) {
        String text = value.length() > 18 ? value.substring(0, 17) + "…" : value;
        graphics.drawString(font, text, x + Math.max(0, (width - font.width(text)) / 2), y, 0xFFFFFF);
    }

    private static void drawLine(GuiGraphics graphics, Font font, String text, int y) {
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
