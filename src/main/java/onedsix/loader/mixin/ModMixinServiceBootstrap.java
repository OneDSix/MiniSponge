package onedsix.loader.mixin;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class ModMixinServiceBootstrap implements IMixinServiceBootstrap {
	@Override
	public String getName() {
		return "MiniSponge";
	}

	@Override
	public String getServiceClassName() {
		return "onedsix.loader.mixin.ModMixinService";
	}

	@Override
	public void bootstrap() {
		// already done in LoaderInitialization.
	}
}
