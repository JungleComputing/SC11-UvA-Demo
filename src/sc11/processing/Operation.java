package sc11.processing;

import java.util.Arrays;

import org.gridlab.gat.GAT;
import org.gridlab.gat.io.File;

import sc11.shared.Result;

import ibis.constellation.Activity;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class Operation extends Activity {

    private static final int STATE_INIT     = 0;
    private static final int STATE_COPY_IN  = 1;
    private static final int STATE_FILTER   = 2;
    private static final int STATE_COPY_OUT = 3;
    private static final int STATE_DONE     = 4;
    private static final int STATE_ERROR    = 99;

    /** Generated */
    private static final long serialVersionUID = -5895535272566557335L;

    private final ActivityIdentifier parent;
    private final long id;

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

    public Operation(ActivityIdentifier parent, long id, File in,
            String filetype, ScriptDescription [] sd, File outDir) throws Exception {

        super(new UnitActivityContext("master", id), true, true);

        this.parent = parent;
        this.id = id;
        this.in = in;

        this.inputName = in.getName();

        this.filetype = filetype;
        this.sd = sd;
        this.outDir = outDir;

        txt = "OPERATION(" + inputName +")";
    }

    private String generateTempFile(String clean, int count, String ext) {
        return "TMP-" + count + "-" + clean + ext;
    }

    private String getFileExtension(String filename) {

        int index = filename.lastIndexOf('.');

        if (index <= 0) {
            return "";
        } else {
            return filename.substring(index);
        }
    }

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

    private void error(String message) {
        error(message, null);
    }

    private void error(String message, Exception e) {

        state = STATE_ERROR;
        error = "ERROR: " + message;
        //LocalConfig.println(txt + " " + error, e);
    }

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
                    ops[i] = sd[i].createScript(tmpFiles[i], tmpFiles[i+1], id);
                }
            }

            state = STATE_COPY_IN;

            File tmp = GAT.createFile("file:///" + LocalConfig.getTmpDir() + File.separator + tmpFiles[0]);

            LocalConfig.println(txt + ": Submitting COPY( " + in +    " -> " + tmp + ")");

            executor.submit(new Copy(identifier(), id, in, tmp));

            suspend();

        } catch (Exception e) {
            error(txt + " Got exception: " + e.getMessage(), e);
            finish();
        } finally {
            time += System.currentTimeMillis() - start;
            LocalConfig.println(txt + ": Init took " + time);
        }
    }

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
                        executor.submit(new Sequence(identifier(), id, inputName, ops));
                        suspend();

                    } else {
                        state = STATE_COPY_OUT;

                        File tmp = GAT.createFile("file:///" + LocalConfig.getTmpDir() + File.separator +
                                tmpFiles[tmpFiles.length-1]);

                        LocalConfig.println(txt + ": Submitting COPY(" + tmp + " -> " + out + ")");

                        executor.submit(new Copy(identifier(), id, tmp, out));
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

                    executor.submit(new Copy(identifier(), id, tmp, out));

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
