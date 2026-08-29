package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;
import com.github.gamekinger1st.imitationcoreapi.api.network.CommitImitatorRecordPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ImitatorFormLibraryPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.RequestImitatorFormLibraryPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.SelectImitatorFormPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public final class ImitatorFormScreen extends Screen {
    private static final int COLUMNS = 3;
    private static final int ROWS = 4;
    private static final int PAGE_SIZE = COLUMNS * ROWS;

    private final ImitatorMenuRequest request;
    private ImitatorFormLibraryPayload library;
    private int page;
    private int overwriteSlot = -1;
    private int pendingSelectionSlot = -1;
    private int pendingCommitSlot = -1;
    private boolean waitingForLibrary;
    private Component actionStatus = Component.empty();

    public ImitatorFormScreen(ImitatorMenuRequest request) {
        super(Component.translatable("screen.imitationcoreapi.imitator_forms"));
        if (request == ImitatorMenuRequest.NONE) {
            throw new IllegalArgumentException("A form screen requires a menu request");
        }
        this.request = request;
    }

    @Override
    protected void init() {
        ImitationApi.clientImitatorFormLibrary().clear();
        library = null;
        waitingForLibrary = true;
        rebuild();
        PacketDistributor.sendToServer(new RequestImitatorFormLibraryPayload());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xC0101010);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        if (library == null || waitingForLibrary) {
            graphics.drawCenteredString(font, Component.translatable("screen.imitationcoreapi.loading_forms"), width / 2, 42, 0xA0A0A0);
            return;
        }
        if (overwriteSlot >= 0) {
            graphics.drawCenteredString(font, Component.translatable("screen.imitationcoreapi.replace_recording", overwriteSlot + 1), width / 2, 50, 0xFFAA00);
            return;
        }
        Component instruction = request == ImitatorMenuRequest.COMMIT_RECORD
                ? pendingRecordInstruction()
                : Component.translatable("screen.imitationcoreapi.choose_transform_form");
        graphics.drawCenteredString(font, instruction, width / 2, 36, 0xA0A0A0);
        if (!actionStatus.getString().isEmpty()) {
            graphics.drawCenteredString(font, actionStatus, width / 2, 48, 0xFFAA00);
        }
        int pageCount = pageCount();
        graphics.drawCenteredString(font, Component.translatable("screen.imitationcoreapi.page", page + 1, pageCount), width / 2, height - 42, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ImitationApi.clientImitatorFormLibrary().current()
                .filter(updated -> updated != library)
                .ifPresent(updated -> {
                    library = updated;
                    waitingForLibrary = false;
                    if (pendingSelectionSlot >= 0) {
                        int selectedSlot = pendingSelectionSlot;
                        pendingSelectionSlot = -1;
                        if (library.selectedSlot().isPresent() && library.selectedSlot().getAsInt() == selectedSlot) {
                            onClose();
                            return;
                        }
                        actionStatus = Component.translatable("screen.imitationcoreapi.selection_rejected");
                    }
                    if (pendingCommitSlot >= 0) {
                        int committedSlot = pendingCommitSlot;
                        pendingCommitSlot = -1;
                        if (library.pendingRecord().isEmpty() && form(committedSlot).isPresent()
                                && library.selectedSlot().isPresent() && library.selectedSlot().getAsInt() == committedSlot) {
                            onClose();
                            return;
                        }
                        actionStatus = Component.translatable("screen.imitationcoreapi.recording_not_stored");
                    }
                    overwriteSlot = -1;
                    rebuild();
                });
    }

    private void rebuild() {
        clearWidgets();
        library = ImitationApi.clientImitatorFormLibrary().current().orElse(library);
        if (library == null || waitingForLibrary) {
            addRenderableWidget(Button.builder(Component.translatable("screen.imitationcoreapi.close"), button -> onClose()).bounds(width / 2 - 50, height - 28, 100, 20).build());
            return;
        }
        if (overwriteSlot >= 0) {
            addRenderableWidget(Button.builder(Component.translatable("screen.imitationcoreapi.confirm_replacement"), button -> commit(overwriteSlot)).bounds(width / 2 - 104, 82, 100, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.imitationcoreapi.cancel"), button -> {
                overwriteSlot = -1;
                rebuild();
            }).bounds(width / 2 + 4, 82, 100, 20).build());
            return;
        }
        int startSlot = page * PAGE_SIZE;
        int left = width / 2 - 154;
        for (int index = 0; index < PAGE_SIZE; index++) {
            int slot = startSlot + index;
            if (slot >= library.slotCapacity()) {
                break;
            }
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            Optional<ImitatorFormLibraryPayload.FormSlot> form = form(slot);
            boolean selected = library.selectedSlot().isPresent() && library.selectedSlot().getAsInt() == slot;
            Component label = Component.translatable("screen.imitationcoreapi.slot", slot + 1, formLabel(form), selected ? " *" : "");
            Button button = Button.builder(label, ignored -> chooseSlot(slot, form.isPresent())).bounds(left + column * 104, 58 + row * 22, 100, 20).build();
            button.active = pendingSelectionSlot < 0 && pendingCommitSlot < 0 && (request == ImitatorMenuRequest.COMMIT_RECORD || form.isPresent());
            addRenderableWidget(button);
        }
        if (page > 0) {
            addRenderableWidget(Button.builder(Component.translatable("screen.imitationcoreapi.previous"), button -> {
                page--;
                rebuild();
            }).bounds(width / 2 - 104, height - 28, 100, 20).build());
        }
        if (page + 1 < pageCount()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.imitationcoreapi.next"), button -> {
                page++;
                rebuild();
            }).bounds(width / 2 + 4, height - 28, 100, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.imitationcoreapi.refresh"), button -> refresh()).bounds(width / 2 - 50, height - 54, 100, 20).build());
    }

    private void chooseSlot(int slot, boolean occupied) {
        if (request == ImitatorMenuRequest.COMMIT_RECORD) {
            if (occupied) {
                overwriteSlot = slot;
                rebuild();
            } else {
                commit(slot);
            }
            return;
        }
        pendingSelectionSlot = slot;
        actionStatus = Component.translatable("screen.imitationcoreapi.selecting_form", slot + 1);
        PacketDistributor.sendToServer(new SelectImitatorFormPayload(slot));
        rebuild();
    }

    private void commit(int slot) {
        pendingCommitSlot = slot;
        overwriteSlot = -1;
        actionStatus = Component.translatable("screen.imitationcoreapi.storing_recording", slot + 1);
        PacketDistributor.sendToServer(new CommitImitatorRecordPayload(slot));
        rebuild();
    }

    private void refresh() {
        waitingForLibrary = true;
        actionStatus = Component.empty();
        rebuild();
        PacketDistributor.sendToServer(new RequestImitatorFormLibraryPayload());
    }

    private Optional<ImitatorFormLibraryPayload.FormSlot> form(int slot) {
        return library.forms().stream().filter(form -> form.slot() == slot).findFirst();
    }

    private int pageCount() {
        return Math.max(1, (library.slotCapacity() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private Component pendingRecordInstruction() {
        return library.pendingRecord()
                .<Component>map(record -> Component.translatable("screen.imitationcoreapi.choose_record_slot_precision", Math.round(record.precision() * 100D)))
                .orElseGet(() -> Component.translatable("screen.imitationcoreapi.choose_record_slot"));
    }

    private static Component formLabel(Optional<ImitatorFormLibraryPayload.FormSlot> form) {
        if (form.isEmpty()) {
            return Component.translatable("screen.imitationcoreapi.empty");
        }
        ImitatorFormLibraryPayload.FormSlot slot = form.get();
        if (slot.displayName().isBlank()) {
            return Component.translatable("screen.imitationcoreapi.unknown_form");
        }
        String name = slot.displayName();
        return Component.literal(name.length() > 16 ? name.substring(0, 16) : name);
    }
}
