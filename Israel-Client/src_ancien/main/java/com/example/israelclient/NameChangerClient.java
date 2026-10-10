package com.example.israelclient;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class NameChangerClient implements ClientModInitializer {
	private static void openGui() {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> mc.setScreen(new NameScreen()));
	}

	@Override
	public void onInitializeClient() {
		NameStore.load();
		Theme.load();

		KeyMapping.Category category = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("israelclient", "main"));
		KeyMapping openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.israelclient.open_gui", InputConstants.Type.KEYSYM, InputConstants.KEY_N, category));

		KeyMapping daggerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.israelclient.take_dagger", InputConstants.Type.KEYSYM, InputConstants.KEY_B, category));
		DaggerConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (daggerKey.consumeClick()) DaggerConfig.take(client);
			while (openKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new NameScreen());
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
			dispatcher.register(ClientCommands.literal("rename")
				.executes(ctx -> { openGui(); return 1; })
				.then(ClientCommands.literal("gui")
					.executes(ctx -> { openGui(); return 1; }))
				.then(ClientCommands.literal("set")
					.then(ClientCommands.argument("pseudo", StringArgumentType.word())
						.then(ClientCommands.argument("nom", StringArgumentType.greedyString())
							.executes(ctx -> {
								String pseudo = StringArgumentType.getString(ctx, "pseudo");
								String nom = StringArgumentType.getString(ctx, "nom").replace('&', '\u00a7');
								NameStore.set(pseudo, nom);
								ctx.getSource().sendFeedback(Component.literal(pseudo + " s'appelle maintenant " + nom));
								return 1;
							}))))
				.then(ClientCommands.literal("reset")
					.then(ClientCommands.argument("pseudo", StringArgumentType.word())
						.executes(ctx -> {
							String pseudo = StringArgumentType.getString(ctx, "pseudo");
							boolean ok = NameStore.remove(pseudo);
							ctx.getSource().sendFeedback(Component.literal(
								ok ? "Nom de " + pseudo + " reinitialise." : "Aucun surnom pour " + pseudo + "."));
							return ok ? 1 : 0;
						})))
				.then(ClientCommands.literal("list")
					.executes(ctx -> {
						if (NameStore.all().isEmpty()) {
							ctx.getSource().sendFeedback(Component.literal("Aucun surnom defini."));
						} else {
							NameStore.all().forEach((k, v) ->
								ctx.getSource().sendFeedback(Component.literal(k + " -> " + v)));
						}
						return 1;
					}))));
	}
}
