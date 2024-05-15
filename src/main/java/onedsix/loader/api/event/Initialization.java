package onedsix.loader.api.event;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

/*
 * For Game interactive initialization, uses this initialization stage for initialization.
 */
@Slf4j
public class Initialization {

	public static class InitialzeEvent extends EventObject {
		public InitialzeEvent(Object source) {
			super(source);
		}
	}

	public interface InitializeListener extends EventListener {
		/** All game related APIs are available here.<br>
		 * At this point all mixins are done, and you can safely modify and run your mods code.<br>
		 * Here is where registering Registry entries and networking classes.<br>
		 * Go wild! */
		void onInitialize(InitialzeEvent event);
	}
	private static final List<InitializeListener> listeners = new ArrayList<>();

	public static void addListener(InitializeListener listener) {
		listeners.add(listener);
	}

	public static void sendInitPhaseEvent() {
		log.info("Mod Startup event tripped");
		for (InitializeListener listener : listeners) {
			InitialzeEvent event = new InitialzeEvent(listener.getClass());
			listener.onInitialize(event);
		}
	}
}
