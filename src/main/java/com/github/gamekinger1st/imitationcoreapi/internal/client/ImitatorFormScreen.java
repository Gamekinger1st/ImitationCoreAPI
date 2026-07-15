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
    private String actionStatus = "";

    public ImitatorFormScreen(ImitatorMenuRequest request) {
        super(Component.literal("Imitator Forms"));
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
            graphics.drawCenteredString(font, Component.literal("Loading form library..."), width / 2, 42, 0xA0A0A0);
            return;
        }
        if (overwriteSlot >= 0) {
            graphics.drawCenteredString(font, Component.literal("Replace the recording in slot " + (overwriteSlot + 1) + "?"), width / 2, 50, 0xFFAA00);
            return;
        }
        String instruction = request == ImitatorMenuRequest.COMMIT_RECORD
                ? pendingRecordInstruction()
                : "Choose a recorded form to transform into";
        graphics.drawCenteredString(font, Component.literal(instruction), width / 2, 36, 0xA0A0A0);
        if (!actionStatus.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal(actionStatus), width / 2, 48, 0xFFAA00);
        }
        int pageCount = pageCount();
        graphics.drawCenteredString(font, Component.literal("Page " + (page + 1) + " / " + pageCount), width / 2, height - 42, 0xA0A0A0);
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
                        actionStatus = "Selection was not accepted";
                    }
                    if (pendingCommitSlot >= 0) {
                        int committedSlot = pendingCommitSlot;
                        pendingCommitSlot = -1;
                        if (library.pendingRecord().isEmpty() && form(committedSlot).isPresent()
                                && library.selectedSlot().isPresent() && library.selectedSlot().getAsInt() == committedSlot) {
                            onClose();
                            return;
                        }
                        actionStatus = "Recording was not stored";
                    }
                    overwriteSlot = -1;
                    rebuild();
                });
    }

    private void rebuild() {
        clearWidgets();
        library = ImitationApi.clientImitatorFormLibrary().current().orElse(library);
        if (library == null || waitingForLibrary) {
            addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose()).bounds(width / 2 - 50, height - 28, 100, 20).build());
            return;
        }
        if (overwriteSlot >= 0) {
            addRenderableWidget(Button.builder(Component.literal("Confirm replacement"), button -> commit(overwriteSlot)).bounds(width / 2 - 104, 82, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
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
            String label = "Slot " + (slot + 1) + ": " + formLabel(form) + (selected ? " *" : "");
            Button button = Button.builder(Component.literal(label), ignored -> chooseSlot(slot, form.isPresent())).bounds(left + column * 104, 58 + row * 22, 100, 20).build();
            button.active = pendingSelectionSlot < 0 && pendingCommitSlot < 0 && (request == ImitatorMenuRequest.COMMIT_RECORD || form.isPresent());
            addRenderableWidget(button);
        }
        if (page > 0) {
            addRenderableWidget(Button.builder(Component.literal("Previous"), button -> {
                page--;
                rebuild();
            }).bounds(width / 2 - 104, height - 28, 100, 20).build());
        }
        if (page + 1 < pageCount()) {
            addRenderableWidget(Button.builder(Component.literal("Next"), button -> {
                page++;
                rebuild();
            }).bounds(width / 2 + 4, height - 28, 100, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> refresh()).bounds(width / 2 - 50, height - 54, 100, 20).build());
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
        actionStatus = "Selecting form in slot " + (slot + 1) + "...";
        PacketDistributor.sendToServer(new SelectImitatorFormPayload(slot));
        rebuild();
    }

    private void commit(int slot) {
        pendingCommitSlot = slot;
        overwriteSlot = -1;
        actionStatus = "Storing recording in slot " + (slot + 1) + "...";
        PacketDistributor.sendToServer(new CommitImitatorRecordPayload(slot));
        rebuild();
    }

    private void refresh() {
        waitingForLibrary = true;
        actionStatus = "";
        rebuild();
        PacketDistributor.sendToServer(new RequestImitatorFormLibraryPayload());
    }

    private Optional<ImitatorFormLibraryPayload.FormSlot> form(int slot) {
        return library.forms().stream().filter(form -> form.slot() == slot).findFirst();
    }

    private int pageCount() {
        return Math.max(1, (library.slotCapacity() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private String pendingRecordInstruction() {
        return library.pendingRecord()
                .map(record -> "Choose a slot for the pending " + Math.round(record.precision() * 100D) + "% recording")
                .orElse("Choose a slot for the pending recording");
    }

    private static String formLabel(Optional<ImitatorFormLibraryPayload.FormSlot> form) {
        if (form.isEmpty()) {
            return "Empty";
        }
        ImitatorFormLibraryPayload.FormSlot slot = form.get();
        String name = slot.displayName().isBlank() ? "Unknown form" : slot.displayName();
        return name.length() > 16 ? name.substring(0, 16) : name;
    }
}
