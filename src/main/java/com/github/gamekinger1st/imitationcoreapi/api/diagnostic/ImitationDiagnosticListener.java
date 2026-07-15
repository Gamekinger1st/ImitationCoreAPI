package com.github.gamekinger1st.imitationcoreapi.api.diagnostic;

@FunctionalInterface
public interface ImitationDiagnosticListener {
    void onImitationDiagnostic(ImitationDiagnostic diagnostic);
}
