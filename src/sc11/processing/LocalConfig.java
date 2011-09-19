package sc11.processing;

public class LocalConfig {

	public final boolean verbose;
	
	public final String tmpdir;
	public final String scriptdir;
	
	private static LocalConfig config;
	
	private LocalConfig(boolean verbose, String tmpdir, String scriptdir) {
		this.verbose = verbose;
		this.tmpdir = tmpdir;
		this.scriptdir = scriptdir;
	}
	
	public static void set(boolean verbose, String tmpdir, String scriptdir) { 
		config = new LocalConfig(verbose, tmpdir, scriptdir);
	}

	public static String getTmpDir() {
		
		if (config == null) { 
			throw new RuntimeException("LocalConfig not set!");
		}
		
		return config.tmpdir;
	}
	
	public static String getScriptDir() {
		
		if (config == null) { 
			throw new RuntimeException("LocalConfig not set!");
		}
		
		return config.scriptdir;
	}
	
	public static boolean verbose() {
		
		if (config == null) { 
			throw new RuntimeException("LocalConfig not set!");
		}
		
		return config.verbose;
	}
	
	public static LocalConfig get() {
		
		if (config == null) { 
			throw new RuntimeException("LocalConfig not set!");
		}
		
		return config;
	}
	
	public static void println(String message) { 
		if (config == null || config.verbose) {
			System.out.println(message);
		}
	}
	
	public static void println(String message, Exception e) { 
		System.out.println(message);
		
		if (e != null) { 
			e.printStackTrace();
		} 
	}

}
