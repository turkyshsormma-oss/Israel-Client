package com.example.israelclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Gfx {
	private Gfx() {}

	public static int lerp(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int ra = (a >> 16) & 255, ga = (a >> 8) & 255, ba = a & 255;
		int rb = (b >> 16) & 255, gb = (b >> 8) & 255, bb = b & 255;
		return 0xFF000000 | ((int) (ra + (rb - ra) * t) << 16) | ((int) (ga + (gb - ga) * t) << 8) | (int) (ba + (bb - ba) * t);
	}

	public static int shade(int c, float f) {
		int r = Math.min(255, (int) (((c >> 16) & 255) * f));
		int g = Math.min(255, (int) (((c >> 8) & 255) * f));
		int b = Math.min(255, (int) ((c & 255) * f));
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public static int hsvToRgb(float h, float s, float v) {
		float hh = (h - (float) Math.floor(h)) * 6f;
		int i = (int) hh;
		float f = hh - i;
		float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | ((int) (r * 255 + 0.5f) << 16) | ((int) (g * 255 + 0.5f) << 8) | (int) (b * 255 + 0.5f);
	}

	public static float[] rgbToHsv(int rgb) {
		float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		float d = max - min, h = 0;
		if (d > 0) {
			if (max == r) h = ((g - b) / d) % 6f;
			else if (max == g) h = (b - r) / d + 2f;
			else h = (r - g) / d + 4f;
			h /= 6f;
			if (h < 0) h += 1f;
		}
		return new float[] {h, max == 0 ? 0 : d / max, max};
	}

	private static int inset(int r, int d) {
		double dx = r - d - 0.5;
		return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dx * dx)));
	}

	/** Rectangle arrondi avec degrade vertical. */
	public static void rrect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		r = Math.min(r, Math.min(w, h) / 2);
		for (int dy = 0; dy < h; dy++) {
			int ins = 0;
			if (dy < r) ins = inset(r, dy);
			else if (dy >= h - r) ins = inset(r, h - 1 - dy);
			g.fill(x + ins, y + dy, x + w - ins, y + dy + 1, lerp(top, bottom, h <= 1 ? 0f : (float) dy / (h - 1)));
		}
	}

	/** Rectangle arrondi avec degrade horizontal. */
	public static void rrectH(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int c1, int c2) {
		if (w <= 0 || h <= 0) return;
		r = Math.min(r, Math.min(w, h) / 2);
		int i = 0;
		while (i < w) {
			int dx = Math.min(i, w - 1 - i);
			int ins = dx < r ? inset(r, dx) : 0;
			int cw = dx < r ? 1 : Math.max(1, Math.min(3, (w - r) - i));
			g.fill(x + i, y + ins, x + i + cw, y + h - ins, lerp(c1, c2, w <= 1 ? 0f : (float) i / (w - 1)));
			i += cw;
		}
	}
}
