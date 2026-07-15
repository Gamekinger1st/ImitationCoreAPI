package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class ChatRestrictionBypassUserApiService implements UserApiService {
    private static final UserProperties FORCED_PROPERTIES = new UserProperties(
            Set.copyOf(EnumSet.of(UserFlag.CHAT_ALLOWED, UserFlag.SERVERS_ALLOWED, UserFlag.REALMS_ALLOWED)),
            Map.<String, BanDetails>of()
    );
    private final UserApiService delegate;

    public ChatRestrictionBypassUserApiService(UserApiService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public UserProperties fetchProperties() throws AuthenticationException {
        return FORCED_PROPERTIES;
    }

    @Override
    public boolean isBlockedPlayer(UUID playerId) {
        return delegate.isBlockedPlayer(playerId);
    }

    @Override
    public void refreshBlockList() {
        delegate.refreshBlockList();
    }

    @Override
    public TelemetrySession newTelemetrySession(Executor executor) {
        return TelemetrySession.DISABLED;
    }

    @Override
    public KeyPairResponse getKeyPair() {
        return delegate.getKeyPair();
    }

    @Override
    public void reportAbuse(AbuseReportRequest request) {
        delegate.reportAbuse(request);
    }

    @Override
    public boolean canSendReports() {
        return delegate.canSendReports();
    }

    @Override
    public AbuseReportLimits getAbuseReportLimits() {
        return delegate.getAbuseReportLimits();
    }
}
