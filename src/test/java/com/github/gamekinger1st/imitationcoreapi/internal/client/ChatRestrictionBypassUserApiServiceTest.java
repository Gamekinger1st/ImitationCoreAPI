package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRestrictionBypassUserApiServiceTest {
    @Test
    void forcesChatServerAndRealmsPropertiesWithoutDelegateFetch() throws AuthenticationException {
        ChatRestrictionBypassUserApiService service = new ChatRestrictionBypassUserApiService(new ThrowingFetchService());
        UserProperties properties = service.fetchProperties();

        assertTrue(properties.flag(UserFlag.CHAT_ALLOWED));
        assertTrue(properties.flag(UserFlag.SERVERS_ALLOWED));
        assertTrue(properties.flag(UserFlag.REALMS_ALLOWED));
        assertFalse(properties.bannedScopes().containsKey("MULTIPLAYER"));
    }

    @Test
    void disablesTelemetrySession() {
        ChatRestrictionBypassUserApiService service = new ChatRestrictionBypassUserApiService(new ThrowingFetchService());

        assertSame(TelemetrySession.DISABLED, service.newTelemetrySession(Runnable::run));
    }

    private static final class ThrowingFetchService implements UserApiService {
        @Override
        public UserProperties fetchProperties() throws AuthenticationException {
            throw new AuthenticationException("blocked");
        }

        @Override
        public boolean isBlockedPlayer(UUID playerId) {
            return false;
        }

        @Override
        public void refreshBlockList() {
        }

        @Override
        public TelemetrySession newTelemetrySession(Executor executor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public KeyPairResponse getKeyPair() {
            return null;
        }

        @Override
        public void reportAbuse(AbuseReportRequest request) {
        }

        @Override
        public boolean canSendReports() {
            return false;
        }

        @Override
        public AbuseReportLimits getAbuseReportLimits() {
            return null;
        }
    }
}
