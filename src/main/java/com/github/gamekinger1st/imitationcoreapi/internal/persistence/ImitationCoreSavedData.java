package com.github.gamekinger1st.imitationcoreapi.internal.persistence;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationRepository;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormLibrarySerialization;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormLibraryState;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormRepository;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelPreferenceRepository;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelPreferenceSerialization;
import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigration;
import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigrationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.session.SessionSerialization;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotSerialization;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

public final class ImitationCoreSavedData extends SavedData implements TransformationRepository, ImitatorFormRepository, ChatChannelPreferenceRepository {
    private static final String DATA_ID = ImitationCoreApi.MOD_ID + "_state";
    private static final int DATA_VERSION = 3;
    private static final SchemaMigrationRegistry MIGRATIONS = new SchemaMigrationRegistry();
    private static final SavedData.Factory<ImitationCoreSavedData> FACTORY = new SavedData.Factory<>(ImitationCoreSavedData::new, ImitationCoreSavedData::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    static {
        MIGRATIONS.register(1, source -> {
            source.put("form_libraries", new ListTag());
            return source;
        });
        MIGRATIONS.register(2, source -> {
            source.put("chat_channels", new ListTag());
            return source;
        });
    }

    private final Map<UUID, IdentitySnapshot> snapshots = new LinkedHashMap<>();
    private final Map<UUID, TransformationSession> sessions = new LinkedHashMap<>();
    private final Map<UUID, ImitatorFormLibraryState> formLibraries = new LinkedHashMap<>();
    private final Map<UUID, ResourceLocation> chatChannels = new LinkedHashMap<>();

    public static ImitationCoreSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    private static ImitationCoreSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        tag = migrate(tag);
        ImitationCoreSavedData data = new ImitationCoreSavedData();
        loadSnapshots(data, tag.getList("snapshots", Tag.TAG_COMPOUND));
        loadSessions(data, tag.getList("sessions", Tag.TAG_COMPOUND));
        loadFormLibraries(data, tag.getList("form_libraries", Tag.TAG_COMPOUND));
        loadChatChannels(data, tag.getList("chat_channels", Tag.TAG_COMPOUND));
        return data;
    }

    public static void registerMigration(int sourceVersion, SchemaMigration migration) {
        MIGRATIONS.register(sourceVersion, migration);
    }

    private static void loadSnapshots(ImitationCoreSavedData data, ListTag tags) {
        for (int index = 0; index < tags.size(); index++) {
            try {
                IdentitySnapshot snapshot = SnapshotSerialization.identityFromTag(tags.getCompound(index));
                data.snapshots.put(snapshot.snapshotId(), snapshot);
            } catch (RuntimeException exception) {
                ImitationCoreApi.LOGGER.warn("Discarded invalid imitation snapshot from persistent data: {}", exception.getMessage());
            }
        }
    }

    private static void loadSessions(ImitationCoreSavedData data, ListTag tags) {
        for (int index = 0; index < tags.size(); index++) {
            try {
                TransformationSession session = SessionSerialization.fromTag(tags.getCompound(index));
                data.sessions.put(session.sessionId(), session);
            } catch (RuntimeException exception) {
                ImitationCoreApi.LOGGER.warn("Discarded invalid imitation transformation session from persistent data: {}", exception.getMessage());
            }
        }
    }

    private static void loadFormLibraries(ImitationCoreSavedData data, ListTag tags) {
        for (int index = 0; index < tags.size(); index++) {
            try {
                CompoundTag tag = tags.getCompound(index);
                if (!tag.hasUUID("owner")) {
                    throw new IllegalArgumentException("Missing form library owner");
                }
                if (!tag.contains("library", Tag.TAG_COMPOUND)) {
                    throw new IllegalArgumentException("Missing form library data");
                }
                data.formLibraries.put(tag.getUUID("owner"), ImitatorFormLibrarySerialization.fromTag(tag.getCompound("library")));
            } catch (RuntimeException exception) {
                ImitationCoreApi.LOGGER.warn("Discarded invalid imitation form library from persistent data: {}", exception.getMessage());
            }
        }
    }

