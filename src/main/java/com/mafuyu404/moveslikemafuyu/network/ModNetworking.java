package com.mafuyu404.moveslikemafuyu.network;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttributeResolver;
import com.mafuyu404.moveslikemafuyu.compat.TaczCompat;
import com.mafuyu404.moveslikemafuyu.network.payload.CrawlPayload;
import com.mafuyu404.moveslikemafuyu.network.payload.KnockPayload;
import com.mafuyu404.moveslikemafuyu.network.payload.MoveAttributesPayload;
import com.mafuyu404.moveslikemafuyu.network.payload.SwimmingBoostPayload;
import com.mafuyu404.moveslikemafuyu.network.payload.TagPayload;
import com.mafuyu404.moveslikemafuyu.util.PoseHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = MovesLikeMafuyu.MOD_ID)
public class ModNetworking {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MovesLikeMafuyu.MOD_ID);
        registrar.playToServer(CrawlPayload.TYPE, CrawlPayload.CODEC, ModNetworking::handleCrawl);
        registrar.playToServer(TagPayload.TYPE, TagPayload.CODEC, ModNetworking::handleTag);
        registrar.playToServer(KnockPayload.TYPE, KnockPayload.CODEC, ModNetworking::handleKnock);
        registrar.playToServer(SwimmingBoostPayload.TYPE, SwimmingBoostPayload.CODEC, ModNetworking::handleSwimmingBoost);
        registrar.playToClient(MoveAttributesPayload.TYPE, MoveAttributesPayload.CODEC, ModNetworking::handleMoveAttributes);
    }

    private static void handleCrawl(CrawlPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (payload.start()) {
            player.addTag("craw");
            PoseHelper.forcePose(player, Pose.SWIMMING);
            TaczCompat.syncCrawling(player, true);
        } else {
            player.removeTag("craw");
            PoseHelper.clearForcedPose(player);
            TaczCompat.syncCrawling(player, false);
        }
    }

    private static void handleTag(TagPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        String tag = payload.tag();
        if (payload.state()) {
            if (!player.getTags().contains(tag)) {
                player.addTag(tag);
            }
            if ("craw".equals(tag)) {
                PoseHelper.forcePose(player, Pose.SWIMMING);
                TaczCompat.syncCrawling(player, true);
            }
            if ("roll_shift".equals(tag)) {
                player.setShiftKeyDown(true);
            }
        } else {
            player.removeTag(tag);
            if ("craw".equals(tag)) {
                PoseHelper.clearForcedPose(player);
                TaczCompat.syncCrawling(player, false);
            }
            if ("roll_shift".equals(tag)) {
                player.setShiftKeyDown(false);
            }
        }
    }

    private static void handleKnock(KnockPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Level level = player.level();
        Vec3 playerMotion = player.getLookAngle();
        for (int id : payload.entityIds()) {
            Entity entity = level.getEntity(id);
            if (entity == null) continue;
            String entityId = entity.getType().builtInRegistryHolder().key().location().toString();
            if (Config.SLIDE_KNOCK_BLACKLIST.get().contains(entityId)) continue;
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.add(playerMotion.x, 0.7, playerMotion.z));
            entity.hurtMarked = true;
        }
        player.setDeltaMovement(playerMotion.add(-playerMotion.x, 0, -playerMotion.z));
        player.hurtMarked = true;
    }

    private static void handleSwimmingBoost(SwimmingBoostPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!Config.enable("SwimmingBoost") || !player.isSwimming()) return;
        int airCost = MoveAttributeResolver.getInt(player, MoveAttribute.SWIMMING_BOOST_AIR_COST);
        player.setAirSupply(player.getAirSupply() - airCost);
    }

    private static void handleMoveAttributes(MoveAttributesPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> handler = Class.forName("com.mafuyu404.moveslikemafuyu.client.ClientMoveAttributesHandler");
            handler.getMethod("handle", CompoundTag.class).invoke(null, payload.tag());
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
