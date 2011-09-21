package sc11.processing;

import sc11.shared.Result;
import ibis.constellation.Activity;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

/**
 * This Activity represents a sequence of Scripts to be applied to a file.
 *
 * @author jason@cs.vu.nl
 */

public class Sequence extends Activity {

    /** Generated */
    private static final long serialVersionUID = 4880988228971533182L;

    private final ActivityIdentifier parent;

    private final Script [] sequence;
    private final Result [] results;

    private String txt;

    private int index = 0;

    /** 
     * Create a new sequence activity that applies a series of scripts to the input file. 
     * @param parent the activity to send the result to.
     * @param rank the rank of this sequence.
     * @param inputName the name of the input file.
     * @param sequence the sequence of scripts to apply.
     */ 
    public Sequence(ActivityIdentifier parent, long rank, String inputName, Script [] sequence) {
        super(new UnitActivityContext("slave", rank), true);

        if (sequence == null || sequence.length == 0) {
            throw new IllegalArgumentException("Illegal sequence");
        }

        this.parent = parent;
        this.txt = "SEQUENCE(" + inputName + ")";
        this.sequence = sequence;
        this.results = new Result[sequence.length];
    }

    @Override
    public void cancel() throws Exception {
        // ignored
    }

    @Override
    public void cleanup() throws Exception {
        // ignored
    }
    
    /**
     * In the initial run of the sequence activity, the first script in the sequence is submitted.
     */
    @Override
    public void initialize() throws Exception {
        executor.submit(new Job(identifier(), sequence[0]));
        suspend();
    }

    /** 
     * Processes event (results of scripts submitted earlier).
     * 
     * Depending on the result and the position in the processing pipeline, the next sequence is submitted, 
     * or the result is send to the parent.
     */ 
    @Override
    public void process(Event e) throws Exception {
        Result tmp = (Result) e.data;

        results[index] = tmp;

        boolean ok = tmp.isSuccess();

        if (!ok || index == sequence.length-1) {
            LocalConfig.println(txt + ": " + (ok ? "OK" : "ERROR"));
            executor.send(new Event(identifier(), parent, new Result(txt, ok, (ok ? "OK" : "ERROR"), 0, results)));
            finish();
        } else {
            index++;
            LocalConfig.println(txt + ": Submitting stage " + (index+1));
            executor.submit(new Job(identifier(), sequence[index]));
            suspend();
        }
    }
}
