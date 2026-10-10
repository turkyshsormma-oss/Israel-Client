package com.example.israelclient.mixin;

import com.example.israelclient.ChatRewriter;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientLanguage.class)
public abstract class UniversalTextMixin {
	@ModifyVariable(method = "getVisualOrder", at = @At("HEAD"), argsOnly = true, require = 0)
	private FormattedText israelclient$rewrite(FormattedText text) {
		return text instanceof Component c ? ChatRewriter.rewrite(c) : text;
	}
}
