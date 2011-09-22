package sc11.processing;

import java.util.Arrays;

import org.gridlab.gat.GAT;
import org.gridlab.gat.io.File;

import sc11.shared.Result;

import ibis.constellation.Activity;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

/**
 * This Activity represents all operations that need to be performed on a single input file.
 *
 * Generally, these operations will consist of copying the (remote) input file to a local temporary, applying several scripts,
 * and copying the output to a (remote) destination.
 *
 * The goal of this class is to create separate activities for each of these steps, and coordinate their execution.
 *
 * @author jason@cs.vu.nl
 *
 */
public class Operation extends Activity {

    /** Generated */
    private static final long serialVersionUID = -5895535272566557335L;

    private final ActivityIdentifier parent;
    private final long rank;

    private final Result [] results = new Result[3];

    private final ScriptDescription [] sd;

    private final File in;

    private final String inputName;
    private final String filetype;

    private final String cleanFileName;
    private final String cleanExt;

    private final File outDir;

    private File out;

    private Script [] ops;

    private String [] tmpFiles;

    private long time;
    private String error;

    private final String txt;

    /**
     * Creates an operation class to coordinate all operations needed to process the "in" file.
     *
     * @param parent the activity to send the result to.
     * @param rank the rank of this operation (to enable in-order processing).
     * @param in the input file (possibly remote).
     * @param filetype the required file extension.
     * @param sd the sequence of scripts that need to be applied.
     * @param outDir the output directory (possibly remote).
     * @throws Exception
     */
    public Operation(ActivityIdentifier parent, long rank, File in, String filetype, ScriptDescription [] sd, File outDir)
            throws Exception {

        super(new UnitActivityContext("master", rank), true, true);

        this.parent = parent;
        this.rank = rank;
        this.in = in;

        this.inputName = in.getName();

        this.filetype = filetype;
        this.sd = sd;
        this.outDir = outDir;

        cleanFileName = getFileNameWithoutExtension(in.getName());
        cleanExt = getFileExtension(in.getName());

        txt = "OPERATION(" + inputName +")";
    }

    // generate a unique tmp name.
    private String generateTempFile(int count, String ext) {
        return "TMP-" + count + "-" + cleanFileName + ext;
    }

    private String generateTempFileNames() {

        if (sd == null || sd.length == 0) {
            tmpFiles = new String[1];
            tmpFiles[0] = generateTempFile(0, cleanExt);
            return cleanExt;
        }

        tmpFiles = new String[sd.length+1];

        String currentExt = cleanExt;

        tmpFiles[0] = generateTempFile(0, cleanExt);

        for (int i=0;i<sd.length;i++) {

            String ins = sd[i].inSuffix;
            String outs = sd[i].outSuffix;

            if (!ins.equals("*") && !currentExt.equalsIgnoreCase(ins)) {
                error("Script output mismatch! "+ currentExt + " != " + ins);
                return null;
            }

            if (outs.equals("*")) {
                tmpFiles[i+1] = generateTempFile(i+1, currentExt);
            } else {
                tmpFiles[i+1] = generateTempFile(i+1, outs);
                currentExt = outs;
            }
        }

        return currentExt;
    }

    // get the extension of a file.
    private static String getFileExtension(String filename) {

        int index = filename.lastIndexOf('.');

        if (index <= 0) {
            return "";
        } else {
            return filename.substring(index);
        }
    }

