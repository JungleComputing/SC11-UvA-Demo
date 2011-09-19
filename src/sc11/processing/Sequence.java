package sc11.processing;

import sc11.shared.Result;
import ibis.constellation.Activity;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class Sequence extends Activity {

    /** Generated */
    private static final long serialVersionUID = 4880988228971533182L;

    private final ActivityIdentifier parent;

    private final Script [] sequence;
    private final Result [] results;

    private String txt;
    
    private int index = 0;

    public Sequence(ActivityIdentifier parent, long id, String inputName, Script [] sequence) {
        super(new UnitActivityContext("slave", id), true);

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

    @Override
    public void initialize() throws Exception {
        executor.submit(new Job(identifier(), sequence[0]));
        suspend();
    }

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
