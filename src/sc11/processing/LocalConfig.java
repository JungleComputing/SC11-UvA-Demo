package sc11.processing;

public class LocalConfig {

	public final String tmpdir;
	public final String scriptdir;
	
	private static LocalConfig config;
	
	private LocalConfig(String tmpdir, String scriptdir) { 
		this.tmpdir = tmpdir;
		this.scriptdir = scriptdir;
	}
	
	public static void set(String tmpdir, String scriptdir) { 
		config = new LocalConfig(tmpdir, scriptdir);
	}

	public static LocalConfig get() { 
		return config;
	}
}
