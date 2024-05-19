package onedsix.loader.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onedsix.loader.api.event.Initialization;
import onedsix.loader.api.event.Initialization.InitializeListener;
import onedsix.loader.api.event.PostInitialization;
import onedsix.loader.api.event.PostInitialization.PostInitListener;
import onedsix.loader.api.event.PreInitialization;
import onedsix.loader.api.event.PreInitialization.PreInitListener;
import onedsix.loader.core.ModLoadingHandler.ModLoadingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Slf4j
public class ModContainer {
	public Class<?> entryClass;
	public Class<?> initClass;
	public Class<?> preInitClass;
	public final Manifest manifest;
	public final ModMetadata metadata;
	public final String mixinConfig;
	public final Path jarPath;

	public ModContainer(JarFile jar, URLClassLoader child) {
		try (URLClassLoader gameMobLoader = new URLClassLoader(child.getURLs(), LoaderInitialization.getTargetClassLoader())) {
			metadata = new ModMetadata(new JSONObject(LoaderUtils.readStringFromInputStream(jar.getInputStream(jar.getEntry("mod.json")))));
			if (!metadata.postInitPoint.isEmpty()) {
				try {
					Class<?> clazz = Class.forName(metadata.postInitPoint, false, gameMobLoader);
					if (PostInitListener.class.isAssignableFrom(clazz))
						PostInitialization.addListener((PostInitListener) clazz.getDeclaredConstructor().newInstance());
				}
				catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			if (!metadata.preInitPoint.isEmpty()) {
				try {
					Class<?> clazz = Class.forName(metadata.preInitPoint, false, child);
					if (PreInitListener.class.isAssignableFrom(clazz))
						PreInitialization.addListener((PreInitListener) clazz.getDeclaredConstructor().newInstance());
				}
				catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			if (!metadata.initPoint.isEmpty()) {
				try {
					Class<?> clazz = Class.forName(metadata.initPoint, false, child);
					if (InitializeListener.class.isAssignableFrom(clazz))
						Initialization.addListener((InitializeListener) clazz.getDeclaredConstructor().newInstance());
				}
				catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			manifest = jar.getManifest();

			jarPath = Paths.get(jar.getName());

			if (jar.getEntry(metadata.modId + ".mixins.json") != null) {
				mixinConfig = metadata.modId + ".mixins.json";
			}
			else {
				mixinConfig = null;
			}

		} catch (IOException | JSONException | ModVersion.MalformedModVersionFormatException | NullPointerException e) {
			throw new ModLoadingException("Unable to load mod file: " + jar.getName(), e);
		}
	}

	public static class ModMetadata {
		public final String modId;
		public final String name;
		public final String license;
		public final String description;
		public final String author;
		public final String postInitPoint;
		public final String initPoint;
		public final String preInitPoint;
		public final ModVersion version; // Versioning.
		public final VersionRange loaderVersion; // The target loader version.
		public final ModSettings settings = new ModSettings();
		private final LinkedList<ModDependency> dependencies = new LinkedList<>(); // Dependencies.

		private ModMetadata(JSONObject json) throws JSONException, ModVersion.MalformedModVersionFormatException {
			modId = json.getString("id");
			if (modId.isEmpty()) throw new JSONException("modId cannot be empty.");
			name = json.getString("name");
			description = json.optString("description");
			author = json.optString("author");
			license = json.optString("license", "ARR");

			postInitPoint = json.optString("postInitPoint");
			initPoint = json.optString("initpoint");
			preInitPoint = json.optString("preInitpoint");
			version = new ModVersion(json.optString("version", "1.0.0"));
			loaderVersion = VersionRange.createFromVersionSpec(json.getString("loaderVersion"));

			if (json.has("dependencies")) {
				JSONArray deps = json.getJSONArray("dependencies");

				for (int i = 0; i < deps.length(); i++) {
					dependencies.add(new ModDependency(deps.getJSONObject(i)));
				}
			}

			if (json.has("settings")) {
				JSONObject sets = json.getJSONObject("settings");

				settings.configValueResetDefaultIfOutOfScope =
					sets.optBoolean("configValueResetDefaultIfOutOfScope", true);
				settings.configNumericLimitIgnoreOtherTypedValue =
					sets.optBoolean("configNumericLimitIgnoreOtherTypedValue", false);
			}
		}

		public ModDependency[] getDependencies() { return dependencies.toArray(new ModDependency[0]); }
	}

	@Data
	public static class ModSettings {
		/** Whether to reset default value when the value is out of set bounds in config. */
		public boolean configValueResetDefaultIfOutOfScope;
		/** Whether to reset default value when the value is not matched with the type of bounds or default value or missing in config. */
		public boolean configNumericLimitIgnoreOtherTypedValue;
	}

	/** Based off <a href="https://docs.neoforged.net/docs/gettingstarted/modfiles/#dependency-configurations">NeoForge</a>'s dependency config. */
	public static class ModDependency {

		public final String modId;
		public final VersionRange version;
		public final String dependencyType;
		public final String dependencyReason;
		public final String ordering;
		/** Either "CLIENT", "SERVER", or "BOTH"/none */
		public final String side;
		/** A URL to the Git repo or Download page of the mod. */
		public final String referral;
		/** A small Toml instance sent to the mod to be computed by it. Usually used in the case of Modules or forcing specific configs. */
		public final JSONObject passedToMod;

		public ModDependency(JSONObject json) throws JSONException, ModVersion.MalformedModVersionFormatException {
			modId = json.getString("modId");
			version = VersionRange.createFromVersionSpec(json.getString("version"));
			referral = json.optString("referral", "http://localhost/"); // TODO: dangerous?

			dependencyType = json.optString("type", "REQUIRED");
			dependencyReason = json.optString("reason", "No Reason Given");
			ordering = json.optString("ordering", "AFTER");
			side = json.optString("side", "BOTH");

			passedToMod = json.getJSONObject("passed");
		}

		/** <h2>REQUIRED</h2>
		 * "REQUIRED" is the default and prevents the mod from loading if the dependency is missing.<br>
		 * <br>
		 * <h2>OPTIONAL</h2>
		 * Will not prevent the mod from loading if the dependency is missing, but still validates that the dependency is compatible.<br>
		 * Used primarily to say something is compatible, but optional, mostly in the case of plugins to other mods.<br>
		 * <br>
		 * <h2>DISCOURAGED</h2>
		 * Still allows the mod to load if the dependency is present, but presents a warning to the user in the console and upon launch.<br>
		 * <br>
		 * <h2>INCOMPATIBLE</h2>
		 * Prevents the game from loading if this dependency is present.<br>
		 * Used primarily for mods that do similar things and are incompatible in many ways. <br>
		 *
		 * @see MissingDependencyException
		 * @see IncompatibleDependencyException
		 * */
		public static final String[] DependencyTypes = {
			"REQUIRED",
			"OPTIONAL",
			"DISCOURAGED",
			"INCOMPATIBLE"
		};

		/** The names are pretty self-explanatory, this mod should load BEFORE or AFTER the dependency has loaded.<br>
		 * The NONE value means the order is subjective and is chosen at runtime.
		 * @see CyclicDependencyError */
		public static final String[] Ordering = {
			"NONE",
			"BEFORE",
			"AFTER"
		};
	}

	public static class CyclicDependencyError extends RuntimeException {
		public CyclicDependencyError(String msg) { super(msg); }
	}

	public static class MissingDependencyException extends RuntimeException {
		public MissingDependencyException(String msg) { super(msg); }
	}

	public static class IncompatibleDependencyException extends RuntimeException {
		public IncompatibleDependencyException(String msg) { super(msg); }
	}
}
