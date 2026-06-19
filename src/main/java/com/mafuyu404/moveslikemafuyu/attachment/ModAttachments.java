package com.mafuyu404.moveslikemafuyu.attachment;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.PlayerMoveAttributes;
import com.mafuyu404.moveslikemafuyu.network.payload.MoveAttributesPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(modid = MovesLikeMafuyu.MOD_ID)
public class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MovesLikeMafuyu.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerMoveAttributes>> PLAYER_MOVE_ATTRIBUTES =
            ATTACHMENT_TYPES.register("move_attributes", () -> AttachmentType
                    .serializable(PlayerMoveAttributes::new)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
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
        PlayerMoveAttributes attributes = player.getData(PLAYER_MOVE_ATTRIBUTES);
        PacketDistributor.sendToPlayer(serverPlayer, new MoveAttributesPayload(attributes.write()));
    }
}