    // get the filename without extension.
    private static String getFileNameWithoutExtension(String filename) {

        int index = filename.lastIndexOf('.');

        if (index <= 0) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    @Override
    public void cancel() throws Exception {
        // Ignored
    }

    // removes any tmp files that are created
    private void cleanupTmp() {

        if (tmpFiles == null) {
            return;
        }

        for (int i=0;i<tmpFiles.length;i++) {

            String file = LocalConfig.getTmpDir() + File.separator + tmpFiles[i];

            try {
                new java.io.File(file).delete();
            } catch (Exception e) {
                System.err.println("Failed to delete: " + file);
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * On termination, the result is send to the parent and any tmp files are removed.
     */
    @Override
    public void cleanup() throws Exception {
        // Send result to parent.
        executor.send(new Event(identifier(), parent, new Result(txt, true, "DONE", time, results)));

        // Cleanup temp files if needed.
        cleanupTmp();

        LocalConfig.println(txt + ": DONE" + time);
    }

    // Check if the input file adheres to all requirements.
    private boolean checkInput() {

        if (!in.getName().endsWith(filetype)) {
            error("Skipping (wrong name): " + in);
            return false;
        } else if (!in.isFile()) {
            error("Skipping (directory): " + in);
            return false;
        } else if (!in.canRead()) {
            error("Skipping (unreadable): " + in);
            return false;
        }

        return true;
    }

    // Print an error message.
    private void error(String message) {
        error(message, null);
    }

    // Print an error message and stacktrace.
    private void error(String message, Exception e) {
        error = "ERROR: " + message;
        //LocalConfig.println(txt + " " + error, e);
    }

    // Performs the actual copy.
    private Result copy(File in, File out) {

        String txt = "COPY(" + in + " -> " + out + ")";

        try {
            long start = System.currentTimeMillis();

            in.copy(out.toGATURI());

            long end = System.currentTimeMillis();

            LocalConfig.println(txt + ": OK " + (end-start));
            return new Result(txt, true, "OK" , (end-start));
        } catch (Exception e) {
            LocalConfig.println(txt + ": ERROR " + e.getMessage(), e);
            return new Result(txt, false, "ERROR " + e.getMessage(), 0);
        }
    }

    // Copies the last tmp file to the output location.
    private Result copyOutput() {

        try {
            File tmp = GAT.createFile("file:///" + LocalConfig.get().tmpdir + File.separator + tmpFiles[tmpFiles.length-1]);
            return copy(tmp, out);
        } catch (Exception e) {
            LocalConfig.println(txt + " ERROR: Copy to output failed", e);
            return new Result(txt, false, "ERROR: Copy to output failed! " + e.getMessage(), 0);
        }
    }

    // Copies the last input file to the first tmp location.
    private Result copyInput() {

        try {
            File tmp = GAT.createFile("file:///" + LocalConfig.getTmpDir() + File.separator + tmpFiles[0]);
            return copy(in, tmp);
        } catch (Exception e) {
            LocalConfig.println(txt + " ERROR: Copy to output failed", e);
            return new Result(txt, false, "ERROR: Copy to output failed! " + e.getMessage(), 0);
        }
    }

    /**
     * The initial run of this activity.
     *
     * First, the input file is checked, after which the necessary tmp file names are generated for each stage of the processing.
     * Next, the input file is copied.
     *
     * If any processing is required, the Sequence is submitted and this operations suspends.
     *
     * Else, this operation copies the output file to the destination and finishes.
     */
    @Override
    public void initialize() {

        long start = System.currentTimeMillis();

        try {
            LocalConfig.println(txt + ": Starting");

            if (!checkInput()) {
                finish();
                return;
            }

            String lastExt = generateTempFileNames();

            if (lastExt == null) {
                LocalConfig.println(txt + " ERROR: Failed to generate tmp files!");
                results[0] = new Result(txt, false, "ERROR: Failed to generate tmp files!", 0);
                finish();
                return;
            }

            LocalConfig.println(txt + ": COPYING Input");

            results[0] = copyInput();

            if (results[0].isError()) {
                finish();
                return;
            }

            if (sd != null && sd.length > 0) {
                // We also need to process.
                LocalConfig.println(txt + ": CREATING Filters");

                ops = new Script[sd.length];

                for (int i=0;i<sd.length;i++) {
                    ops[i] = sd[i].createScript(tmpFiles[i], tmpFiles[i+1], rank);
                }

                executor.submit(new Sequence(identifier(), rank, inputName, ops));
                suspend();

            } else {
                // We only need to copy.
                results[2] = copyOutput();
                finish();
            }

        } finally {
            time += System.currentTimeMillis() - start;
            LocalConfig.println(txt + ": Init took " + time);
        }
    }

    /**
     * Process an event (the result from the sequence).
     *
     * If this result was successful, the last temp file will be copied to the output location.
     */
    @Override
    public void process(Event e) {

        long start = System.currentTimeMillis();

        LocalConfig.println(txt + ": Received sequence result.");

        results[1] = (Result) e.data;

        if (results[1].isSuccess()) {
            results[2] = copyOutput();
        }

        finish();

        time += System.currentTimeMillis() - start;
    }
}
