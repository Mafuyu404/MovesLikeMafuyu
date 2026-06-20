package com.mafuyu404.moveslikemafuyu.event;

import net.neoforged.fml.common.EventBusSubscriber;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = MovesLikeMafuyu.MODID)
public class ClientSetupEvent {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(KeyPrompts::init);
    }
}
