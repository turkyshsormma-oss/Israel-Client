package com.example.namechanger;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NameScreen extends Screen {
	private static final String[] TABS = {"Surnoms", "Theme", "Dague", "Aide"};
	private static final int[] QUICK = {
		0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
		0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
	};
	private static final int[][] PRESETS = {
		{0x8B5CF6, 0x3B82F6}, {0xEC4899, 0xF59E0B}, {0x10B981, 0x06B6D4},
		{0xEF4444, 0x8B5CF6}, {0x3B82F6, 0x22D3EE}, {0xF472B6, 0xA78BFA}
	};
	private static final int R = 44;
	private static final int[] CELL_X;
	private static final int[] CELL_Y;
	private static final float[] CELL_H;
	private static final float[] CELL_S;

	static {
		List<int[]> pos = new ArrayList<>();
		List<float[]> hs = new ArrayList<>();
		for (int oy = -R; oy < R; oy += 2) {
			for (int ox = -R; ox < R; ox += 2) {
				double cx = ox + 1, cy = oy + 1;
				double d = Math.sqrt(cx * cx + cy * cy);
				if (d <= R) {
					pos.add(new int[] {ox, oy});
					hs.add(new float[] {(float) ((Math.atan2(cy, cx) / (2 * Math.PI) + 1.0) % 1.0), (float) (d / R)});
				}
			}
		}
		CELL_X = new int[pos.size()];
		CELL_Y = new int[pos.size()];
		CELL_H = new float[pos.size()];
		CELL_S = new float[pos.size()];
		for (int i = 0; i < pos.size(); i++) {
			CELL_X[i] = pos.get(i)[0];
			CELL_Y[i] = pos.get(i)[1];
			CELL_H[i] = hs.get(i)[0];
			CELL_S[i] = hs.get(i)[1];
		}
	}

	private static int tab = 0;

	private record Btn(int x, int y, int w, int h, String label, int style) {}

	private final List<AbstractWidget> drawables = new ArrayList<>();
	private final List<Btn> visuals = new ArrayList<>();
	private final List<Map.Entry<String, String>> rows = new ArrayList<>();
	private EditBox pseudoBox;
	private EditBox nameBox;
	private EditBox hexBox;
	private String pseudoText = "";
	private String nameText = "";
	private int selectedRgb = -1;
	private int page = 0;
	private int total = 0;
	private int pages = 1;

	// selecteur de couleur
	private boolean pickerOpen = false;
	private int pickerTarget = 0; // 0 surnom, 1 theme couleur 1, 2 theme couleur 2
	private int pickerBackup = 0;
	private float pHue = 0, pSat = 0, pVal = 1;
	private boolean settingHex = false;
	private int dragTarget = 0; // 0 rien, 1 roue, 2 luminosite
	private int cardX, cardY, cardW, cardH, wcx, wcy, sliderX, sliderY, sliderW, rx, rw;

	// mise en page
	private int px, py, pw, ph;
	private int sbX, sbY, sbW;
	private int ctX, ctY, ctW, ctH;
	private int cardAH, fieldY, fieldW, fx1, fx2, swY, swSize, previewY;
	private int listY, listH, rowsTop;
	private int themeY;

	public NameScreen() {
		super(Component.literal("Name Changer"));
	}

	// ---------- couleurs du theme ----------

	private int edge() { return Gfx.shade(Gfx.lerp(Theme.c1, Theme.c2, 0.5f), 0.7f); }
	private int cardTop() { return Gfx.shade(Theme.c1, 0.26f); }
	private int cardBot() { return Gfx.shade(Theme.c2, 0.22f); }

	private void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int r) {
		Gfx.rrect(g, x, y, w, h, r, edge(), edge());
		Gfx.rrect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), cardTop(), cardBot());
	}

	private void card(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
		box(g, x, y, w, h, 6);
		g.text(this.font, title, x + (w - this.font.width(title)) / 2, y + 7, 0xFFFFFFFF, false);
		Gfx.rrectH(g, x + 12, y + 19, w - 24, 1, 0, Theme.c1, Theme.c2);
	}

	private void fieldBg(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean focused) {
		Gfx.rrect(g, x, y, w, h, 4, focused ? Theme.c1 : edge(), focused ? Theme.c2 : edge());
		Gfx.rrect(g, x + 1, y + 1, w - 2, h - 2, 3, Gfx.shade(Theme.c1, 0.09f), Gfx.shade(Theme.c2, 0.09f));
	}

	private void drawBtn(GuiGraphicsExtractor g, Btn b, int mx, int my) {
		boolean hover = mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h();
		switch (b.style()) {
			case 1 -> Gfx.rrectH(g, b.x(), b.y(), b.w(), b.h(), 5,
				hover ? 0xFFF87171 : 0xFFEF4444, hover ? 0xFFDC2626 : 0xFFB91C1C);
			case 2 -> Gfx.rrect(g, b.x(), b.y(), b.w(), b.h(), 5,
				Gfx.shade(Theme.c1, hover ? 0.50f : 0.36f), Gfx.shade(Theme.c2, hover ? 0.44f : 0.30f));
			default -> Gfx.rrectH(g, b.x(), b.y(), b.w(), b.h(), 5,
				Gfx.lerp(Theme.c1, 0xFFFFFFFF, hover ? 0.25f : 0f), Gfx.lerp(Theme.c2, 0xFFFFFFFF, hover ? 0.25f : 0f));
		}
		g.text(this.font, b.label(), b.x() + (b.w() - this.font.width(b.label())) / 2,
			b.y() + (b.h() - 8) / 2, 0xFFFFFFFF, false);
	}

	// ---------- utilitaires ----------

	private <T extends AbstractWidget> T add(T widget) {
		addWidget(widget);
		drawables.add(widget);
		return widget;
	}

	private void btn(int x, int y, int w, int h, String label, int style, Runnable action) {
		addWidget(Button.builder(Component.empty(), b -> action.run()).bounds(x, y, w, h).build());
		visuals.add(new Btn(x, y, w, h, label, style));
	}

	private void hiddenBtn(int x, int y, int w, int h, Runnable action) {
		addWidget(Button.builder(Component.empty(), b -> action.run()).bounds(x, y, w, h).build());
	}

	private void keepText() {
		if (pseudoBox != null) pseudoText = pseudoBox.getValue();
		if (nameBox != null) nameText = nameBox.getValue();
	}

	private static String hex(int rgb) {
		return String.format("#%06X", rgb & 0xFFFFFF);
	}

	private static String shown(String key) {
		if (key.length() < 2) return "\u00a7b" + key;
		return "\u00a7b" + key.charAt(0) + "\u00a7b" + key.substring(1);
	}

	// ---------- construction ----------

	@Override
	protected void init() {
		drawables.clear();
		visuals.clear();
		rows.clear();
		pseudoBox = null;
		nameBox = null;
		hexBox = null;

		pw = Math.min(this.width - 20, 460);
		ph = Math.min(this.height - 20, 300);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		sbW = 100;
		sbX = px + 8;
		sbY = py + 30;
		ctX = px + sbW + 16;
		ctY = py + 30;
		ctW = pw - sbW - 24;
		ctH = ph - 38;

		if (pickerOpen) {
			initPicker();
			return;
		}

		for (int i = 0; i < TABS.length; i++) {
			final int index = i;
			hiddenBtn(sbX + 5, sbY + 30 + i * 22, sbW - 10, 19, () -> {
				keepText();
				tab = index;
				page = 0;
				rebuildWidgets();
			});
		}

		btn(sbX + 8, sbY + ctH - 28, sbW - 16, 20, "Fermer", 2, this::onClose);

		if (tab == 0) initNicknames();
		else if (tab == 1) initTheme();
		else if (tab == 2) initDagger();
	}

	private void initNicknames() {
		int availW = ctW - 20;
		int btnW = 64;
		fieldW = (availW - btnW - 8) / 2;
		fieldY = ctY + 26;
		fx1 = ctX + 10;
		fx2 = ctX + 14 + fieldW;

		pseudoBox = new EditBox(this.font, fx1 + 6, fieldY + 7, fieldW - 12, 10, Component.literal("Pseudo"));
		pseudoBox.setBordered(false);
		pseudoBox.setHint(Component.literal("Pseudo reel"));
		pseudoBox.setMaxLength(16);
		pseudoBox.setValue(pseudoText);
		add(pseudoBox);

		nameBox = new EditBox(this.font, fx2 + 6, fieldY + 7, fieldW - 12, 10, Component.literal("Nom"));
		nameBox.setBordered(false);
		nameBox.setHint(Component.literal("Nouveau nom"));
		nameBox.setMaxLength(48);
		nameBox.setValue(nameText);
		add(nameBox);

		btn(ctX + ctW - 10 - btnW, fieldY, btnW, 22, "Ajouter", 0, () -> {
			String pseudo = pseudoBox.getValue().trim();
			String name = nameBox.getValue().trim().replace('&', '\u00a7');
			if (!pseudo.isEmpty() && !name.isEmpty()) {
				if (selectedRgb >= 0) name = NameFormat.hexPrefix(selectedRgb) + name;
				NameStore.set(pseudo, name);
				pseudoText = "";
				nameText = "";
				rebuildWidgets();
			}
		});

		swSize = Math.max(8, Math.min(16, (availW - 15 * 3) / 16));
		swY = fieldY + 22 + 8;
		for (int i = 0; i < 16; i++) {
			final int rgb = QUICK[i];
			hiddenBtn(ctX + 10 + i * (swSize + 3), swY, swSize, swSize,
				() -> selectedRgb = selectedRgb == rgb ? -1 : rgb);
		}

		previewY = swY + swSize + 10;
		btn(ctX + ctW - 10 - 100, previewY - 4, 100, 16, "Autre couleur", 2, () -> openPicker(0));
		cardAH = previewY + 18 + 6 - ctY;

		listY = ctY + cardAH + 8;
		listH = ctH - cardAH - 8;
		rowsTop = listY + 26;
		int perPage = Math.max(1, (listH - 26 - 30) / 24);

		List<Map.Entry<String, String>> all = new ArrayList<>(new TreeMap<>(NameStore.all()).entrySet());
		total = all.size();
		pages = Math.max(1, (total + perPage - 1) / perPage);
		page = Math.min(page, pages - 1);

		int from = page * perPage;
		int to = Math.min(total, from + perPage);
		for (int i = from; i < to; i++) {
			final String key = all.get(i).getKey();
			final String value = all.get(i).getValue();
			int y = rowsTop + (i - from) * 24;
			rows.add(all.get(i));

			btn(ctX + ctW - 10 - 22 - 4 - 64, y, 64, 20, "Modifier", 2, () -> {
				pseudoText = key;
				String v = value;
				selectedRgb = -1;
				if (v.length() >= 8 && v.startsWith("\u00a7#")) {
					try {
						selectedRgb = Integer.parseInt(v.substring(2, 8), 16);
						v = v.substring(8);
					} catch (NumberFormatException ignored) {
					}
				} else if (v.length() >= 2 && v.charAt(0) == '\u00a7') {
					int idx = "0123456789abcdef".indexOf(v.charAt(1));
					if (idx >= 0) {
						selectedRgb = QUICK[idx];
						v = v.substring(2);
					}
				}
				nameText = v.replace('\u00a7', '&');
				rebuildWidgets();
			});

			btn(ctX + ctW - 10 - 22, y, 22, 20, "X", 1, () -> {
				keepText();
				NameStore.remove(key);
				rebuildWidgets();
			});
		}

		int navY = listY + listH - 26;
		btn(ctX + 10, navY, 24, 18, "<", 2, () -> {
			if (page > 0) { keepText(); page--; rebuildWidgets(); }
		});
		btn(ctX + ctW - 34, navY, 24, 18, ">", 2, () -> {
			if (page < pages - 1) { keepText(); page++; rebuildWidgets(); }
		});
	}

	private EditBox trigBox;
	private EditBox dagBox;
	private String dagTrigText = "";
	private String dagNameText = "";
	private int dagFieldW, dagFieldY, dagListY, dagMaxRows;

	private void keepDagger() {
		if (trigBox != null) dagTrigText = trigBox.getValue();
		if (dagBox != null) dagNameText = dagBox.getValue();
	}

	private void initDagger() {
		trigBox = null;
		dagBox = null;

		btn(ctX + 10, ctY + 28, 110, 20, "Auto : " + (DaggerConfig.isAuto() ? "OUI" : "NON"),
			DaggerConfig.isAuto() ? 0 : 2, () -> {
				keepDagger();
				DaggerConfig.setAuto(!DaggerConfig.isAuto());
				rebuildWidgets();
			});
		btn(ctX + ctW - 10 - 150, ctY + 28, 150, 20, "Prendre maintenant", 2,
			() -> DaggerConfig.take(this.minecraft));

		int btnW = 64;
		dagFieldW = (ctW - 20 - btnW - 8) / 2;
		dagFieldY = ctY + 58;

		trigBox = new EditBox(this.font, ctX + 16, dagFieldY + 7, dagFieldW - 12, 10, Component.literal("Texte"));
		trigBox.setBordered(false);
		trigBox.setHint(Component.literal("Texte detecte"));
		trigBox.setMaxLength(40);
		trigBox.setValue(dagTrigText);
		add(trigBox);

		dagBox = new EditBox(this.font, ctX + 20 + dagFieldW, dagFieldY + 7, dagFieldW - 12, 10, Component.literal("Dague"));
		dagBox.setBordered(false);
		dagBox.setHint(Component.literal("Nom de la dague"));
		dagBox.setMaxLength(40);
		dagBox.setValue(dagNameText);
		add(dagBox);

		dagListY = ctY + 92;
		dagMaxRows = Math.max(1, Math.min(5, (ctH - 92 - 40) / 24));

		btn(ctX + ctW - 10 - btnW, dagFieldY, btnW, 22, "Ajouter", 0, () -> {
			keepDagger();
			String tr = dagTrigText.trim();
			String dg = dagNameText.trim();
			if (!tr.isEmpty() && !dg.isEmpty() && DaggerConfig.rules().size() < dagMaxRows) {
				DaggerConfig.addRule(tr, dg);
				dagTrigText = "";
				dagNameText = "";
			}
			rebuildWidgets();
		});

		List<DaggerConfig.Rule> rules = DaggerConfig.rules();
		for (int i = 0; i < Math.min(rules.size(), dagMaxRows); i++) {
			final int index = i;
			btn(ctX + ctW - 10 - 22, dagListY + i * 24, 22, 20, "X", 1, () -> {
				keepDagger();
				DaggerConfig.removeRule(index);
				rebuildWidgets();
			});
		}
	}

	private void drawDagger(GuiGraphicsExtractor g) {
		card(g, ctX, ctY, ctW, ctH, "Dague du Blaze Slayer");
		fieldBg(g, ctX + 10, dagFieldY, dagFieldW, 22, trigBox != null && trigBox.isFocused());
		fieldBg(g, ctX + 14 + dagFieldW, dagFieldY, dagFieldW, 22, dagBox != null && dagBox.isFocused());

		List<DaggerConfig.Rule> rules = DaggerConfig.rules();
		if (rules.isEmpty()) {
			String none = "Aucune regle. Ajoute : texte du nom -> dague.";
			g.text(this.font, none, ctX + (ctW - this.font.width(none)) / 2, dagListY + 6, 0xFFA29BCB, false);
		}

		int maxW = ctW - 20 - 22 - 12;
		for (int i = 0; i < Math.min(rules.size(), dagMaxRows); i++) {
			int y = dagListY + i * 24;
			Gfx.rrect(g, ctX + 8, y - 2, ctW - 16, 24, 5, Gfx.shade(Theme.c1, 0.34f), Gfx.shade(Theme.c2, 0.30f));
			DaggerConfig.Rule r = rules.get(i);
			String line = "\u00a7b" + r.trigger + " \u00a77->\u00a7r " + r.dagger;
			g.enableScissor(ctX + 14, y, ctX + 14 + maxW, y + 20);
			g.text(this.font, line, ctX + 14, y + 6, 0xFFFFFFFF, false);
			g.disableScissor();
		}

		int hy = ctY + ctH - 36;
		g.text(this.font, "Auto : si le nom d'une entite proche contient le texte,", ctX + 12, hy, 0xFFDDD8F8, false);
		g.text(this.font, "la dague est prise une fois par changement.", ctX + 12, hy + 11, 0xFFDDD8F8, false);
		g.text(this.font, "Touche B : pareil, a la demande.", ctX + 12, hy + 22, 0xFFDDD8F8, false);
	}

	private void initTheme() {
		themeY = ctY + 30;
		btn(ctX + ctW - 10 - 64, themeY, 64, 20, "Modifier", 2, () -> openPicker(1));
		btn(ctX + ctW - 10 - 64, themeY + 28, 64, 20, "Modifier", 2, () -> openPicker(2));

		int chipW = (ctW - 20 - 5 * 6) / 6;
		for (int i = 0; i < PRESETS.length; i++) {
			final int[] p = PRESETS[i];
			hiddenBtn(ctX + 10 + i * (chipW + 6), themeY + 108, chipW, 22, () -> {
				Theme.c1 = 0xFF000000 | p[0];
				Theme.c2 = 0xFF000000 | p[1];
				Theme.save();
			});
		}

		btn(ctX + 10, themeY + 142, 110, 20, "Reinitialiser", 2, () -> {
			Theme.c1 = Theme.DEFAULT_1;
			Theme.c2 = Theme.DEFAULT_2;
			Theme.save();
		});
	}

	// ---------- selecteur de couleur ----------

	private void openPicker(int target) {
		keepText();
		pickerTarget = target;
		int cur = target == 0 ? (selectedRgb >= 0 ? selectedRgb : 0xFFFFFF) : (target == 1 ? Theme.c1 : Theme.c2);
		pickerBackup = cur;
		float[] hsv = Gfx.rgbToHsv(cur);
		pHue = hsv[0];
		pSat = hsv[1];
		pVal = hsv[2];
		pickerOpen = true;
		dragTarget = 0;
		rebuildWidgets();
	}

	private void closePicker() {
		pickerOpen = false;
		dragTarget = 0;
		rebuildWidgets();
	}

	private void initPicker() {
		cardW = 262;
		cardH = 196;
		cardX = (this.width - cardW) / 2;
		cardY = (this.height - cardH) / 2;
		wcx = cardX + 16 + R;
		wcy = cardY + 34 + R;
		sliderX = cardX + 16;
		sliderW = 2 * R;
		sliderY = wcy + R + 12;
		rx = cardX + 16 + 2 * R + 18;
		rw = cardX + cardW - 16 - rx;

		hexBox = new EditBox(this.font, rx + 6, cardY + 34 + 28 + 10 + 7, rw - 12, 10, Component.literal("Hex"));
		hexBox.setBordered(false);
		hexBox.setMaxLength(7);
		settingHex = true;
		hexBox.setValue(hex(Gfx.hsvToRgb(pHue, pSat, pVal)));
		settingHex = false;
		hexBox.setResponder(s -> {
			if (settingHex) return;
			String t = s.startsWith("#") ? s.substring(1) : s;
			if (t.length() == 6) {
				try {
					int rgb = Integer.parseInt(t, 16);
					float[] hsv = Gfx.rgbToHsv(rgb);
					pHue = hsv[0];
					pSat = hsv[1];
					pVal = hsv[2];
					applyTheme();
				} catch (NumberFormatException ignored) {
				}
			}
		});
		add(hexBox);

		if (pickerTarget == 0) {
			btn(rx, cardY + cardH - 88, rw, 20, "Aucune couleur", 2, () -> {
				selectedRgb = -1;
				closePicker();
			});
		}
		btn(rx, cardY + cardH - 64, rw, 20, "OK", 0, () -> {
			int rgb = Gfx.hsvToRgb(pHue, pSat, pVal);
			if (pickerTarget == 0) selectedRgb = rgb & 0xFFFFFF;
			else Theme.save();
			closePicker();
		});
		btn(rx, cardY + cardH - 40, rw, 20, "Annuler", 2, () -> {
			if (pickerTarget == 1) Theme.c1 = pickerBackup | 0xFF000000;
			else if (pickerTarget == 2) Theme.c2 = pickerBackup | 0xFF000000;
			closePicker();
		});
	}

	private void applyTheme() {
		int rgb = Gfx.hsvToRgb(pHue, pSat, pVal);
		if (pickerTarget == 1) Theme.c1 = rgb;
		else if (pickerTarget == 2) Theme.c2 = rgb;
	}

	private void applyAndShowHex() {
		applyTheme();
		if (hexBox != null) {
			settingHex = true;
			hexBox.setValue(hex(Gfx.hsvToRgb(pHue, pSat, pVal)));
			settingHex = false;
		}
	}

	// ---------- souris (evenements de Minecraft) ----------

	private boolean startDrag(double mx, double my) {
		double dx = mx - wcx, dy = my - wcy;
		if (Math.sqrt(dx * dx + dy * dy) <= R + 2) {
			dragTarget = 1;
		} else if (mx >= sliderX - 4 && mx <= sliderX + sliderW + 4 && my >= sliderY - 4 && my <= sliderY + 16) {
			dragTarget = 2;
		} else {
			dragTarget = 0;
			return false;
		}
		updateDrag(mx, my);
		return true;
	}

	private void updateDrag(double mx, double my) {
		if (dragTarget == 1) {
			double dx = mx - wcx, dy = my - wcy;
			double d = Math.min(Math.sqrt(dx * dx + dy * dy), R);
			pHue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1.0) % 1.0);
			pSat = (float) (d / R);
			applyAndShowHex();
		} else if (dragTarget == 2) {
			pVal = (float) Math.max(0.0, Math.min(1.0, (mx - sliderX) / (double) sliderW));
			applyAndShowHex();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (pickerOpen && event.button() == 0 && startDrag(event.x(), event.y())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (pickerOpen && dragTarget != 0) {
			updateDrag(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragTarget = 0;
		return super.mouseReleased(event);
	}

	private void drawPicker(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.fill(0, 0, this.width, this.height, 0x99000000);
		box(g, cardX, cardY, cardW, cardH, 8);
		String t = pickerTarget == 0 ? "Couleur du surnom" : (pickerTarget == 1 ? "Couleur 1 de l'interface" : "Couleur 2 de l'interface");
		g.text(this.font, t, cardX + (cardW - this.font.width(t)) / 2, cardY + 9, 0xFFFFFFFF, false);
		Gfx.rrectH(g, cardX + 12, cardY + 21, cardW - 24, 1, 0, Theme.c1, Theme.c2);

		// roue
		for (int i = 0; i < CELL_X.length; i++) {
			g.fill(wcx + CELL_X[i], wcy + CELL_Y[i], wcx + CELL_X[i] + 2, wcy + CELL_Y[i] + 2,
				Gfx.hsvToRgb(CELL_H[i], CELL_S[i], pVal));
		}
		double ang = pHue * 2 * Math.PI;
		int mxp = wcx + (int) Math.round(Math.cos(ang) * pSat * R);
		int myp = wcy + (int) Math.round(Math.sin(ang) * pSat * R);
		g.outline(mxp - 3, myp - 3, 7, 7, 0xFF000000);
		g.outline(mxp - 2, myp - 2, 5, 5, 0xFFFFFFFF);

		// luminosite
		Gfx.rrectH(g, sliderX, sliderY, sliderW, 12, 5, 0xFF000000, Gfx.hsvToRgb(pHue, pSat, 1f));
		int kx = sliderX + Math.round(pVal * sliderW);
		Gfx.rrect(g, kx - 2, sliderY - 2, 5, 16, 2, 0xFFFFFFFF, 0xFFFFFFFF);

		// apercu + hex
		int cur = Gfx.hsvToRgb(pHue, pSat, pVal);
		Gfx.rrect(g, rx, cardY + 34, rw, 28, 5, edge(), edge());
		Gfx.rrect(g, rx + 1, cardY + 35, rw - 2, 26, 4, cur, cur);
		fieldBg(g, rx, cardY + 34 + 28 + 10, rw, 22, hexBox != null && hexBox.isFocused());

		for (Btn b : visuals) drawBtn(g, b, mouseX, mouseY);
		for (AbstractWidget w : drawables) w.extractRenderState(g, mouseX, mouseY, 0f);
	}

	// ---------- dessin ----------

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, 0x99000000);

		// panneau principal : bordure en degrade + fond
		Gfx.rrect(g, px, py, pw, ph, 9, Theme.c1, Theme.c2);
		Gfx.rrect(g, px + 1, py + 1, pw - 2, ph - 2, 8, Gfx.shade(Theme.c1, 0.17f), Gfx.shade(Theme.c2, 0.13f));

		String title = "\u00a7lName Changer";
		g.text(this.font, title, px + (pw - this.font.width(title)) / 2, py + 10, 0xFFFFFFFF, false);
		Gfx.rrectH(g, px + 16, py + 23, pw - 32, 2, 1, Theme.c1, Theme.c2);

		// barre laterale
		box(g, sbX, sbY, sbW, ctH, 6);
		String cat = "Categories";
		g.text(this.font, cat, sbX + (sbW - this.font.width(cat)) / 2, sbY + 12, Gfx.lerp(Theme.c1, 0xFFFFFFFF, 0.5f), false);

		for (int i = 0; i < TABS.length; i++) {
			int ty = sbY + 30 + i * 22;
			boolean hover = !pickerOpen && mouseX >= sbX + 5 && mouseX < sbX + sbW - 5 && mouseY >= ty && mouseY < ty + 19;
			if (i == tab) Gfx.rrectH(g, sbX + 5, ty, sbW - 10, 19, 6, Theme.c1, Theme.c2);
			else if (hover) Gfx.rrect(g, sbX + 5, ty, sbW - 10, 19, 6, Gfx.shade(Theme.c1, 0.5f), Gfx.shade(Theme.c2, 0.45f));
			g.text(this.font, TABS[i], sbX + (sbW - this.font.width(TABS[i])) / 2, ty + 5, 0xFFFFFFFF, false);
		}

		if (pickerOpen) {
			drawPicker(g, mouseX, mouseY);
			return;
		}

		if (tab == 0) drawNicknames(g, mouseX, mouseY);
		else if (tab == 1) drawTheme(g, mouseX, mouseY);
		else if (tab == 2) drawDagger(g);
		else drawHelp(g);

		for (Btn b : visuals) drawBtn(g, b, mouseX, mouseY);
		for (AbstractWidget w : drawables) w.extractRenderState(g, mouseX, mouseY, delta);
	}

	private void drawNicknames(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		card(g, ctX, ctY, ctW, cardAH, "Ajouter / modifier un surnom");
		fieldBg(g, fx1, fieldY, fieldW, 22, pseudoBox.isFocused());
		fieldBg(g, fx2, fieldY, fieldW, 22, nameBox.isFocused());

		for (int i = 0; i < 16; i++) {
			int sx = ctX + 10 + i * (swSize + 3);
			boolean sel = selectedRgb == QUICK[i];
			boolean hover = mouseX >= sx && mouseX < sx + swSize && mouseY >= swY && mouseY < swY + swSize;
			Gfx.rrect(g, sx - (sel || hover ? 1 : 0), swY - (sel || hover ? 1 : 0),
				swSize + (sel || hover ? 2 : 0), swSize + (sel || hover ? 2 : 0), 3,
				sel ? 0xFFFFFFFF : (hover ? 0xFFB8B0E0 : edge()), sel ? 0xFFFFFFFF : (hover ? 0xFFB8B0E0 : edge()));
			Gfx.rrect(g, sx + 1, swY + 1, swSize - 2, swSize - 2, 2, 0xFF000000 | QUICK[i], 0xFF000000 | QUICK[i]);
		}

		String label = "Apercu : ";
		String raw = nameBox.getValue().isEmpty() ? "..." : nameBox.getValue().replace('&', '\u00a7');
		String prefix = selectedRgb >= 0 ? NameFormat.hexPrefix(selectedRgb) : "";
		g.text(this.font, label, ctX + 10, previewY, 0xFFC4B8F0, false);
		g.text(this.font, NameFormat.toComponent(prefix + raw, Style.EMPTY),
			ctX + 10 + this.font.width(label), previewY, 0xFFFFFFFF, false);

		card(g, ctX, listY, ctW, listH, "Surnoms (" + total + ")");

		if (rows.isEmpty()) {
			String none = "Aucun surnom pour le moment.";
			g.text(this.font, none, ctX + (ctW - this.font.width(none)) / 2, rowsTop + 6, 0xFFA29BCB, false);
		}

		int maxW = ctW - 20 - 64 - 22 - 12;
		for (int r = 0; r < rows.size(); r++) {
			int y = rowsTop + r * 24;
			Gfx.rrect(g, ctX + 8, y - 2, ctW - 16, 24, 5, Gfx.shade(Theme.c1, 0.34f), Gfx.shade(Theme.c2, 0.30f));
			Map.Entry<String, String> e = rows.get(r);
			Component line = NameFormat.toComponent(shown(e.getKey()) + " \u00a77->\u00a7r ", Style.EMPTY)
				.append(NameFormat.toComponent(e.getValue(), Style.EMPTY));
			g.enableScissor(ctX + 14, y, ctX + 14 + maxW, y + 20);
			g.text(this.font, line, ctX + 14, y + 6, 0xFFFFFFFF, false);
			g.disableScissor();
		}

		String info = (page + 1) + " / " + pages;
		g.text(this.font, info, ctX + (ctW - this.font.width(info)) / 2, listY + listH - 21, 0xFFA29BCB, false);
	}

	private void drawTheme(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		card(g, ctX, ctY, ctW, Math.min(ctH, 176), "Couleurs de l'interface");

		int[] cols = {Theme.c1, Theme.c2};
		for (int i = 0; i < 2; i++) {
			int y = themeY + i * 28;
			g.text(this.font, "Couleur " + (i + 1), ctX + 14, y + 6, 0xFFFFFFFF, false);
			Gfx.rrect(g, ctX + 90, y, 40, 20, 5, 0xFFFFFFFF, 0xFFFFFFFF);
			Gfx.rrect(g, ctX + 91, y + 1, 38, 18, 4, cols[i], cols[i]);
			g.text(this.font, hex(cols[i]), ctX + 138, y + 6, 0xFFC4B8F0, false);
		}

		Gfx.rrectH(g, ctX + 10, themeY + 60, ctW - 20, 18, 7, Theme.c1, Theme.c2);
		g.text(this.font, "Themes pre-faits", ctX + 12, themeY + 90, 0xFFC4B8F0, false);

		int chipW = (ctW - 20 - 5 * 6) / 6;
		for (int i = 0; i < PRESETS.length; i++) {
			int x = ctX + 10 + i * (chipW + 6);
			boolean hover = mouseX >= x && mouseX < x + chipW && mouseY >= themeY + 108 && mouseY < themeY + 130;
			if (hover) Gfx.rrect(g, x - 1, themeY + 107, chipW + 2, 24, 6, 0xFFFFFFFF, 0xFFFFFFFF);
			Gfx.rrectH(g, x, themeY + 108, chipW, 22, 5, 0xFF000000 | PRESETS[i][0], 0xFF000000 | PRESETS[i][1]);
		}
	}

	private void drawHelp(GuiGraphicsExtractor g) {
		card(g, ctX, ctY, ctW, ctH, "Aide");
		String[] lines = {
			"Touche N  -  ouvre cette interface",
			"(a changer dans Options > Controles)",
			"",
			"/rename  -  ouvre cette interface",
			"/rename set <pseudo> <nom>  -  ajoute un surnom",
			"/rename reset <pseudo>  -  supprime un surnom",
			"",
			"Couleur du surnom : pastilles ou 'Autre couleur'.",
			"Onglet Theme : choisis 2 couleurs pour le degrade.",
			"Le changement n'est visible que sur ton ecran.",
			"Le nouveau nom ne doit pas contenir l'ancien pseudo."
		};
		for (int i = 0; i < lines.length; i++) {
			g.text(this.font, lines[i], ctX + 12, ctY + 30 + i * 13, 0xFFDDD8F8, false);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}