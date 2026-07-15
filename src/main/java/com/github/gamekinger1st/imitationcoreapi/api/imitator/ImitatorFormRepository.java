package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.UUID;

public interface ImitatorFormRepository {
    ImitatorFormLibraryState formLibrary(UUID ownerId);

    void saveFormLibrary(UUID ownerId, ImitatorFormLibraryState library);
}
