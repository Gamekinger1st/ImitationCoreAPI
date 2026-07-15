package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.OptionalDouble;

public final class ClientDisguiseQueries {
    private ClientDisguiseQueries() {
    }

    public static Optional<ClientDisguiseState> state(Entity subject) {
        return ImitationApi.clientDisguiseStore().get(subject.getId());
    }

    public static Optional<DisguisePresentation> presentation(Entity subject) {
        return state(subject).map(state -> ImitationApi.disguisePresentations().resolve(subject, state));
    }

    public static OptionalDouble eyeHeight(Entity subject) {
        return presentation(subject).map(DisguisePresentation::eyeHeight).orElseGet(OptionalDouble::empty);
    }

    public static Optional<Component> appraisalName(Entity subject) {
        return state(subject).map(state -> Component.literal(state.displayName()));
    }

    public static Optional<DisguiseAppraisalSnapshot> appraisal(Entity subject) {
        return state(subject).flatMap(ClientDisguiseState::appraisal);
    }

    public static boolean replacesNameTag(Entity subject) {
        return presentation(subject).map(DisguisePresentation::replaceNameTag).orElse(false);
    }
}
