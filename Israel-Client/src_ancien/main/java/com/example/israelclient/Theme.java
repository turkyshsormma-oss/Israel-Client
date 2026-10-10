package com.example.israelclient;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Theme {
	public static final int DEFAULT_1 = 0xFF8B5CF6;
	public static final int DEFAULT_2 = 0xFF3B82F6;
	public static int c1 = DEFAULT_1;
	public static int c2 = DEFAULT_2;

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("israelclient-theme.json");
	private static final Gson GSON = new Gson();

	private Theme() {}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				int[] d = GSON.fromJson(Files.readString(FILE), int[].class);
				if (d != null && d.length == 2) {
					c1 = d[0] | 0xFF000000;
					c2 = d[1] | 0xFF000000;
				}
			}
		} catch (IOException | RuntimeException e) {
			System.err.println("[israel-client] theme illisible: " + e);
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(new int[] {c1, c2}));
		} catch (IOException e) {
			System.err.println("[israel-client] theme non sauvegarde: " + e);
		}
	}
}
