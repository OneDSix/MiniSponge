package onedsix.loader.core;

import lombok.extern.slf4j.Slf4j;
import onedsix.loader.mixin.ModClassDelegate;
import onedsix.loader.mixin.ModClassLoader;
import onedsix.loader.mixin.ModMixinBootstrap;
import org.json.JSONObject;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.Manifest;

import static onedsix.loader.core.FileHandler.searchForConfig;

@Slf4j
public class LoaderInitialization {
	public static final JSONObject config;
	private static ModClassDelegate classLoader;
	private static boolean unlocked;

	static {
		try {
			config = searchForConfig();
		}
		catch (Exception e) {
			throw new RuntimeException("Loader config is missing, misplaced, or some other error occurred that stopped it from loading. A file called \"loader.config.json\" must be located in either \"./\", \"./resources/\" (ONLY BEFORE COMPILATION), or \"./config/loader/\"", e);
		}
	}

	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			throwable.printStackTrace();

			StringWriter string = new StringWriter();
			PrintWriter printer = new PrintWriter(string);
			throwable.printStackTrace(printer);

			JTextArea errorDisplay = new JTextArea(string.toString());
			errorDisplay.setEditable(false);
			JScrollPane errorPane = new JScrollPane(errorDisplay);
			JOptionPane.showMessageDialog(null, errorPane, "An error has occurred", JOptionPane.ERROR_MESSAGE);

			System.exit(-1);
		});

		ModLoadingHandler.initLoadingScreen();
		ModLoadingHandler.overallPro.text = "Starting Initialization";

		ModConfigFileHandler.loadLoaderStageModConfig();
		Mods.init();

		Mods.launchGame(classLoader.getClassLoader(), args);
	}

	public static void init() {
		ModLoadingHandler.overallPro.cur = 1;
		ModLoadingHandler.overallPro.text = "Initializing Mod Loader";

		(ModLoadingHandler.secondaryPro = new ModLoadingHandler.Progress(1)).text = "Getting Class Loader";
		classLoader = new ModClassLoader().getDelegate();
		ClassLoader cl = classLoader.getClassLoader();
		Thread.currentThread().setContextClassLoader(cl);

		// Adding code sources.
		Path cwd = Paths.get("").toAbsolutePath();
		classLoader.setValidParentClassPath(Arrays.asList( // TODO use a better system, wtf?!
			cwd.resolve("./minimods-2.0.7-0.4.0.jar"),
			cwd.resolve("./desktop-d1.1.0.jar"),

			cwd.resolve("lib/annotations-21.0.1.jar"),
			cwd.resolve("lib/asm-9.6.jar"),
			cwd.resolve("lib/asm-analysis-9.6.jar"),
			cwd.resolve("lib/asm-commons-9.6.jar"),
			cwd.resolve("lib/asm-tree-9.6.jar"),
			cwd.resolve("lib/asm-util-9.6.jar"),
			cwd.resolve("lib/checker-qual-3.12.0.jar"),
			cwd.resolve("lib/commons-codec-1.15.jar"),
			cwd.resolve("lib/commons-io-2.4.jar"),
			cwd.resolve("lib/commons-logging-1.2.jar"),
			cwd.resolve("lib/dnsjava-3.4.1.jar"),
			cwd.resolve("lib/error_prone_annotations-2.11.0.jar"),
			cwd.resolve("lib/failureaccess-1.0.1.jar"),
			cwd.resolve("lib/gson-2.10.1.jar"),
			cwd.resolve("lib/guava-31.1-jre.jar"),
			cwd.resolve("lib/httpasyncclient-4.1.5.jar"),
			cwd.resolve("lib/httpclient-4.5.13.jar"),
			cwd.resolve("lib/httpcore-4.4.15.jar"),
			cwd.resolve("lib/httpcore-nio-4.4.15.jar"),
			cwd.resolve("lib/httpmime-4.5.13.jar"),
			cwd.resolve("lib/j2objc-annotations-1.3.jar"),
			cwd.resolve("lib/jna-5.12.1.jar"),
			cwd.resolve("lib/jna-platform-5.12.1.jar"),
			cwd.resolve("lib/json-20211205.jar"),
			cwd.resolve("lib/jsr305-3.0.2.jar"),
			cwd.resolve("lib/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar"),
			cwd.resolve("lib/mixins-0.8.5.jar"),
			cwd.resolve("lib/oshi-core-6.2.2.jar"),
			cwd.resolve("lib/slf4j-api-1.7.36.jar"),
			cwd.resolve("lib/unirest-java-1.4.9.jar"),
			cwd.resolve("lib/unirest-java-3.13.7.jar"),
			cwd.resolve("lib/xmlgraphics-commons-2.6.jar")
		));

		ModLoadingHandler.secondaryPro.cur = 1;
		ModLoadingHandler.secondaryPro.text = "Applying Loader Injection";
		GameTransformer.transform();

		ModLoadingHandler.overallPro.cur = 2;
		ModLoadingHandler.overallPro.text = "Finding Mods";
		ModLoadingHandler.secondaryPro = null;
		ModFindHander.findMods();

		ModLoadingHandler.overallPro.cur = 3;
		ModLoadingHandler.overallPro.text = "Phase 1: PreInit";
		ModLoadingHandler.secondaryPro = null;
		// Load ALL loader stage ModConfig.

		// PreInit Mods
		ModHandler.preInitPhaseMods();

		ModLoadingHandler.overallPro.cur = 4;
		ModLoadingHandler.overallPro.text = "Booting Mixin";
		ModLoadingHandler.secondaryPro = null;
		ModMixinBootstrap.init();
		ModMixinBootstrap.goPhaseInit();

		classLoader.initializeTransformers();

		try {
			addToClassPath(Paths.get(LoaderInitialization.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		unlocked = true;

		ModLoadingHandler.overallPro.cur = 5;
		ModLoadingHandler.overallPro.text = "Phase 2: Init";
		ModLoadingHandler.secondaryPro = null;
		ModHandler.initPhaseMods();
		ModMixinBootstrap.goPhaseDefault();
	}

	public static void addToClassPath(Path path, String... allowedPrefixes) {
		log.info("Adding " + path + " to classpath.");

		classLoader.setAllowedPrefixes(path, allowedPrefixes);
		classLoader.addCodeSource(path);
	}

	public static void setAllowedPrefixes(Path path, String... prefixes) {
		classLoader.setAllowedPrefixes(path, prefixes);
	}

	public static void setValidParentClassPath(Collection<Path> paths) {
		classLoader.setValidParentClassPath(paths);
	}

	public static boolean isClassLoaded(String name) {
		return classLoader.isClassLoaded(name);
	}

	public static Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
		return classLoader.loadIntoTarget(name);
	}

	public static InputStream getResourceAsStream(String name) {
		return classLoader.getClassLoader().getResourceAsStream(name);
	}

	public static ClassLoader getTargetClassLoader() {
		ModClassDelegate classLoader = LoaderInitialization.classLoader;

		return classLoader != null ? classLoader.getClassLoader() : null;
	}

	public static byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
		if (!unlocked) throw new IllegalStateException("early getClassByteArray access");

		if (runTransformers) {
			return classLoader.getPreMixinClassBytes(name);
		} else {
			return classLoader.getRawClassBytes(name);
		}
	}

	public static Manifest getManifest(Path originPath) {
		return classLoader.getManifest(originPath);
	}

	public static boolean isDebug() {
		return Mods.debug;
	}
}
