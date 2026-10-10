package com.example.namechanger.mixin;

import com.example.namechanger.ChatRewriter;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class TabListMixin {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true, require = 0)
	private void namechanger$tab(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		cir.setReturnValue(ChatRewriter.rewrite(cir.getReturnValue()));
	}
}