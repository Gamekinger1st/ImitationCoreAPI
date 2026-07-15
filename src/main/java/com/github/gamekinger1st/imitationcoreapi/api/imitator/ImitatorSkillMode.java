package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.List;

public enum ImitatorSkillMode {
    RECORD("Record"),
    TRANSFORM("Transform"),
    REPLICA("Replica");

    private final String displayName;

    ImitatorSkillMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public ImitatorSkillMode next() {
        ImitatorSkillMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    public static List<ImitatorSkillMode> ordered() {
        return List.of(values());
    }
}
