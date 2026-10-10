package com.example.namechanger.mixin;

import com.example.namechanger.NameFormat;
import com.example.namechanger.NameStore;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(StringDecomposer.class)
public abstract class StringTextMixin {
	@ModifyVariable(
		method = "iterateFormatted(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
		at = @At("HEAD"), argsOnly = true, require = 0)
	private static String namechanger$replace(String text) {
		if (text == null || NameStore.all().isEmpty()) return text;
		String lower = text.toLowerCase(Locale.ROOT);
		String out = text;
		for (var e : NameStore.all().entrySet()) {
			if (lower.contains(e.getKey())) {
				out = Pattern.compile(Pattern.quote(e.getKey()), Pattern.CASE_INSENSITIVE)
					.matcher(out)
					.replaceAll(Matcher.quoteReplacement(NameFormat.toLegacy(e.getValue())));
			}
		}
		return out;
	}
}