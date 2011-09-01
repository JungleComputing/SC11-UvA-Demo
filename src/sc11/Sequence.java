package sc11;

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

    private int index = 0;

    public Sequence(ActivityIdentifier parent, Script [] sequence) {
        super(UnitActivityContext.DEFAULT, true);

        if (sequence == null || sequence.length == 0) {
            throw new IllegalArgumentException("Illegal sequence");
        }

        this.parent = parent;
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
        executor.submit(new Job(sequence[0], identifier()));
        suspend();
    }
    
    @Override
    public void process(Event e) throws Exception {
        results[index] = (Result) e.data;

        if (!results[index].success() || index == sequence.length-1) {
            executor.send(new Event(identifier(), parent, Result.merge(results)));
            finish();
        } else {
            executor.submit(new Job(sequence[++index], identifier()));
            suspend();
        }
    }
}
