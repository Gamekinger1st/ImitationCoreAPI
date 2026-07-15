package com.github.gamekinger1st.imitationcoreapi;

import com.github.gamekinger1st.imitationcoreapi.internal.server.ImitationCoreServerEvents;
import com.github.gamekinger1st.imitationcoreapi.internal.network.ImitationCoreNetwork;
import com.github.gamekinger1st.imitationcoreapi.internal.chat.PersonaChatServer;
import com.github.gamekinger1st.imitationcoreapi.internal.gecko.ReflectiveGeckoAnimationBridge;
import com.github.gamekinger1st.imitationcoreapi.internal.skill.ReflectiveManasTensuraSkillBridge;
import com.github.gamekinger1st.imitationcoreapi.internal.skill.OwnerSkillSuppressionCleanupAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.skill.TemporarySkillCleanupAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.skill.ImitatorSkillCopySnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.tensura.ReflectiveTensuraStateBridge;
import com.github.gamekinger1st.imitationcoreapi.internal.tensura.TensuraMirrorSyncApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.tensura.TensuraSnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.disguise.DefaultDisguisePresentationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.disguise.DefaultDisguiseAnimationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.disguise.GeckoDisguiseAnimationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.disguise.GeckoDisguisePresentationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.disguise.PlayerProfileSnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.disguise.DisguiseAppraisalSnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.targeting.TagBackedMobFactionResolver;
import com.github.gamekinger1st.imitationcoreapi.internal.targeting.VanillaMobFactionResolver;
import com.github.gamekinger1st.imitationcoreapi.internal.client.ClientDisguiseLifecycle;
import com.github.gamekinger1st.imitationcoreapi.internal.imitator.ImitatorPersonaChatProvider;
import com.github.gamekinger1st.imitationcoreapi.internal.imitator.CoreImitatorIntegration;
import com.github.gamekinger1st.imitationcoreapi.internal.imitator.DefaultImitatorFormAbilities;
import com.github.gamekinger1st.imitationcoreapi.internal.imitator.ImitatorFormProgressionApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.imitator.ImitatorSkillCopyPolicyCleanupAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.imitator.ImitatorAdminCommands;
import com.github.gamekinger1st.imitationcoreapi.internal.chat.DefaultChatChannels;
import com.github.gamekinger1st.imitationcoreapi.internal.chat.ImitationChatCommands;
import com.github.gamekinger1st.imitationcoreapi.internal.config.ImitationCoreConfig;
import com.github.gamekinger1st.imitationcoreapi.internal.physical.PhysicalFormApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.replica.ImitatorReplicaApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.internal.service.DefaultImitationCoreServiceProvider;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ImitationCoreApi.MOD_ID)
public final class ImitationCoreApi {
    public static final String MOD_ID = "imitationcoreapi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ImitationCoreApi(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ImitationCoreConfig.SPEC);
        ImitationCoreServices.initialize(new DefaultImitationCoreServiceProvider());
        ImitationCoreServerEvents.register();
        PersonaChatServer.register();
        ImitationChatCommands.register();
        ImitatorAdminCommands.register();
        ImitationApi.geckoAnimations().register(new ReflectiveGeckoAnimationBridge());
        ImitationApi.skillBridges().register(new ReflectiveManasTensuraSkillBridge());
        ImitationApi.tensuraStates().register(new ReflectiveTensuraStateBridge());
        ImitationApi.mobFactions().register(new VanillaMobFactionResolver());
        ImitationApi.mobFactions().register(new TagBackedMobFactionResolver());
        ImitationApi.adapters().register(new PlayerProfileSnapshotCaptureAdapter());
        ImitationApi.adapters().register(new DisguiseAppraisalSnapshotCaptureAdapter());
        ImitationApi.adapters().register(new TensuraSnapshotCaptureAdapter());
        ImitationApi.adapters().register(new ImitatorSkillCopySnapshotCaptureAdapter());
        ImitationApi.transformationApplications().register(new OwnerSkillSuppressionCleanupAdapter());
        ImitationApi.transformationApplications().register(new ImitatorFormProgressionApplicationAdapter());
        ImitationApi.transformationApplications().register(new ImitatorSkillCopyPolicyCleanupAdapter());
        ImitationApi.transformationApplications().register(new TemporarySkillCleanupAdapter());
        ImitationApi.transformationApplications().register(new PhysicalFormApplicationAdapter());
        ImitationApi.transformationApplications().register(new TensuraMirrorSyncApplicationAdapter());
        ImitationApi.transformationApplications().register(new ImitatorReplicaApplicationAdapter());
        ImitationApi.disguiseAnimations().register(new DefaultDisguiseAnimationAdapter());
        ImitationApi.disguiseAnimations().register(new GeckoDisguiseAnimationAdapter());
        ImitationApi.disguisePresentations().register(new GeckoDisguisePresentationAdapter());
        ImitationApi.disguisePresentations().register(new DefaultDisguisePresentationAdapter());
        if (FMLEnvironment.dist.isClient()) {
            ClientDisguiseLifecycle.register();
        }
        ImitationApi.personaChats().register(new ImitatorPersonaChatProvider());
        ImitationApi.imitatorIntegrations().register(new CoreImitatorIntegration());
        DefaultImitatorFormAbilities.create().forEach(ImitationApi.imitatorFormAbilities()::register);
        DefaultChatChannels.create().forEach(ImitationApi.chatChannels()::register);
        modEventBus.addListener(ImitationCoreNetwork::register);
    }
}
