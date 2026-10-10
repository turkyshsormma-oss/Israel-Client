package com.example.namechanger;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Locale;
import java.util.Optional;

public final class ChatRewriter {
	private ChatRewriter() {}

	public static Component rewrite(Component message) {
		if (NameStore.all().isEmpty()) return message;

		String plain = message.getString().toLowerCase(Locale.ROOT);
		boolean hit = false;
		for (String k : NameStore.all().keySet()) {
			if (plain.contains(k)) { hit = true; break; }
		}
		if (!hit) return message;

		MutableComponent out = Component.empty();
		message.visit((style, text) -> {
			appendReplaced(out, text, style);
			return Optional.empty();
		}, Style.EMPTY);
		return out;
	}

	private static void appendReplaced(MutableComponent out, String text, Style style) {
		String lower = text.toLowerCase(Locale.ROOT);
		int pos = 0;
		while (pos < text.length()) {
			int best = -1;
			String bestKey = null;
			for (String k : NameStore.all().keySet()) {
				int i = lower.indexOf(k, pos);
				if (i >= 0 && (best < 0 || i < best || (i == best && k.length() > bestKey.length()))) {
					best = i;
					bestKey = k;
				}
			}
			if (best < 0) {
				out.append(Component.literal(text.substring(pos)).withStyle(style));
				return;
			}
			if (best > pos) out.append(Component.literal(text.substring(pos, best)).withStyle(style));
			out.append(NameFormat.toComponent(NameStore.all().get(bestKey), style));
			pos = best + bestKey.length();
		}
	}
}