package sc11.processing;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.io.File;

import sc11.shared.Result;

import ibis.constellation.Activity;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

/**
 * This activity represents 'bulk operation' responsible for processing all files with a given suffix in a given input directory.
 *
 * A BulkOperation starts with check the access of both the in and output directories. Next, the list of files in the input
 * directory is retrieved and a new {@link Operation} is created for each file. Each {@link Operation} is responsible for the
 * processing of a single input file.
 *
 * Note that the BulkOperation does not check the suffix or accessibility of each of the input files, as these may be expensive
 * remote operations. Performing these sequentially inside this BulkOperation would significantly reduce performance. Instead,
 * it is up to each {@link Operation} to perform these tasks before starting.
 *
 * Note that this BulkOperation is a 'restricted to local' activity. As such, it can not leave the machine on which it was
 * created. It can freely move between cores however.
 *
 * @author jason@cs.vu.nl
 */
public class BulkOperation extends Activity {

    /** Generated */
    private static final long serialVersionUID = 8629453481490936053L;

    private final Master parent;
    private final long id;

    private final String in;
    private final String filetype;
    private final String out;

    private final String txt;

    private final ScriptDescription [] sd;

    private Result [] results;
    private int resultCount = 0;

    private String error;
    private long time;
    private boolean done;

    /**
     * Creates BulkOperation responsible for processing all files with a given suffix in a given input directory.
     *
     * @param parent the Master that submitted this BulkOperation.
     * @param id a unique ID for this activity.
     * @param in the input URI.
     * @param filetype the input suffix.
     * @param sd an array of {@link ScriptDescription}s describing the filters to be applied.
     * @param out the output URI.
     */
    public BulkOperation(Master parent, long id, String in, String filetype, ScriptDescription [] sd, String out) {

        super(new UnitActivityContext("master", id), true, true);

        this.parent = parent;
        this.id = id;
        this.in = in;
        this.filetype = filetype;
        this.out = out;
        this.sd = sd;

        txt = "BULK(" + in + " -> " + out + ")";
    }

    /**
     * Retrieve the unique ID of this BulkOperation.
     *
     * @return the unique ID of this BulkOperation.
     */
    public long getID() {
        return id;
    }

    /**
     * Returns if the BulkOperation has successfully completed.
     *
     * @return if the BulkOperation has successfully completed.
     */
    public synchronized boolean success() {
        return (error == null);
    }

    /**
     * Returns the current execution state of the BulkOperation.
     *
     * @return the current execution state of the BulkOperation.
     */
    public synchronized Result getResult() {

        if (done) {
            if (error != null) {
                return new Result(txt, false, error, 0, results);
            }

            return new Result(txt, true, "OK", time, results);
        }

        if (results == null) {
            return new Result(txt, "INITIALIZING");
        }

        return new Result(txt, "PROCESSING: " + resultCount + " / " + results.length);
    }

    @Override
    public void cancel() throws Exception {
        // not used
    }

    // Prints and error on stdout.
    private void error(String message) {
        error(message, null);
    }

    // Prints and error (and stack trace) on stdout.
    private synchronized void error(String message, Exception e) {

        error = "ERROR: " + message;
        done = true;

        LocalConfig.println(txt + ": " + error, e);
    }

    /**
     * Initial run of the BulkOperation activity.
     *
     * Here, the in and output URIs are checked. Next, a list of input files is generated and an {@link Operation} is submitted
     * for each input file.
     *
     * Finally, the BulkOperation suspends and waits for the results of the submitted {@link Operation}s.
     */
    @Override
    public void initialize() throws Exception {

        LocalConfig.println(txt + "Started.");

        long startInit = System.currentTimeMillis();

        try {
            // Check if input exists
            File input = GAT.createFile(in);

            if (!input.exists() || !input.canRead()) {
                error("Cannot read input: " + in);
                finish();
                return;
            }

            // Check if output exists
            File output = GAT.createFile(out);

            if (!output.exists() || !output.isDirectory() || !output.canWrite()) {
                error("Cannot write to output directory: " + out);
                finish();
                return;
            }

            int submissions = 0;

            if (input.isDirectory()) {

                if (output.isFile()) {
                    error("Cannot copy directory to file: " + in + " -> " + out);
                    finish();
                    return;
                }

                LocalConfig.println(txt + "Retrieving file list.");

                long start = System.currentTimeMillis();

                File [] tmp = (File[]) input.listFiles();

                long end = System.currentTimeMillis();

                LocalConfig.println(txt + "File list retrieved in " + ((end-start)/1000.0) + " sec.");

                if (tmp == null || tmp.length == 0) {
                    error("No input files in: " + in);
                    finish();
                    return;
                }

                for (int i=0;i<tmp.length;i++) {

                    if (tmp[i].getName().endsWith(filetype)) {
                        submissions++;
                        executor.submit(new Operation(identifier(),
                                (id << 8) | i, tmp[i], filetype, sd, output));
                    } else {
                        error("Skipping (wrong name): " + in);
                    }
                }
            } else {
                if (input.getName().endsWith(filetype)) {
                    submissions++;
                    executor.submit(new Operation(identifier(), (id << 8),
                            input, filetype, sd, output));
                } else {
                    error("Skipping (wrong name): " + in);
                }
            }

            if (submissions > 0) {
                results = new Result[submissions];
                suspend();
            } else {
                finish();
            }

        } catch (Exception e) {
            error("Got Exception: " + e.getMessage(), e);
            finish();
        } finally {
            time += System.currentTimeMillis() - startInit;
        }
    }

    // Add a result to the result list. Returns if more results are expected.
    private synchronized boolean addResult(Result res) {

        LocalConfig.println(txt + " GOT RESULT " + resultCount + " (expecting " + results.length + "): ", res);

        results[resultCount++] = res;

        if (resultCount == results.length) {
            done = true;
            return true;
        }

        return false;
    }

    /**
     * Process an incoming event (a result from one of the {@link Operation}s).
     *
     * Suspends if more results are expected, finishes if this was the final result.
     */
    @Override
    public void process(Event e) throws Exception {
        if (!addResult((Result) e.data)) {
            suspend();
        } else {
            finish();
        }
    }

    /**
     * At termination, we notify our parent that we are finished, and provide the (combined) result.
     */
    @Override
    public void cleanup() throws Exception {
        parent.done(this, getResult());
    }
}
