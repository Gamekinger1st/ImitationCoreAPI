package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;

@FunctionalInterface
public interface ImitatorMenuListener {
    void onImitatorMenuRequested(ImitatorMenuRequest request);
}
