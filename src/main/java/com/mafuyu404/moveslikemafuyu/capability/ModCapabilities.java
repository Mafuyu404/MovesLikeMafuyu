package com.mafuyu404.moveslikemafuyu.capability;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.network.MoveAttributesMessage;
import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class ModCapabilities {
    public static final Capability<PlayerMoveAttributes> PLAYER_MOVE_ATTRIBUTES =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation MOVE_ATTRIBUTES_ID =
            new ResourceLocation(MovesLikeMafuyu.MODID, "move_attributes");

    @Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerMoveAttributes.class);
        }
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(MOVE_ATTRIBUTES_ID, new MoveAttributesProvider());
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        LazyOptional<PlayerMoveAttributes> oldCap = event.getOriginal().getCapability(PLAYER_MOVE_ATTRIBUTES);
        LazyOptional<PlayerMoveAttributes> newCap = event.getEntity().getCapability(PLAYER_MOVE_ATTRIBUTES);
        oldCap.ifPresent(oldAttributes -> newCap.ifPresent(newAttributes -> newAttributes.copyFrom(oldAttributes)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void syncOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncToClient(event.getEntity());
    }

    @SubscribeEvent
    public static void syncOnRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncToClient(event.getEntity());
    }

    @SubscribeEvent
    public static void syncOnDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncToClient(event.getEntity());
    }

    public static void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        player.getCapability(PLAYER_MOVE_ATTRIBUTES).ifPresent(attributes ->
                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new MoveAttributesMessage(attributes.serializeNBT())
                )
        );
    }
}
