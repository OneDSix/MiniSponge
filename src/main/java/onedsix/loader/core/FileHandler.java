package onedsix.loader.core;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static onedsix.loader.core.LoaderUtils.*;

@Slf4j
public class FileHandler {

	public static void checkAndReplaceWithDir(File file) {
		if (file.isFile()) {
			log.error("File \"{}\" is not a directory, it is replaced by a newly created directory. All contents in the path are removed.", file);
			file.delete();
			file.mkdirs();
		}
	}
	public static void checkAndDeleteIfDir(File file) {
		if (file.isDirectory()) {
			log.error("File \"{}\" is a directory, it is replaced by a newly created empty file. All contents in the path are removed.", file);
			file.delete();
		}
	}

	/**
	 * Searches both the Local directory ({@code ./}) and the .jar resource directory ({@code ./resources} pre-compile)<br>
	 * Do not prepend a {@code /} to the file name.
	 * */
	public static JSONObject searchForFile(String fileToSearch) {
		try {
			// Search in the resource folder of the JAR
			String s = readStringFromInputStream(getResource("/"+fileToSearch));
			if (s != null) return new JSONObject(s);
			else throw new RuntimeException("Local Directory String/InputStream is null");
		}
		catch (Exception e) {
			log.error("Could not read file from Resource: ", e);
		}

		try {
			// Search in the local directory
			String s = readStringFromFile(new File(fileToSearch));
			if (s != null) return new JSONObject(s);
			else throw new RuntimeException("Local Directory String/InputStream is null");
		}
		catch (Exception e) {
			log.error("Could not read file from Local: ", e);
		}

		// Failed, returning null
		return null;
	}

	public static JSONObject searchForConfig() {
		final String name = "loader.config.json";
		JSONObject config;

		config = searchForFile(name);
		if (config != null) {
			return config;
		}

		try {
			// Search in the local directory
			return new JSONObject(readStringFromFile(new File("./config/loader" + name)));
		}
		catch (Exception e) {
			log.error("Could not read file from Config: ", e);
		}

		// Failed, returning null
		return null;
	}

	public static InputStream getResource(String fileName) {
		return FileHandler.class.getResourceAsStream(fileName);
	}

	/** Returns the current file, usually a <code>.jar</code>, sometimes a <code>.class</code>. */
	static File currentFile() throws URISyntaxException {
		return Paths.get(FileHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName().toFile();
	}
}
