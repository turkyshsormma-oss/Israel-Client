package com.example.israelclient;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Regles "texte dans le nom d'une entite proche -> dague a prendre". */
public final class DaggerConfig {
	public static final class Rule {
		public String trigger = "";
		public String dagger = "";

		public Rule() {}

		public Rule(String trigger, String dagger) {
			this.trigger = trigger;
			this.dagger = dagger;
		}
	}

	private static final class Data {
		boolean auto = true;
		List<Rule> rules = new ArrayList<>();
	}

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("israelclient-dagger-rules.json");
	private static final Gson GSON = new Gson();
	private static Data data = new Data();
	private static boolean registered = false;
	private static int counter = 0;
	private static String lastTrigger = null;

	private DaggerConfig() {}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				Data d = GSON.fromJson(Files.readString(FILE), Data.class);
				if (d != null) {
					data = d;
					if (data.rules == null) data.rules = new ArrayList<>();
				}
			}
		} catch (IOException | RuntimeException e) {
			System.err.println("[israel-client] regles dague illisibles: " + e);
		}
		if (!registered) {
			registered = true;
			ClientTickEvents.END_CLIENT_TICK.register(DaggerConfig::tick);
		}
	}

	private static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(data));
		} catch (IOException e) {
			System.err.println("[israel-client] regles dague non sauvegardees: " + e);
		}
	}

	public static boolean isAuto() {
		return data.auto;
	}

	public static void setAuto(boolean value) {
		data.auto = value;
		lastTrigger = null;
		save();
	}

	public static List<Rule> rules() {
		return data.rules;
	}

	public static void addRule(String trigger, String dagger) {
		data.rules.add(new Rule(trigger, dagger));
		save();
	}

	public static void removeRule(int index) {
		if (index >= 0 && index < data.rules.size()) {
			data.rules.remove(index);
			lastTrigger = null;
			save();
		}
	}

	/** Cherche la premiere regle dont le texte apparait dans le nom d'une entite proche. */
	private static Rule detect(Minecraft mc) {
		if (mc.player == null || mc.level == null) return null;
		List<Entity> near = mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(48.0));
		for (Rule r : data.rules) {
			if (r.trigger == null || r.trigger.isBlank() || r.dagger == null || r.dagger.isBlank()) continue;
			String wanted = r.trigger.trim().toLowerCase(Locale.ROOT);
			for (Entity e : near) {
				if (e == mc.player) continue;
				Component name = e.getCustomName();
				if (name != null && name.getString().toLowerCase(Locale.ROOT).contains(wanted)) {
					return r;
				}
			}
		}
		return null;
	}

	private static boolean select(Minecraft mc, String daggerName) {
		Inventory inv = mc.player.getInventory();
		String wanted = daggerName.trim().toLowerCase(Locale.ROOT);
		for (int i = 0; i < 9; i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()
					&& stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(wanted)) {
				inv.setSelectedSlot(i);
				return true;
			}
		}
		message(mc, "Dague introuvable dans la barre : " + daggerName.trim());
		return false;
	}

	/** A la demande : touche ou bouton. */
	public static boolean take(Minecraft mc) {
		if (mc == null || mc.player == null) return false;
		Rule r = detect(mc);
		if (r == null) {
			message(mc, "Aucun attunement detecte autour de toi.");
			return false;
		}
		return select(mc, r.dagger);
	}

	/** Mode automatique : une fois a chaque changement de texte detecte. */
	private static void tick(Minecraft mc) {
		if (!data.auto || mc.player == null || mc.level == null || mc.screen != null) return;
		if (++counter % 5 != 0) return;
		Rule r = detect(mc);
		if (r == null) {
			lastTrigger = null;
			return;
		}
		if (r.trigger.equals(lastTrigger)) return;
		lastTrigger = r.trigger;
		select(mc, r.dagger);
	}

	private static void message(Minecraft mc, String text) {
		System.out.println("[israel-client] " + text);
	}
}