    private static void loadChatChannels(ImitationCoreSavedData data, ListTag tags) {
        for (int index = 0; index < tags.size(); index++) {
            try {
                ChatChannelPreferenceSerialization.Entry entry = ChatChannelPreferenceSerialization.fromTag(tags.getCompound(index));
                data.chatChannels.put(entry.playerId(), entry.channelId());
            } catch (RuntimeException exception) {
                ImitationCoreApi.LOGGER.warn("Discarded invalid active chat channel from persistent data: {}", exception.getMessage());
            }
        }
    }

    @Override
    public synchronized Optional<IdentitySnapshot> snapshot(UUID snapshotId) {
        return Optional.ofNullable(snapshots.get(snapshotId));
    }

    @Override
    public synchronized void saveSnapshot(IdentitySnapshot snapshot) {
        snapshots.put(snapshot.snapshotId(), snapshot);
        setDirty();
    }

    @Override
    public synchronized Optional<TransformationSession> session(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public synchronized Collection<TransformationSession> sessionsForOwner(UUID ownerId) {
        return sessions.values().stream().filter(session -> session.ownerId().equals(ownerId)).toList();
    }

    @Override
    public synchronized Collection<TransformationSession> sessions() {
        return List.copyOf(sessions.values());
    }

    @Override
    public synchronized void saveSession(TransformationSession session) {
        sessions.put(session.sessionId(), session);
        setDirty();
    }

    @Override
    public synchronized ImitatorFormLibraryState formLibrary(UUID ownerId) {
        return formLibraries.getOrDefault(ownerId, ImitatorFormLibraryState.empty());
    }

    @Override
    public synchronized void saveFormLibrary(UUID ownerId, ImitatorFormLibraryState library) {
        formLibraries.put(ownerId, library);
        setDirty();
    }

    @Override
    public synchronized Optional<ResourceLocation> activeChatChannel(UUID playerId) {
        return Optional.ofNullable(chatChannels.get(playerId));
    }

    @Override
    public synchronized void saveActiveChatChannel(UUID playerId, ResourceLocation channelId) {
        chatChannels.put(Objects.requireNonNull(playerId, "playerId"), Objects.requireNonNull(channelId, "channelId"));
        setDirty();
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("data_version", DATA_VERSION);
        ListTag snapshotTags = new ListTag();
        for (IdentitySnapshot snapshot : snapshots.values()) {
            snapshotTags.add(SnapshotSerialization.identityToTag(snapshot));
        }
        ListTag sessionTags = new ListTag();
        for (TransformationSession session : sessions.values()) {
            sessionTags.add(SessionSerialization.toTag(session));
        }
        ListTag formLibraryTags = new ListTag();
        for (Map.Entry<UUID, ImitatorFormLibraryState> entry : formLibraries.entrySet()) {
            CompoundTag formLibrary = new CompoundTag();
            formLibrary.putUUID("owner", entry.getKey());
            formLibrary.put("library", ImitatorFormLibrarySerialization.toTag(entry.getValue()));
            formLibraryTags.add(formLibrary);
        }
        tag.put("snapshots", snapshotTags);
        tag.put("sessions", sessionTags);
        tag.put("form_libraries", formLibraryTags);
        ListTag chatChannelTags = new ListTag();
        for (Map.Entry<UUID, ResourceLocation> entry : chatChannels.entrySet()) {
            chatChannelTags.add(ChatChannelPreferenceSerialization.toTag(entry.getKey(), entry.getValue()));
        }
        tag.put("chat_channels", chatChannelTags);
        return tag;
    }

    private static CompoundTag migrate(CompoundTag source) {
        if (!source.contains("data_version", Tag.TAG_INT)) {
            throw new IllegalStateException("Missing Imitation Core API data version");
        }
        int sourceVersion = source.getInt("data_version");
        if (sourceVersion < 1 || sourceVersion > DATA_VERSION) {
            throw new IllegalStateException("Unsupported Imitation Core API data version: " + sourceVersion);
        }
        if (sourceVersion == DATA_VERSION) {
            return source.copy();
        }
        CompoundTag migrated = MIGRATIONS.migrate(source, sourceVersion, DATA_VERSION);
        migrated.putInt("data_version", DATA_VERSION);
        return migrated;
    }
}
