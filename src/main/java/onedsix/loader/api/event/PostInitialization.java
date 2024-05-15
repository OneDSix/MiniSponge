package onedsix.loader.api.event;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

@Slf4j
public class PostInitialization {

	public static class PostInitEvent extends EventObject {
		public PostInitEvent(Object source) {
			super(source);
		}
	}

	public interface PostInitListener extends EventListener {
		/** All game related APIs are available here.<br>
		 * At this point all mixins are done, and you can safely modify and run your mods code.<br>
		 * Here is where registering Registry entries and networking classes.<br>
		 * Go wild! */
		void onPostInit(PostInitEvent event);
	}
	private static final List<PostInitListener> listeners = new ArrayList<>();

	public static void addListener(PostInitListener listener) {
		listeners.add(listener);
	}

	public static void sendPostInitEvent() {
		log.info("Mod Startup event tripped");
		for (PostInitListener listener : listeners) {
			PostInitEvent event = new PostInitEvent(listener.getClass());
			listener.onPostInit(event);
		}
	}
}
