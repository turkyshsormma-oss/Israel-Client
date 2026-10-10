package com.example.namechanger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Stocke les surnoms (pseudo reel -> faux nom) dans config/namechanger.json. */
public final class NameStore {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("namechanger.json");
	private static final Gson GSON = new Gson();
	private static Map<String, String> names = new HashMap<>();

	private NameStore() {}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				Map<String, String> data = GSON.fromJson(Files.readString(FILE),
						new TypeToken<Map<String, String>>() {}.getType());
				if (data != null) names = new HashMap<>(data);
			}
		} catch (IOException | RuntimeException e) {
			System.err.println("[namechanger] lecture impossible: " + e);
		}
	}

	private static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(names));
		} catch (IOException e) {
			System.err.println("[namechanger] ecriture impossible: " + e);
		}
	}

	public static String get(String realName) {
		return names.get(realName.toLowerCase());
	}

	public static void set(String realName, String newName) {
		names.put(realName.toLowerCase(), newName);
		save();
	}

	public static boolean remove(String realName) {
		boolean removed = names.remove(realName.toLowerCase()) != null;
		if (removed) save();
		return removed;
	}

	public static Map<String, String> all() {
		return names;
	}
}
