package com.example.containersearch.mixin;

import com.example.containersearch.ContainerTracker;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Міксин для перехоплення оновлення та закриття екранів контейнерів (ScreenHandler / AbstractContainerScreen).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void onContainerTick(CallbackInfo ci) {
        ContainerTracker.updateContainerItems((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onScreenRemoved(CallbackInfo ci) {
        ContainerTracker.updateContainerItems((AbstractContainerScreen<?>) (Object) this);
    }
}
