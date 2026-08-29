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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ImitationCoreSavedData extends SavedData implements TransformationRepository, ImitatorFormRepository, ChatChannelPreferenceRepository {
    private static final String DATA_ID = ImitationCoreApi.MOD_ID + "_state";
    private static final int DATA_VERSION = 4;
    private static final int MAX_RETAINED_TERMINAL_SESSIONS = 512;
    private static final int MAX_RETAINED_ORPHAN_SNAPSHOTS = 256;
    private static final int MAX_RETAINED_PLAYER_PREFERENCES = 4_096;
    private static final String PRESERVED_FUTURE_DATA = "preserved_future_data";
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
        MIGRATIONS.register(3, source -> source);
    }

    private final Map<UUID, IdentitySnapshot> snapshots = new LinkedHashMap<>();
    private final Map<UUID, TransformationSession> sessions = new LinkedHashMap<>();
    private final Map<UUID, ImitatorFormLibraryState> formLibraries = new LinkedHashMap<>();
    private final Map<UUID, ResourceLocation> chatChannels = new LinkedHashMap<>();
    private final Map<UUID, Long> ownerLastTouched = new LinkedHashMap<>();
    private CompoundTag preservedFutureData = new CompoundTag();

    public static ImitationCoreSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    private static ImitationCoreSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        tag = migrate(tag);
        ImitationCoreSavedData data = new ImitationCoreSavedData();
        if (tag.contains(PRESERVED_FUTURE_DATA, Tag.TAG_COMPOUND)) {
            data.preservedFutureData = tag.getCompound(PRESERVED_FUTURE_DATA).copy();
        }
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
                data.ownerLastTouched.merge(tag.getUUID("owner"), tag.getLong("last_touched"), Math::max);
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
                data.ownerLastTouched.merge(entry.playerId(), tags.getCompound(index).getLong("last_touched"), Math::max);
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
        touch(ownerId);
        setDirty();
    }

    @Override
    public synchronized Optional<ResourceLocation> activeChatChannel(UUID playerId) {
        return Optional.ofNullable(chatChannels.get(playerId));
    }

    @Override
    public synchronized void saveActiveChatChannel(UUID playerId, ResourceLocation channelId) {
        chatChannels.put(Objects.requireNonNull(playerId, "playerId"), Objects.requireNonNull(channelId, "channelId"));
        touch(playerId);
        setDirty();
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        pruneHistoricalData();
        tag.putInt("data_version", DATA_VERSION);
        if (!preservedFutureData.isEmpty()) {
            tag.put(PRESERVED_FUTURE_DATA, preservedFutureData.copy());
        }
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
            formLibrary.putLong("last_touched", ownerLastTouched.getOrDefault(entry.getKey(), 0L));
            formLibraryTags.add(formLibrary);
        }
        tag.put("snapshots", snapshotTags);
        tag.put("sessions", sessionTags);
        tag.put("form_libraries", formLibraryTags);
        ListTag chatChannelTags = new ListTag();
        for (Map.Entry<UUID, ResourceLocation> entry : chatChannels.entrySet()) {
            CompoundTag channel = ChatChannelPreferenceSerialization.toTag(entry.getKey(), entry.getValue());
            channel.putLong("last_touched", ownerLastTouched.getOrDefault(entry.getKey(), 0L));
            chatChannelTags.add(channel);
        }
        tag.put("chat_channels", chatChannelTags);
        return tag;
    }

    private void pruneHistoricalData() {
        Set<UUID> retainedTerminalSessions = sessions.values().stream()
                .filter(session -> session.state().isTerminal())
                .sorted(Comparator.comparingLong(TransformationSession::updatedGameTime).reversed())
                .limit(MAX_RETAINED_TERMINAL_SESSIONS)
                .map(TransformationSession::sessionId)
                .collect(java.util.stream.Collectors.toSet());
        sessions.values().removeIf(session -> session.state().isTerminal() && !retainedTerminalSessions.contains(session.sessionId()));

        Set<UUID> referencedSnapshots = new HashSet<>();
        sessions.values().forEach(session -> referencedSnapshots.add(session.snapshotId()));
        formLibraries.values().forEach(library -> {
            library.forms().values().forEach(form -> referencedSnapshots.add(form.snapshotId()));
            library.pendingRecord().ifPresent(pending -> referencedSnapshots.add(pending.snapshotId()));
        });
        Set<UUID> retainedOrphans = snapshots.values().stream()
                .filter(snapshot -> !referencedSnapshots.contains(snapshot.snapshotId()))
                .sorted(Comparator.comparingLong(IdentitySnapshot::capturedGameTime).reversed())
                .limit(MAX_RETAINED_ORPHAN_SNAPSHOTS)
                .map(IdentitySnapshot::snapshotId)
                .collect(java.util.stream.Collectors.toSet());
        snapshots.keySet().removeIf(snapshotId -> !referencedSnapshots.contains(snapshotId) && !retainedOrphans.contains(snapshotId));

        Set<UUID> activeOwners = sessions.values().stream()
                .filter(session -> !session.state().isTerminal())
                .map(TransformationSession::ownerId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> retainedOwners = ownerLastTouched.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed().thenComparing(entry -> entry.getKey().toString()))
                .limit(MAX_RETAINED_PLAYER_PREFERENCES)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        retainedOwners.addAll(activeOwners);
        formLibraries.keySet().removeIf(owner -> !retainedOwners.contains(owner));
        chatChannels.keySet().removeIf(owner -> !retainedOwners.contains(owner));
        ownerLastTouched.keySet().removeIf(owner -> !formLibraries.containsKey(owner) && !chatChannels.containsKey(owner) && !activeOwners.contains(owner));
    }

    private static CompoundTag migrate(CompoundTag source) {
        if (!source.contains("data_version", Tag.TAG_INT)) {
            source = source.copy();
            source.putInt("data_version", 1);
        }
        int sourceVersion = source.getInt("data_version");
        if (sourceVersion < 1) {
            sourceVersion = 1;
            source = source.copy();
            source.putInt("data_version", sourceVersion);
        }
        if (sourceVersion > DATA_VERSION) {
            ImitationCoreApi.LOGGER.error("Quarantined unsupported future Imitation Core API data version {} instead of failing world load", sourceVersion);
            CompoundTag quarantined = new CompoundTag();
            quarantined.putInt("data_version", DATA_VERSION);
            quarantined.put("snapshots", new ListTag());
            quarantined.put("sessions", new ListTag());
            quarantined.put("form_libraries", new ListTag());
            quarantined.put("chat_channels", new ListTag());
            quarantined.put(PRESERVED_FUTURE_DATA, source.copy());
            return quarantined;
        }
        if (sourceVersion == DATA_VERSION) {
            return source.copy();
        }
        CompoundTag migrated = MIGRATIONS.migrate(source, sourceVersion, DATA_VERSION);
        migrated.putInt("data_version", DATA_VERSION);
        return migrated;
    }

    private void touch(UUID ownerId) {
        ownerLastTouched.put(Objects.requireNonNull(ownerId, "ownerId"), System.currentTimeMillis());
    }
}
