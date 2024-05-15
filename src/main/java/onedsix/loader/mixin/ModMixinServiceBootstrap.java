package onedsix.loader.mixin;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class ModMixinServiceBootstrap implements IMixinServiceBootstrap {
	@Override
	public String getName() {
		return "MiniMods";
	}

	@Override
	public String getServiceClassName() {
		return "minicraft.minimods.mixins.ModMixinService";
	}

	@Override
	public void bootstrap() {
		// already done in LoaderInitialization.
	}
}
