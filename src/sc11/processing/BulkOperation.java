package sc11.processing;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.io.File;

import ibis.constellation.Activity;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class BulkOperation extends Activity {

    /** Generated */
    private static final long serialVersionUID = 8629453481490936053L;

    private final Master parent;
    private final long id;

    private final String in;
    private final String filetype;
    private final String out;

    private final ScriptDescription [] sd;

    private Result [] results;
    private int resultCount = 0;
    private Result error;
    private boolean done;

    public BulkOperation(Master parent, long id, String in, String filetype, 
            ScriptDescription [] sd, String out) {

        super(new UnitActivityContext("master", id), true, true);

        this.parent = parent;
        this.id = id;
        this.in = in;
        this.filetype = filetype;
        this.out = out;
        this.sd = sd;
    }

    public long getID() {
        return id;
    }

    public synchronized boolean success() {
        return (error == null);
    }

    public synchronized Result getResult() {

        if (done) {
            if (error != null) {
                return error;
            }

            return Result.merge(results);
        }

        if (results == null) {
            return new Result().setState("INIT");
        }

        return new Result().setState("PROCESSING: " +
                resultCount + " / " + results.length);
    }

    @Override
    public void cancel() throws Exception {
        // not used
    }

    private synchronized void error(String message) {
        error = new Result().failed(message);
        done = true;
    }

    private File createOutputFile(File in, File outdir) 
    		throws GATObjectCreationException { 
    	return GAT.createFile(outdir.toGATURI() + "/" + in.getName());
    }
    
    @Override
    public void initialize() throws Exception {

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

                File [] tmp = (File[]) input.listFiles();

                if (tmp == null || tmp.length == 0) {
                    error("No input files in: " + in);
                    finish();
                    return;
                }

                for (int i=0;i<tmp.length;i++) {
                	
                	if (!tmp[i].getName().endsWith(filetype)) { 
                		System.out.println("Skipping (wrong name): " + tmp[i]);
                    } else if (!tmp[i].isFile()) { 
                		System.out.println("Skipping (directory): " + tmp[i]);
                	} else if (!tmp[i].canRead()) { 
                		System.out.println("Skipping (unreadable): " + tmp[i]);
                	} else { 
                		submissions++;
                		executor.submit(new Operation(identifier(),
                				(id << 8) | i, tmp[i], sd, 
                				createOutputFile(tmp[i], output)));
                	}
                }
            } else {
             	if (!input.getName().endsWith(filetype)) { 
            		System.out.println("Skipping (wrong name): " + input);
                } else if (!input.isFile()) { 
            		System.out.println("Skipping (directory): " + input);
            	} else if (!input.canRead()) { 
            		System.out.println("Skipping (unreadable): " + input);
            	} else { 
            		submissions++;
            		executor.submit(new Operation(identifier(), (id << 8), 
            				input, sd, createOutputFile(input, output)));
            	}
            }

            if (submissions > 0) { 
        		results = new Result[submissions];
                suspend();
            } else { 
            	finish();
            }

        } catch (Exception e) {
            error("Unexpected error: " + e.getMessage());
            finish();
        }
    }

    private synchronized boolean addResult(Result res) {
        results[resultCount++] = res;

        if (resultCount == results.length) {
            done = true;
            return true;
        }

        return false;
    }

    @Override
    public void process(Event e) throws Exception {
        if (!addResult((Result) e.data)) {
            suspend();
        } else {
            finish();
        }
    }

    @Override
    public void cleanup() throws Exception {
        parent.done(this);
    }
}
