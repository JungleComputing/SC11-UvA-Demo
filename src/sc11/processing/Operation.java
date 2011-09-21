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

    private static final int STATE_INIT     = 0;
    private static final int STATE_COPY_IN  = 1;
    private static final int STATE_FILTER   = 2;
    private static final int STATE_COPY_OUT = 3;
    private static final int STATE_DONE     = 4;
    private static final int STATE_ERROR    = 99;

    private final ActivityIdentifier parent;
    private final long rank;

    private final Result [] results = new Result[3];;

    private final ScriptDescription [] sd;

    private final File in;

    private final String inputName;

    private final String filetype;

    private final File outDir;

    private File out;

    private Script [] ops;

    private String [] tmpFiles;

    private int state = STATE_INIT;

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

        txt = "OPERATION(" + inputName +")";
    }

    // generate a unique tmp name.
    private String generateTempFile(String clean, int count, String ext) {
        return "TMP-" + count + "-" + clean + ext;
    }

    // get the extension of a file.
    private String getFileExtension(String filename) {

        int index = filename.lastIndexOf('.');

        if (index <= 0) {
            return "";
        } else {
            return filename.substring(index);
        }
    }

    // get the filename without extension.
    private String getFileNameWithoutExtension(String filename) {

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

        boolean success = (state != STATE_ERROR);

        if (success) {
            error = "OK";
        }

        executor.send(new Event(identifier(), parent, new Result(txt, success, error, time, results)));

        // Cleanup temp files
        cleanupTmp();

        LocalConfig.println(txt + ": " + error + " " + time);
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

        state = STATE_ERROR;
        error = "ERROR: " + message;
        //LocalConfig.println(txt + " " + error, e);
    }

    /** 
     * The initial run of this activity.  
     * 
     * First, the input file is checked, after which the necessary tmp file names are generated for each stage of the processing.
     * Next, the initial activity is submitted; copying the input file to a local temporary. 
     * 
     * The Operation then suspends to wait for the result.
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

            String cleanFileName = getFileNameWithoutExtension(in.getName());
            String cleanExt = getFileExtension(in.getName());

            if (sd == null || sd.length == 0) {

                LocalConfig.println(txt + ": COPY only");

                this.ops = null;
                tmpFiles = new String[1];
                tmpFiles[0] = generateTempFile(cleanFileName, 0, cleanExt);
                out = GAT.createFile(outDir.toGATURI() + "/" + in.getName());

            } else {
                LocalConfig.println(txt + ": COPY + FILTERS");

                tmpFiles = new String[sd.length+1];

                String currentExt = cleanExt;

                tmpFiles[0] = generateTempFile(cleanFileName, 0, cleanExt);

                for (int i=0;i<sd.length;i++) {

                    String ins = sd[i].inSuffix;
                    String outs = sd[i].outSuffix;

                    if (!ins.equals("*") && !currentExt.equalsIgnoreCase(ins)) {
                        error("Script output mismatch! "+ currentExt + " != " + ins);
                        finish();
                        return;
                    }

                    if (outs.equals("*")) {
                        tmpFiles[i+1] = generateTempFile(cleanFileName, i+1, currentExt);
                    } else {
                        tmpFiles[i+1] = generateTempFile(cleanFileName, i+1, outs);
                        currentExt = outs;
                    }
                }

                out = GAT.createFile(outDir.toGATURI() + "/" + cleanFileName + currentExt);

                ops = new Script[sd.length];

                for (int i=0;i<sd.length;i++) {
                    ops[i] = sd[i].createScript(tmpFiles[i], tmpFiles[i+1], rank);
                }
            }

            state = STATE_COPY_IN;

            File tmp = GAT.createFile("file:///" + LocalConfig.getTmpDir() + File.separator + tmpFiles[0]);

            LocalConfig.println(txt + ": Submitting COPY( " + in +    " -> " + tmp + ")");

            executor.submit(new Copy(identifier(), rank, in, tmp));

            suspend();

        } catch (Exception e) {
            error(txt + " Got exception: " + e.getMessage(), e);
            finish();
        } finally {
            time += System.currentTimeMillis() - start;
            LocalConfig.println(txt + ": Init took " + time);
        }
    }

    /** 
     * Process an event (a result from one of the submitted activities). 
     * 
     * Depending on this result (OK or ERROR) and the current stage in the processing pipeline, the next processing step will be 
     * submitted, or the result will be returned to the parent.  
     */
    @Override
    public void process(Event e) {

        long start = System.currentTimeMillis();

        try {
            LocalConfig.println(txt + ": Received event");

            Result res = (Result) e.data;

            switch (state) {
            case STATE_COPY_IN:

                results[0] = res;

                if (res.isSuccess()) {
                    if (ops != null && ops.length > 0) {

                        LocalConfig.println(txt + ": Submitting SEQUENCE(" + inputName + ") " + Arrays.toString(ops));

                        state = STATE_FILTER;
                        executor.submit(new Sequence(identifier(), rank, inputName, ops));
                        suspend();

                    } else {
                        state = STATE_COPY_OUT;

                        File tmp = GAT.createFile("file:///" + LocalConfig.getTmpDir() + File.separator +
                                tmpFiles[tmpFiles.length-1]);

                        LocalConfig.println(txt + ": Submitting COPY(" + tmp + " -> " + out + ")");

                        executor.submit(new Copy(identifier(), rank, tmp, out));
                        suspend();
                    }
                } else {
                    error("Failed to copy input file!");
                    finish();
                }
                break;

            case STATE_FILTER:

                results[1] = res;

                if (res.isSuccess()) {

                    state = STATE_COPY_OUT;

                    File tmp = GAT.createFile("file:///" + LocalConfig.get().tmpdir + File.separator + tmpFiles[tmpFiles.length-1]);

                    LocalConfig.println(txt + ": Submitting COPY_OUT " + tmp + " -> " + out);

                    executor.submit(new Copy(identifier(), rank, tmp, out));

                    suspend();
                } else {
                    error("Failed to execute filter!");
                    finish();
                }
                break;

            case STATE_COPY_OUT:

                results[2] = res;

                if (res.isSuccess()) {
                    LocalConfig.println(txt + ": Done");
                    state = STATE_DONE;
                    finish();
                } else {
                    error("Failed to copy output file!");
                    finish();
                }
                break;

            default:
                error("Operation in illegal state! " + state);
                finish();
            }

        } catch (Exception ex) {
            error("Got exception!", ex);
            finish();
        } finally {
            time += System.currentTimeMillis() - start;
        }
    }
}
