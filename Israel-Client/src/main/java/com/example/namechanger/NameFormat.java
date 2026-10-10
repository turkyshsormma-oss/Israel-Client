package com.example.namechanger;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Format des surnoms : codes Minecraft + couleur libre "§#RRGGBB". */
public final class NameFormat {
	private static final String CODES = "0123456789abcdef";
	private static final int[] RGB = {
		0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
		0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
	};

	private NameFormat() {}

	public static String hexPrefix(int rgb) {
		return "\u00a7#" + String.format("%06X", rgb & 0xFFFFFF);
	}

	public static char nearestCode(int rgb) {
		int best = 15;
		long bestD = Long.MAX_VALUE;
		int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
		for (int i = 0; i < 16; i++) {
			long dr = r - ((RGB[i] >> 16) & 255), dg = g - ((RGB[i] >> 8) & 255), db = b - (RGB[i] & 255);
			long d = dr * dr + dg * dg + db * db;
			if (d < bestD) { bestD = d; best = i; }
		}
		return CODES.charAt(best);
	}

	private static boolean isHex(String s, int from) {
		for (int k = 0; k < 6; k++) if (Character.digit(s.charAt(from + k), 16) < 0) return false;
		return true;
	}

	/** Pour les textes bruts : remplace chaque couleur libre par le code Minecraft le plus proche. */
	public static String toLegacy(String value) {
		if (value.indexOf("\u00a7#") < 0) return value;
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < value.length()) {
			if (i + 8 <= value.length() && value.charAt(i) == '\u00a7' && value.charAt(i + 1) == '#' && isHex(value, i + 2)) {
				sb.append('\u00a7').append(nearestCode(Integer.parseInt(value.substring(i + 2, i + 8), 16)));
				i += 8;
			} else {
				sb.append(value.charAt(i));
				i++;
			}
		}
		return sb.toString();
	}

	/** Pour les Components : couleurs exactes. */
	public static MutableComponent toComponent(String value, Style base) {
		MutableComponent out = Component.empty();
		Style style = base;
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < value.length()) {
			char c = value.charAt(i);
			if (c == '\u00a7' && i + 1 < value.length()) {
				char n = value.charAt(i + 1);
				if (n == '#' && i + 8 <= value.length() && isHex(value, i + 2)) {
					flush(out, sb, style);
					style = style.withColor(TextColor.fromRgb(Integer.parseInt(value.substring(i + 2, i + 8), 16)));
					i += 8;
					continue;
				}
				ChatFormatting f = ChatFormatting.getByCode(n);
				if (f != null) {
					flush(out, sb, style);
					style = f == ChatFormatting.RESET ? base : style.applyLegacyFormat(f);
					i += 2;
					continue;
				}
			}
			sb.append(c);
			i++;
		}
		flush(out, sb, style);
		return out;
	}

	private static void flush(MutableComponent out, StringBuilder sb, Style style) {
		if (sb.length() > 0) {
			out.append(Component.literal(sb.toString()).withStyle(style));
			sb.setLength(0);
		}
	}
}