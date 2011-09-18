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
    private final String filetype;
        
    private final File outDir;

    private File out;

    private Script [] ops;

    private String [] tmpFiles;
    
    private int state = STATE_INIT;

    private long created;
    private long started;
    private long copyInDone;
    private long processingDone;
    private long copyOutDone;
    
    public Operation(ActivityIdentifier parent, long id, File in,
            String filetype, ScriptDescription [] sd, File outDir) throws Exception {

        super(new UnitActivityContext("master", id), true, true);

        created = System.currentTimeMillis();
        
        this.parent = parent;
        this.id = id;
        this.in = in;
        this.filetype = filetype;
        this.sd = sd;
        this.outDir = outDir;
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

    @Override
    public void cleanup() throws Exception {
    	// Send result to parent. 
        executor.send(new Event(identifier(), parent, Result.merge(results)));
        
        // Cleanup temp files
        for (int i=0;i<tmpFiles.length;i++) {        	
        	
        	String file = LocalConfig.get().tmpdir + File.separator + tmpFiles[i];
        	
        	try {         	
        		new java.io.File(file).delete();
        	} catch (Exception e) {
        		System.err.println("Failed to delete: " + file);
        		e.printStackTrace(System.err);
        	}
        }
        
        System.out.println("Operation " + id + " done: " + in.getName() + " " + 
        		(started-created) + " / " + (copyInDone - created) + " / " + 
        		(processingDone-created) + " / " + (copyOutDone-created));
    }

    private boolean checkInput() { 
    	
    	if (!in.getName().endsWith(filetype)) {
        	results[0] = new Result().failed("Skipping (wrong name): " + in);
            System.out.println("Skipping (wrong name): " + in);
            return false;
        } else if (!in.isFile()) {
        	results[0] = new Result().failed("Skipping (directory): " + in);
        	System.out.println("Skipping (directory): " + in);
        	return false;
        } else if (!in.canRead()) {
        	results[0] = new Result().failed("Skipping (unreadable): " + in);            
            System.out.println("Skipping (unreadable): " + in);
            return false;
        } 
    	
    	return true;
    }
    
    @Override
    public void initialize() throws Exception {

    	System.out.println("Operation " + id + " starting");

        started = System.currentTimeMillis();
        
        if (!checkInput()) { 
        	finish();
        	return;
        } 
        
        String cleanFileName = getFileNameWithoutExtension(in.getName());
        String cleanExt = getFileExtension(in.getName());

        if (sd == null || sd.length == 0) {
    
        	System.out.println("Operation " + id + " COPY only");
        	
        	this.ops = null;
            tmpFiles = new String[1];
            tmpFiles[0] = generateTempFile(cleanFileName, 0, cleanExt);            
            out = GAT.createFile(outDir.toGATURI() + "/" + in.getName());
        } else {
        	
        	System.out.println("Operation " + id + " COPY + FILTERS");

            tmpFiles = new String[sd.length+1];

            String currentExt = cleanExt;

            tmpFiles[0] = generateTempFile(cleanFileName, 0, cleanExt);

            for (int i=0;i<sd.length;i++) {

                String ins = sd[i].inSuffix;
                String outs = sd[i].outSuffix;

                if (!ins.equals("*") && !currentExt.equalsIgnoreCase(ins)) {
                    throw new Exception("Script output mismatch! "
                            + currentExt + " != " + ins);
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

        File tmp = GAT.createFile("file:///" + LocalConfig.get().tmpdir +
                File.separator + tmpFiles[0]);

        System.out.println("Operation " + id + " submitting COPY_IN " + in +
                " -> " + tmp);

        executor.submit(new Copy(identifier(), id, in, tmp));

        long time = System.currentTimeMillis();
        
        System.out.println("Operation " + id + " init took " + (time-created));
        
        suspend();
    }

    @Override
    public void process(Event e) throws Exception {

        System.out.println("Operation " + id + " received event");

        Result res = (Result) e.data;

        switch (state) {
        case STATE_COPY_IN:

            results[0] = res;
            
            copyInDone = System.currentTimeMillis();
            
            if (res.success()) {
                if (ops != null && ops.length > 0) {

                    System.out.println("Operation " + id + " submitting SEQUENCE " + Arrays.toString(ops));

                    state = STATE_FILTER;
                    executor.submit(new Sequence(identifier(), id, ops));
                    suspend();

                } else {
                    state = STATE_COPY_OUT;

                    processingDone = copyInDone;
                    
                    File tmp = GAT.createFile("file:///" +
                            LocalConfig.get().tmpdir + File.separator + tmpFiles[tmpFiles.length-1]);

                    System.out.println("Operation " + id + " submitting COPY_OUT "
                            + tmp + " -> " + out);

                    executor.submit(new Copy(identifier(), id, tmp, out));
                    suspend();
                }
            } else {
                state = STATE_ERROR;
                System.out.println("Operation " + id +
                     " FAILED: Failed to copy input file!\n" + res.getError());
                finish();
            }
            break;

        case STATE_FILTER:

            results[1] = res;

            processingDone = System.currentTimeMillis();
            
            if (res.success()) {

                state = STATE_COPY_OUT;

                File tmp = GAT.createFile("file:///" +
                        LocalConfig.get().tmpdir + File.separator + tmpFiles[tmpFiles.length-1]);

                System.out.println("Operation " + id + " submitting COPY_OUT "
                        + tmp + " -> " + out);

                executor.submit(new Copy(identifier(), id, tmp, out));

                suspend();
            } else {
                state = STATE_ERROR;
                System.out.println("Operation " + id +
                       " FAILED: Failed to execute filter!\n" + res.getError());
                finish();
            }
            break;

        case STATE_COPY_OUT:

            results[2] = res;

            if (res.success()) {

            	copyOutDone = System.currentTimeMillis();
            	
                System.out.println("Operation " + id + " DONE");

                state = STATE_DONE;
                finish();
            } else {
                state = STATE_ERROR;
                System.out.println("Operation " + id +
                     " FAILED: Failed to copy output file!\n" + res.getError());
                finish();
            }
            break;

        default:
            state = STATE_ERROR;
            System.out.println("Operation " + id +
                    " FAILED: Operation in illegal state! " + state);
            finish();
        }
    }
}
