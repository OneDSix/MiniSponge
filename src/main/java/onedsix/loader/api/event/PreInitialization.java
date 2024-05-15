package onedsix.loader.api.event;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

/*
 * For mod ready stage, uses this initialization stage for initialization.
 */
@Slf4j
public class PreInitialization {

	public static class PreInitEvent extends EventObject {
		public PreInitEvent(Object source) {
			super(source);
		}
	}

	public interface PreInitListener extends EventListener {
		/** Only a small portion of game APIs are available at this stage of startup.<br>
		 * Mixins haven't been processed yet, so it's best to stay within the confines of the mod in this method.<br>
		 * If you need to leave this scope, its probably best you do it with a mixins instead.<br>
		 * Set up configs and load assets here.*/
		void onPreInit(PreInitEvent event);
	}
	private static final List<PreInitListener> listeners = new ArrayList<>();

	public static void addListener(PreInitListener listener) {
		listeners.add(listener);
	}

	public static void sendPreInitEvent() {
		log.info("Mod Startup event tripped");
		for (PreInitListener listener : listeners) {
			PreInitEvent event = new PreInitEvent(listener.getClass());
			listener.onPreInit(event);
		}
	}
}
