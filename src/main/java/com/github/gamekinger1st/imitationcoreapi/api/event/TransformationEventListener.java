package com.github.gamekinger1st.imitationcoreapi.api.event;

@FunctionalInterface
public interface TransformationEventListener {
    void onTransformationEvent(TransformationEvent event);
}
