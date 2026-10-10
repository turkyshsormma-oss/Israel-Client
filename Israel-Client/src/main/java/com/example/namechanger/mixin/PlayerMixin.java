package com.example.namechanger.mixin;

import com.example.namechanger.NameFormat;
import com.example.namechanger.NameStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerMixin {
	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	private void namechanger$replaceName(CallbackInfoReturnable<Component> cir) {
		if ((Object) this instanceof Player player) {
			String custom = NameStore.get(player.getGameProfile().name());
			if (custom != null) {
				cir.setReturnValue(NameFormat.toComponent(custom, Style.EMPTY));
			}
		}
	}
}

