package com.mafuyu404.moveslikemafuyu.command;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.ModCapabilities;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.event.MaidTestMode;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class MovesLikeCommand {
    private static final DynamicCommandExceptionType UNKNOWN_ATTRIBUTE =
            new DynamicCommandExceptionType(attribute -> Component.literal("Unknown move attribute: " + attribute));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("moveslikecommand")
                        .requires(source -> source.hasPermission(2))
                        .then(attributeCommand("attribute"))
                        .then(testMaidCommand())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> testMaidCommand() {
        return Commands.literal("testmaid")
                .then(Commands.literal("on")
                        .executes(context -> setTestMaid(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setTestMaid(context.getSource(), false)));
    }

    private static int setTestMaid(CommandSourceStack source, boolean value) {
        MaidTestMode.setEnabled(source.getServer(), value);
        source.sendSuccess(() -> Component.literal("Maid dodge test mode " + (value ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attributeCommand(String name) {
        return Commands.literal(name)
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("attribute", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (MoveAttribute attribute : MoveAttribute.values()) {
                                        builder.suggest(attribute.key());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(context -> setMoveAttribute(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        parseAttribute(StringArgumentType.getString(context, "attribute")),
                                                        DoubleArgumentType.getDouble(context, "value")
                                                ))))
                                .then(Commands.literal("clear")
                                        .executes(context -> clearMoveAttribute(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                parseAttribute(StringArgumentType.getString(context, "attribute"))
                                        )))));
    }

    private static MoveAttribute parseAttribute(String key) throws CommandSyntaxException {
        try {
            return MoveAttribute.byKey(key);
        } catch (IllegalArgumentException exception) {
            throw UNKNOWN_ATTRIBUTE.create(key);
        }
    }

    private static int setMoveAttribute(CommandSourceStack source, Collection<ServerPlayer> players, MoveAttribute attribute, double value) {
        for (ServerPlayer player : players) {
            player.getCapability(ModCapabilities.PLAYER_MOVE_ATTRIBUTES).ifPresent(attributes -> attributes.set(attribute, value));
            ModCapabilities.syncToClient(player);
        }
        source.sendSuccess(() -> Component.literal("Set " + attribute.key() + " for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int clearMoveAttribute(CommandSourceStack source, Collection<ServerPlayer> players, MoveAttribute attribute) {
        for (ServerPlayer player : players) {
            player.getCapability(ModCapabilities.PLAYER_MOVE_ATTRIBUTES).ifPresent(attributes -> attributes.clear(attribute));
            ModCapabilities.syncToClient(player);
        }
        source.sendSuccess(() -> Component.literal("Cleared " + attribute.key() + " for " + players.size() + " player(s)."), true);
        return players.size();
    }
}
