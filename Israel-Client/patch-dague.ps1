$ErrorActionPreference = "Stop"
$base = Join-Path $PSScriptRoot "src\main\java\com\example\namechanger"

function Patch($file, $old, $new) {
	$path = Join-Path $base $file
	$t = [System.IO.File]::ReadAllText($path)
	if (-not $t.Contains($old)) { throw "Introuvable dans $file : $old" }
	[System.IO.File]::WriteAllText($path, $t.Replace($old, $new))
}

# --- verifications ---
$screen = [System.IO.File]::ReadAllText((Join-Path $base "NameScreen.java"))
if ($screen.Contains("initDagger")) { throw "NameScreen.java contient deja l'onglet Dague." }

$dcPath = Join-Path $base "DaggerConfig.java"
if (-not (Test-Path $dcPath)) { throw "DaggerConfig.java manquant." }
$dc = [System.IO.File]::ReadAllText($dcPath)
if (-not $dc.Contains("class Rule")) { throw "DaggerConfig.java n'est pas la version avec les regles." }

# --- DaggerConfig : messages vers le journal (anciennes methodes inexistantes) ---
$dc = $dc.Replace('mc.player.displayClientMessage(Component.literal(text), true);', 'System.out.println("[namechanger] " + text);')
$dc = $dc.Replace('mc.gui.getChat().addMessage(Component.literal(text));', 'System.out.println("[namechanger] " + text);')
[System.IO.File]::WriteAllText($dcPath, $dc)

# --- NameScreen : onglet ---
Patch "NameScreen.java" 'private static final String[] TABS = {"Surnoms", "Theme", "Aide"};' 'private static final String[] TABS = {"Surnoms", "Theme", "Dague", "Aide"};'

Patch "NameScreen.java" 'else if (tab == 1) initTheme();' @'
else if (tab == 1) initTheme();
		else if (tab == 2) initDagger();
'@

Patch "NameScreen.java" 'else if (tab == 1) drawTheme(g, mouseX, mouseY);' @'
else if (tab == 1) drawTheme(g, mouseX, mouseY);
		else if (tab == 2) drawDagger(g);
'@

Patch "NameScreen.java" 'private void initTheme() {' @'
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
'@

# --- NameChangerClient : touche B ---
$clientPath = Join-Path $base "NameChangerClient.java"
$client = [System.IO.File]::ReadAllText($clientPath)
if (-not $client.Contains("daggerKey")) {
	Patch "NameChangerClient.java" 'ClientTickEvents.END_CLIENT_TICK.register(client -> {' @'
KeyMapping daggerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.namechanger.take_dagger", InputConstants.Type.KEYSYM, InputConstants.KEY_B, category));
		DaggerConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (daggerKey.consumeClick()) DaggerConfig.take(client);
'@
}

# --- Traductions ---
$lang = Join-Path $PSScriptRoot "src\main\resources\assets\namechanger\lang"
New-Item -ItemType Directory -Force $lang | Out-Null
[System.IO.File]::WriteAllText("$lang\en_us.json", '{"key.category.namechanger.main":"Name Changer","key.namechanger.open_gui":"Open interface","key.namechanger.take_dagger":"Select dagger"}')
