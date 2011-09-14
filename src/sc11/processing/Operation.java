package sc11.processing;

import org.gridlab.gat.GAT;
import org.gridlab.gat.io.File;

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

    private final File in;
    private final File out;

    private final String firstTmp;
    private final String lastTmp;

    private final Script [] ops;
    private final Result [] results;

    private int state = STATE_INIT;

    public Operation(ActivityIdentifier parent, long id, File in,
            ScriptDescription [] sd, File out) {

        super(new UnitActivityContext("master", id), true, true);

        this.parent = parent;
        this.id = id;

        this.in = in;
        this.out = out;

        this.results = new Result[3];

        if (sd == null || sd.length == 0) {
            this.ops = null;
            this.firstTmp = this.lastTmp = generateTempFile(in.getName(), 0);
        } else {
            String [] tmp = new String[sd.length+1];

            for (int i=0;i<sd.length+1;i++) {
                tmp[i] = generateTempFile(in.getName(), i);
            }

            firstTmp = tmp[0];
            lastTmp = tmp[tmp.length-1];

            ops = new Script[sd.length];

            for (int i=0;i<sd.length;i++) {
                ops[i] = sd[i].createScript(tmp[i], tmp[i+1], id);
            }
        }
    }

    private String generateTempFile(String filename, int count) {
        return "TMP-" + id + "-" + count + "-" + filename;
    }

    @Override
    public void cancel() throws Exception {
        // Ignored
    }

    @Override
    public void cleanup() throws Exception {
        executor.send(new Event(identifier(), parent, Result.merge(results)));
    }

    @Override
    public void initialize() throws Exception {

        System.out.println("Operation " + id + " starting");

        state = STATE_COPY_IN;

        File tmp = GAT.createFile("file:///" + LocalConfig.get().tmpdir + 
        		File.separator + firstTmp);
        
        System.out.println("Operation " + id + " submitting COPY_IN " + in + 
        		" -> " + tmp);
        
        executor.submit(new Copy(identifier(), id, in, tmp));

        suspend();
    }

    @Override
    public void process(Event e) throws Exception {

        System.out.println("Operation " + id + " received event");

        Result res = (Result) e.data;

        switch (state) {
        case STATE_COPY_IN:

            results[0] = res;

            if (res.success()) {

                System.out.println("Operation " + id + " submitting SEQUENCE");

                if (ops != null && ops.length > 0) { 
                	state = STATE_FILTER;
                    executor.submit(new Sequence(identifier(), id, ops));
                	suspend();
                } else { 
                	state = STATE_COPY_OUT;

                    File tmp = GAT.createFile("file:///" + 
                    		LocalConfig.get().tmpdir + File.separator + lastTmp);
                    
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

            if (res.success()) {
                
                state = STATE_COPY_OUT;

                File tmp = GAT.createFile("file:///" + 
                		LocalConfig.get().tmpdir + File.separator + lastTmp);
                
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
