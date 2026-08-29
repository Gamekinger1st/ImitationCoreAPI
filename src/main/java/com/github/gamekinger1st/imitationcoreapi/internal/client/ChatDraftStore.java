package com.github.gamekinger1st.imitationcoreapi.internal.client;

import java.util.Objects;

public final class ChatDraftStore {
    private static final int MAX_LENGTH = 256;
    private static String draft = "";
    private static boolean acceptsSaves = true;

    private ChatDraftStore() {
    }

    public static String restore(String initial) {
        Objects.requireNonNull(initial, "initial");
        acceptsSaves = true;
        return initial.isEmpty() ? draft : initial;
    }

    public static void save(String value) {
        Objects.requireNonNull(value, "value");
        if (!acceptsSaves) {
            return;
        }
        draft = value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }

    public static void clear() {
        draft = "";
        acceptsSaves = true;
    }

    public static void discard() {
        draft = "";
        acceptsSaves = false;
    }
}
