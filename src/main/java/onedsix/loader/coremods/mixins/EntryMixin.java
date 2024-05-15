package onedsix.loader.coremods.mixins;

import lombok.extern.slf4j.Slf4j;
import onedsix.Entrypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static onedsix.loader.core.ModHandler.initMods;

@Slf4j
@Mixin(Entrypoint.class)
public class EntryMixin {

	@Inject(method = "main([Ljava/lang/String;)V", at = @At(value = "TAIL"), remap = false)
	private static void mainInitRun(CallbackInfo ci) {
		log.info("Mixin Success Check.");
		initMods();
	}
}
