package com.github.gamekinger1st.imitationcoreapi.api.session;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigration;
import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigrationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotSerialization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class SessionSerialization {
    public static final int CURRENT_SCHEMA_VERSION = 5;
    private static final SchemaMigrationRegistry MIGRATIONS = new SchemaMigrationRegistry();

    static {
        MIGRATIONS.register(1, source -> {
            ListTag temporaryState = source.getList("temporary_state", Tag.TAG_COMPOUND);
            for (int index = 0; index < temporaryState.size(); index++) {
                temporaryState.getCompound(index).putString("handler", TemporaryStateKinds.UNASSIGNED_HANDLER.toString());
            }
            source.put("temporary_state", temporaryState);
            return source;
        });
        MIGRATIONS.register(2, source -> {
            source.putString("scope", TransformationScope.GAMEPLAY.name());
            return source;
        });
        MIGRATIONS.register(3, source -> {
            source.putDouble("gameplay_scale", 1D);
            return source;
        });
        MIGRATIONS.register(4, source -> {
            source.putBoolean("has_expiration", false);
            return source;
        });
    }

    private SessionSerialization() {
    }

    public static CompoundTag toTag(TransformationSession session) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", CURRENT_SCHEMA_VERSION);
        tag.putUUID("id", session.sessionId());
        tag.putUUID("owner", session.ownerId());
        tag.putUUID("snapshot", session.snapshotId());
        tag.putString("scope", session.scope().name());
        tag.putDouble("gameplay_scale", session.gameplayScale());
        tag.put("baseline", SnapshotSerialization.baselineToTag(session.baseline()));
        tag.put("compatibility", compatibilityToTag(session.compatibility()));
        tag.putString("state", session.state().name());
        tag.putLong("created_game_time", session.createdGameTime());
        tag.putLong("updated_game_time", session.updatedGameTime());
        tag.putLong("revision", session.revision());
        tag.putBoolean("has_expiration", session.expiresGameTime().isPresent());
        session.expiresGameTime().ifPresent(value -> tag.putLong("expires_game_time", value));
        session.failureDetail().ifPresent(value -> tag.putString("failure", value));
        ListTag temporaryState = new ListTag();
        for (TemporaryStateReference reference : session.temporaryState()) {
            temporaryState.add(temporaryStateToTag(reference));
        }
        tag.put("temporary_state", temporaryState);
        return tag;
    }

    public static TransformationSession fromTag(CompoundTag tag) {
        tag = migrate(tag);
        UUID sessionId = requireUuid(tag, "id");
        UUID ownerId = requireUuid(tag, "owner");
        UUID snapshotId = requireUuid(tag, "snapshot");
        TransformationScope scope = requireEnum(tag, "scope", TransformationScope.class);
        if (!tag.contains("gameplay_scale", Tag.TAG_DOUBLE)) {
            throw new IllegalArgumentException("Missing gameplay scale");
        }
        double gameplayScale = tag.getDouble("gameplay_scale");
        BaselineSnapshot baseline = SnapshotSerialization.baselineFromTag(requireCompound(tag, "baseline"));
        CompatibilityAssessment compatibility = compatibilityFromTag(requireCompound(tag, "compatibility"));
        TransformationState state = requireEnum(tag, "state", TransformationState.class);
        long createdGameTime = tag.getLong("created_game_time");
        long updatedGameTime = tag.getLong("updated_game_time");
        long revision = tag.getLong("revision");
        OptionalLong expiresGameTime = OptionalLong.empty();
        if (tag.getBoolean("has_expiration")) {
            if (!tag.contains("expires_game_time", Tag.TAG_LONG)) {
                throw new IllegalArgumentException("Missing expiration game time");
            }
            expiresGameTime = OptionalLong.of(tag.getLong("expires_game_time"));
        }
        Optional<String> failure = tag.contains("failure", Tag.TAG_STRING) ? Optional.of(tag.getString("failure")) : Optional.empty();
        List<TemporaryStateReference> temporaryState = new ArrayList<>();
        ListTag temporaryTags = tag.getList("temporary_state", Tag.TAG_COMPOUND);
        if (temporaryTags.size() > TransformationSession.MAX_TEMPORARY_STATE_REFERENCES) {
            throw new IllegalArgumentException("Transformation session has too many temporary state references");
        }
        for (int index = 0; index < temporaryTags.size(); index++) {
            temporaryState.add(temporaryStateFromTag(temporaryTags.getCompound(index)));
        }
        return new TransformationSession(sessionId, ownerId, snapshotId, scope, gameplayScale, baseline, compatibility, state, createdGameTime, updatedGameTime, revision, expiresGameTime, failure, temporaryState);
    }

    public static void registerMigration(int sourceVersion, SchemaMigration migration) {
        MIGRATIONS.register(sourceVersion, migration);
    }

    private static CompoundTag compatibilityToTag(CompatibilityAssessment assessment) {
        CompoundTag tag = new CompoundTag();
        tag.putString("level", assessment.level().name());
        ListTag reasons = new ListTag();
        for (String reason : assessment.reasons()) {
            CompoundTag reasonTag = new CompoundTag();
            reasonTag.putString("value", reason);
            reasons.add(reasonTag);
        }
        tag.put("reasons", reasons);
        return tag;
    }

    private static CompatibilityAssessment compatibilityFromTag(CompoundTag tag) {
        CompatibilityLevel level = requireEnum(tag, "level", CompatibilityLevel.class);
        List<String> reasons = new ArrayList<>();
        ListTag reasonTags = tag.getList("reasons", Tag.TAG_COMPOUND);
        if (reasonTags.size() > CompatibilityAssessment.MAX_REASONS) {
            throw new IllegalArgumentException("Transformation compatibility has too many reasons");
        }
        for (int index = 0; index < reasonTags.size(); index++) {
            reasons.add(reasonTags.getCompound(index).getString("value"));
        }
        return new CompatibilityAssessment(level, reasons);
    }

    private static CompoundTag temporaryStateToTag(TemporaryStateReference reference) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", reference.referenceId());
        tag.putUUID("session", reference.sessionId());
        tag.putString("handler", reference.handlerId().toString());
        tag.putString("kind", reference.kind().toString());
        tag.put("payload", reference.payload());
        tag.putString("status", reference.status().name());
        return tag;
    }

    private static TemporaryStateReference temporaryStateFromTag(CompoundTag tag) {
        UUID referenceId = requireUuid(tag, "id");
        UUID sessionId = requireUuid(tag, "session");
        ResourceLocation handlerId = ResourceLocation.tryParse(tag.getString("handler"));
        if (handlerId == null) {
            throw new IllegalArgumentException("Invalid temporary state handler");
        }
        ResourceLocation kind = ResourceLocation.tryParse(tag.getString("kind"));
        if (kind == null) {
            throw new IllegalArgumentException("Invalid temporary state kind");
        }
        CompoundTag payload = requireCompound(tag, "payload");
        TemporaryStateStatus status = requireEnum(tag, "status", TemporaryStateStatus.class);
        return new TemporaryStateReference(referenceId, sessionId, handlerId, kind, payload, status);
    }

    private static UUID requireUuid(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) {
            throw new IllegalArgumentException("Missing UUID field: " + key);
        }
        return tag.getUUID(key);
    }

    private static CompoundTag migrate(CompoundTag source) {
        if (!source.contains("schema", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing transformation session schema");
        }
        int sourceVersion = source.getInt("schema");
        if (sourceVersion < 1 || sourceVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported transformation session schema version: " + sourceVersion);
        }
        if (sourceVersion == CURRENT_SCHEMA_VERSION) {
            return source.copy();
        }
        CompoundTag migrated = MIGRATIONS.migrate(source, sourceVersion, CURRENT_SCHEMA_VERSION);
        migrated.putInt("schema", CURRENT_SCHEMA_VERSION);
        return migrated;
    }

    private static CompoundTag requireCompound(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Missing compound field: " + key);
        }
        return tag.getCompound(key);
    }

    private static <T extends Enum<T>> T requireEnum(CompoundTag tag, String key, Class<T> enumType) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Missing enum field: " + key);
        }
        try {
            return Enum.valueOf(enumType, tag.getString(key));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + enumType.getSimpleName() + " field: " + key, exception);
        }
    }
}
