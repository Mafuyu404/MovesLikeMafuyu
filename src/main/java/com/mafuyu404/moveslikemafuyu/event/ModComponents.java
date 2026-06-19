package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = MovesLikeMafuyu.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModComponents {
    private static final FoodProperties HEART_OF_THE_SEA_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1f)
            .alwaysEdible()
            .build();

    @SubscribeEvent
    public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(Items.HEART_OF_THE_SEA, builder -> builder.set(DataComponents.FOOD, HEART_OF_THE_SEA_FOOD));
    }
}
