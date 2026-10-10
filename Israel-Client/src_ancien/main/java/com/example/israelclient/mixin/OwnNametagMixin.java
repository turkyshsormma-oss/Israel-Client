package com.example.israelclient.mixin;

import com.example.israelclient.NameStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class OwnNametagMixin {
	@Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true, require = 0)
	private void israelclient$showOwnName(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) return;
		Minecraft mc = Minecraft.getInstance();
		if (entity == mc.player
				&& Minecraft.renderNames()
				&& !mc.options.getCameraType().isFirstPerson()
				&& NameStore.get(mc.player.getGameProfile().name()) != null) {
			cir.setReturnValue(true);
		}
	}
}
