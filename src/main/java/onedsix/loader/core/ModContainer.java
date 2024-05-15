package onedsix.loader.core;

import lombok.extern.slf4j.Slf4j;
import onedsix.loader.api.event.Initialization;
import onedsix.loader.api.event.Initialization.*;
import onedsix.loader.api.event.PostInitialization;
import onedsix.loader.api.event.PostInitialization.*;
import onedsix.loader.api.event.PreInitialization;
import onedsix.loader.api.event.PreInitialization.*;
import onedsix.loader.core.ModLoadingHandler.ModLoadingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

@Slf4j
public class ModContainer {

	public static final String postInitMethod = "onPostInit";
	public static final String initMethod = "onInitialize";
	public static final String preInitMethod = "onPreInit";
	public Class<?> entryClass;
	public Class<?> initClass;
	public Class<?> preInitClass;
	public final Manifest manifest;
	public final ModMetadata metadata;
	public final ModSettings settings;
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

			ZipEntry entry;
			if ((entry = jar.getEntry("settings.json")) != null) {
				settings = new ModSettings(new JSONObject(LoaderUtils.readStringFromInputStream(jar.getInputStream(entry))));
			}
			else {
				settings = new ModSettings(new JSONObject());
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
		public final String description;
		public final String author;
		public final String postInitPoint;
		public final String initPoint;
		public final String preInitPoint;
		// Versioning.
		public final ModVersion version;
		// public final ModVersion apiVersion;
		public final VersionRange gameVersion; // The target game version.
		public final VersionRange loaderVersion; // The target loader version.
		// Dependencies.
		private final ArrayList<ModDependency> dependencies = new ArrayList<>();

		private ModMetadata(JSONObject json) throws JSONException, ModVersion.MalformedModVersionFormatException {
			modId = json.getString("id");
			if (modId.isEmpty()) throw new JSONException("modId cannot be empty.");

			name = json.getString("name");
			description = json.optString("description");
			version = new ModVersion(json.optString("version", "1.0.0"));
			// if (json.has("apiVersion")) {
			// 	apiVersion = new ModVersion(json.getString("apiVersion"));
			// } else
			// 	apiVersion = version;

			gameVersion = VersionRange.createFromVersionSpec(json.getString("gameVersion"));
			loaderVersion = VersionRange.createFromVersionSpec(json.getString("loaderVersion"));

			author = json.optString("author");
			postInitPoint = json.optString("postInitPoint");
			initPoint = json.optString("initpoint");
			preInitPoint = json.optString("preInitpoint");

			if (json.has("dependencies")) {
				JSONArray deps = json.getJSONArray("dependencies");
				for (int i = 0; i < deps.length(); i++) {
					dependencies.add(new ModDependency(deps.getJSONObject(i)));
				}
			}
		}

		public ModDependency[] getDependencies() { return dependencies.toArray(new ModDependency[0]); }

		public static class ModDependency {
			public final String modId;
			public final VersionRange version;
			public final boolean essential;

			public ModDependency(JSONObject json) throws JSONException, ModVersion.MalformedModVersionFormatException {
				modId = json.getString("modId");
				version = VersionRange.createFromVersionSpec(json.getString("version"));
				essential = json.optBoolean("essential", true);
			}
		}
	}

	public static class ModSettings {
		/** Whether to reset default value when the value is out of set bounds in config. */
		public final boolean configValueResetDefaultIfOutOfScope;
		/** Whether to reset default value when the value is not matched with the type of bounds or default value or missing in config. */
		public final boolean configNumericLimitIgnoreOtherTypedValue;

		private ModSettings(JSONObject json) {
			configValueResetDefaultIfOutOfScope = json.optBoolean("configValueResetDefaultIfOutOfScope", true);
			configNumericLimitIgnoreOtherTypedValue = json.optBoolean("configNumericLimitIgnoreOtherTypedValue", false);
		}
	}
}
