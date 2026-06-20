package com.mafuyu404.moveslikemafuyu.capability;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class ModCapabilities {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MovesLikeMafuyu.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerMoveAttributes>> PLAYER_MOVE_ATTRIBUTES =
            ATTACHMENT_TYPES.register("move_attributes", () ->
                    AttachmentType.serializable(PlayerMoveAttributes::new)
                            .copyOnDeath()
                            .build()
            );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static PlayerMoveAttributes get(Player player) {
        return player.getData(PLAYER_MOVE_ATTRIBUTES);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        get(event.getEntity()).copyFrom(get(event.getOriginal()));
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
        player.syncData(PLAYER_MOVE_ATTRIBUTES);
    }
}
