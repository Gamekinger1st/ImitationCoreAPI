package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;
import net.minecraft.client.Minecraft;

public final class ClientImitatorFormMenus {
    private ClientImitatorFormMenus() {
    }

    public static void open(ImitatorMenuRequest request) {
        Minecraft.getInstance().setScreen(new ImitatorFormScreen(request));
    }
}
