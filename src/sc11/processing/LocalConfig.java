package sc11.processing;

import sc11.shared.Result;

/**
 * A simple helper class providing some static fields and methods.
 *
 * @author jason@cs.vu.nl
 */

public class LocalConfig {

    /** Should we be verbose ? */
    public final boolean verbose;

    /** The path of the temporary directory. */
    public final String tmpdir;

    /** The path of the directory containing the scripts to execute. */
    public final String scriptdir;

    private static LocalConfig config;

    private LocalConfig(boolean verbose, String tmpdir, String scriptdir) {
        this.verbose = verbose;
        this.tmpdir = tmpdir;
        this.scriptdir = scriptdir;
    }

    /**
     * Set the global verbose, tmpdir and scriptdir fields.
     *
     * @param verbose
     * @param tmpdir
     * @param scriptdir
     */
    public static void set(boolean verbose, String tmpdir, String scriptdir) {
        config = new LocalConfig(verbose, tmpdir, scriptdir);
    }

    /**
     * Retrieve the location of the temporary directory.
     * @return the location of the temporary directory.
     */
    public static String getTmpDir() {

        if (config == null) {
            throw new RuntimeException("LocalConfig not set!");
        }

        return config.tmpdir;
    }

    /**
     * Retrieve the location of the scripts directory.
     * @return the location of the scripts directory.
     */
    public static String getScriptDir() {

        if (config == null) {
            throw new RuntimeException("LocalConfig not set!");
        }

        return config.scriptdir;
    }


    /**
     * Retrieve the verbose flag.
     * @return the verbose flag.
     */
    public static boolean verbose() {

        if (config == null) {
            throw new RuntimeException("LocalConfig not set!");
        }

        return config.verbose;
    }

    /**
     * Retrieve the LocalConfig object containing the shared global fields.
     *
     * @return the LocalConfig object.
     */
    public static LocalConfig get() {

        if (config == null) {
            throw new RuntimeException("LocalConfig not set!");
        }

        return config;
    }

    /**
     * Prints a message, provided that verbose is true or unset.
     *
     * @param message the message to print.
     */
    public static void println(String message) {
        if (config == null || config.verbose) {
            System.out.println(message);
        }
    }

    /**
     * Prints a message and a result, provided that verbose is true or unset.
     *
     * @param message the message to print.
     * @param result the result to print.
     */
    public static void println(String message, Result result) {
        if (config == null || config.verbose) {
            System.out.println(message);
            result.prettyPrint(System.out, "   ");
        }
    }

    /**
     * Prints a message and a stacktrace, provided that verbose is true or unset.
     *
     * @param message the message to print.
     * @param e the exception for which to print a stacktrace.
     */
    public static void println(String message, Exception e) {
        System.out.println(message);

        if (e != null) {
            e.printStackTrace(System.out);
        }
    }
}
