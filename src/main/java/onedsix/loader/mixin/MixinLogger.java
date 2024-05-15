package onedsix.loader.mixin;

import lombok.extern.slf4j.Slf4j;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

/**
 * <b>Note: {@link Slf4j} and {@link MixinLogger#log(Level, String, Object...)} are not the same thing!</b><br>
 * One is added by {@link lombok.extern.slf4j}, the other {@link org.spongepowered.asm}.
 * */
@Slf4j
final class MixinLogger extends LoggerAdapterAbstract {

	MixinLogger(String name) {
		super(name);
	}

	@Override
	public String getType() {
		return "MiniMods Mixin Logger";
	}

	@Override
	public void catching(Level level, Throwable t) {
		log(level, "Catching ".concat(t.toString()), t);
	}

	@Override
	public void log(Level level, String message, Object... params) {
		switch (level) {
			case WARN:
			case ERROR:
				log.error(message);
				break;
			default:
			case DEBUG:
			case TRACE:
			case INFO:
				log.info(message);
				break;
		}
	}

	@Override
	public void log(Level level, String message, Throwable t) {
		log(level, message, new Object[]{t});
	}

	@Override
	public <T extends Throwable> T throwing(T t) {
		log(Level.ERROR, "Throwing ".concat(t.toString()), t);

		return t;
	}
}
