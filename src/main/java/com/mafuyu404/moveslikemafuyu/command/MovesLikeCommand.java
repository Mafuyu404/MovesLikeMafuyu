package com.mafuyu404.moveslikemafuyu.command;

import com.mafuyu404.moveslikemafuyu.attachment.ModAttachments;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class MovesLikeCommand {
    private static final DynamicCommandExceptionType UNKNOWN_ATTRIBUTE =
            new DynamicCommandExceptionType(attribute -> Component.literal("Unknown move attribute: " + attribute));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("moveslikecommand")
                        .requires(source -> source.hasPermission(2))
                        .then(attributeCommand("attribute"))
        );
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
            player.getData(ModAttachments.PLAYER_MOVE_ATTRIBUTES).set(attribute, value);
            ModAttachments.syncToClient(player);
        }
        source.sendSuccess(() -> Component.literal("Set " + attribute.key() + " for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int clearMoveAttribute(CommandSourceStack source, Collection<ServerPlayer> players, MoveAttribute attribute) {
        for (ServerPlayer player : players) {
            player.getData(ModAttachments.PLAYER_MOVE_ATTRIBUTES).clear(attribute);
            ModAttachments.syncToClient(player);
        }
        source.sendSuccess(() -> Component.literal("Cleared " + attribute.key() + " for " + players.size() + " player(s)."), true);
        return players.size();
    }
}
