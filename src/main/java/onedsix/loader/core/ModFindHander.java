package onedsix.loader.core;

import lombok.extern.slf4j.Slf4j;
import onedsix.loader.core.ModLoadingHandler.ModLoadingException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.jar.JarFile;

import static onedsix.loader.core.Mods.GAMEVERSION;
import static onedsix.loader.core.Mods.LOADERVERSION;

@Slf4j
public class ModFindHander {

	public static final ModVersion COMPAT_GAME_VERSION = new ModVersion(GAMEVERSION);
	public static final ModVersion COMPAT_LOADER_VERSION = new ModVersion(LOADERVERSION);

	/**
	 * This method is split into 3 sections:<br>
	 * <br>
	 * <h2>Finding Mods</h2>
	 * This section is where the mods are found and stored.
	 * */
	static void findMods() {
		File[] files = readModsFolder();
		if (files.length == 0) {
			ModLoadingHandler.secondaryPro = new ModLoadingHandler.Progress(1);
			ModLoadingHandler.secondaryPro.cur = 1;
			ModLoadingHandler.secondaryPro.text = "No Mods";
			log.info("No mods found.");
		} else {
			ModLoadingHandler.secondaryPro = new ModLoadingHandler.Progress(files.length);
			for (File file : files) {
				ModLoadingHandler.secondaryPro.text = "Found: " + file.getName();
				try (JarFile jar = new JarFile(file)) {
					URLClassLoader child = new URLClassLoader(
						new URL[] {file.toURI().toURL()},
						ModFindHander.class.getClassLoader()
					);

					ModContainer mod = new ModContainer(jar, child);
					Mods.mods.add(mod);
				} catch (IOException e) {
					throw new ModLoadingException(e);
				}

				ModLoadingHandler.secondaryPro.cur++;
			}
		}

		int count = 0;
        while (true) { // Sorting with their dependencies.
            for (int i = 0; i < Mods.mods.size(); i++) {
                if (count > Mods.mods.size()*Mods.mods.size()) {
                    throw new ModLoadingException("Dependency structure too complex.");
                }

                ModContainer.ModDependency[] deps = Mods.mods.get(i).metadata.getDependencies();
                if (deps.length > 0) {
                    int index = i;
					for (ModContainer.ModDependency n : deps) {
						int jdx = Mods.mods.indexOf(Mods.mods.stream().filter(m -> m.metadata.modId.equals(n.modId)).findAny().orElse(null));
						if (jdx == -1) {
							if (n.dependencyType.equals("REQUIRED")) throw new ModLoadingException("Dependency not found: " + n.modId);
							log.info(String.format("Unessential dependency does not exist: %s for %s", n.modId, Mods.mods.get(i).metadata.modId));
							continue; // Skip if not exist and not essential.
						} else if (!n.version.containsVersion(Mods.mods.get(jdx).metadata.version)) {
							throw new ModLoadingException("Dependency not compatible: " + n.modId + " " + n.version + "; found: " + Mods.mods.get(jdx).metadata.version);
						}

						if (jdx > index) index = jdx;
					}

                    if (index > i) Mods.mods.add(index, Mods.mods.remove(i));
                }

                count++;
            }

            boolean valid = true;
            for (int i = 0; i < Mods.mods.size(); i++) {
                ModContainer.ModDependency[] deps = Mods.mods.get(i).metadata.getDependencies();
                if (deps.length > 0) {
                    int index = i;
					for (ModContainer.ModDependency n : deps) {
						int jdx = Mods.mods.indexOf(Mods.mods.stream().filter(m -> m.metadata.modId.equals(n.modId)).findAny().orElse(null));
						if (jdx > index) index = jdx;
					}

                    if (index > i) valid = false;
                }
            }

            if (valid) break;
        }

		// Adding mods to the classpaths.
		for (ModContainer mod : Mods.mods) {
			LoaderInitialization.addToClassPath(mod.jarPath);
		}
	}

	private static File[] readModsFolder() {
		log.info(String.valueOf(new File(Mods.gameModsDir)));
		if (!new File(Mods.gameModsDir).exists())
			new File(Mods.gameModsDir).mkdirs();

		return new File(Mods.gameModsDir).listFiles((dir, name) -> name.endsWith(".jar"));
	}

	public static boolean checkModToLoaderCompatibility(ModContainer mod) throws ModLoadingHandler.ModLoadingException {
		ModContainer.ModMetadata meta = mod.metadata;
		if (!meta.loaderVersion.containsVersion(COMPAT_LOADER_VERSION)) {
			log.warn(String.format("Incompatible mod: %s: compatible loader version %s; current: %s", meta.modId, meta.loaderVersion, LOADERVERSION));
			return false;
		}
		return true;
	}
}
