package onedsix.loader.core;

import com.sun.jna.Function;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import onedsix.loader.core.ModLoadingHandler.ModLoadingException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Mods {
	public static final ArrayList<ModContainer> mods = new ArrayList<>();

	public static final String ENTRYPOINT = "onedsix.loader";
    public static final String GAMEVERSION = "1.0.0";
	public static final String LOADERVERSION = "1.0.0";

	// A copy from FileHandler, this may need to update from the original code when updating
	public static final String OS = System.getProperty("os.name").toLowerCase();
	public static final String gameDir = "./";
	public static String gameModsDir = "./mods/";
	public static boolean debug;
	public static boolean logClassLoad = false;

    public static void init() {
		if (debug) System.setProperty("mixins.debug", "true");
		if (OS.equalsIgnoreCase("windows 10")) { // https://stackoverflow.com/a/52767586
			// Set output mode to handle virtual terminal sequences
			Function GetStdHandleFunc = Function.getFunction("kernel32", "GetStdHandle");
			DWORD STD_OUTPUT_HANDLE = new DWORD(-11);
			HANDLE hOut = (HANDLE)GetStdHandleFunc.invoke(HANDLE.class, new Object[]{STD_OUTPUT_HANDLE});

			DWORDByReference p_dwMode = new DWORDByReference(new DWORD(0));
			Function GetConsoleModeFunc = Function.getFunction("kernel32", "GetConsoleMode");
			GetConsoleModeFunc.invoke(BOOL.class, new Object[]{hOut, p_dwMode});

			int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 4;
			DWORD dwMode = p_dwMode.getValue();
			dwMode.setValue(dwMode.intValue() | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
			Function SetConsoleModeFunc = Function.getFunction("kernel32", "SetConsoleMode");
			SetConsoleModeFunc.invoke(BOOL.class, new Object[]{hOut, dwMode});
		}

		LoaderInitialization.init();
	}

	public static void setDebug(boolean debug) {
		Mods.debug = debug;
	}

	public static void launchGame(ClassLoader loader, String[] args) {
		ModLoadingHandler.overallPro.cur = 6;
		ModLoadingHandler.overallPro.text = "Phase 3: Post-Init";
		ModLoadingHandler.secondaryPro = null;
		try {
			Class<?> c = loader.loadClass(ENTRYPOINT);
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, (Object) args);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Game has crashed", e.getCause());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to start game", e);
		}
	}
}
