package onedsix.loader.core;

import lombok.extern.slf4j.Slf4j;
import onedsix.core.util.Logger;

import java.io.File;

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
}
