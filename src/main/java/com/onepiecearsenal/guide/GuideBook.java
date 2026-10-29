package com.onepiecearsenal.guide;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.onepiecearsenal.ArsenalItems;
import com.onepiecearsenal.ArsenalMod;

/**
 * The Shipwright's Manual: Arsenal's in-game guide to parts, materials, traits, stations, meito and the Marine
 * disguise. Right-click it to read it (the screen is client/GuideScreen; the chapters are GuideChapters).
 * - You get one the first time you join with Arsenal installed.
 * - Lost it? Type /arsenalguide, or craft one from a Book and Driftwood.
 */
public class GuideBook extends Item {
	/**
	 * Opens the book screen. The screen only exists on the client, so ArsenalClient fills this in when the game
	 * starts. On a dedicated server it stays empty and does nothing.
	 */
	public static Runnable openBook = () -> {};

	/** True once a player got their first manual, so they only get it once. */
	public static final AttachmentType<Boolean> GOT_GUIDE = AttachmentRegistry.create(
			ArsenalMod.id("got_guide"),
			builder -> builder.persistent(Codec.BOOL).copyOnDeath()
	);

	public GuideBook(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			openBook.run();
		}
		return InteractionResult.SUCCESS;
	}

	public static void initialize() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			if (!Boolean.TRUE.equals(player.getAttached(GOT_GUIDE))) {
				player.setAttached(GOT_GUIDE, true);
				give(player);
				player.sendSystemMessage(Component.literal("You got a Shipwright's Manual. Right-click it to learn how to build "
						+ "your own swords, guns and armor.").withStyle(ChatFormatting.GOLD));
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("arsenalguide").executes(GuideBook::newBook)));
	}

	/** /arsenalguide: a new manual, if you lost yours. */
	private static int newBook(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (player.getInventory().countItem(ArsenalItems.GUIDE) > 0) {
			context.getSource().sendFailure(Component.literal("You already have a Shipwright's Manual. Right-click it to read it."));
			return 0;
		}
		give(player);
		return 1;
	}

	private static void give(ServerPlayer player) {
		ItemStack stack = new ItemStack(ArsenalItems.GUIDE);
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false, Prediction.SERVER_ONLY); // Inventory full: drop it at their feet.
		}
	}
}
