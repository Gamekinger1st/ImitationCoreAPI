package com.github.gamekinger1st.imitationcoreapi.api.network;

import java.util.Objects;
import java.util.Optional;

public final class ClientImitatorFormLibraryStore implements ImitatorFormLibraryListener {
    private volatile ImitatorFormLibraryPayload current;

    public Optional<ImitatorFormLibraryPayload> current() {
        return Optional.ofNullable(current);
    }

    public void clear() {
        current = null;
    }

    @Override
    public void onFormLibrary(ImitatorFormLibraryPayload payload) {
        current = Objects.requireNonNull(payload, "payload");
    }
}
