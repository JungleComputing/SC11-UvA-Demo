package sc11.processing;

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

    public Sequence(ActivityIdentifier parent, long id, Script [] sequence) {
        super(new UnitActivityContext("slave", id), true);

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
        executor.submit(new Job(identifier(), sequence[0]));
        suspend();
    }

    @Override
    public void process(Event e) throws Exception {
        results[index] = (Result) e.data;

        if (!results[index].success() || index == sequence.length-1) {

            System.out.println("Sequence done!");

            executor.send(new Event(identifier(), parent, Result.merge(results)));
            finish();
        } else {
            index++;
            System.out.println("Sequence submitting " + (index+1));
            executor.submit(new Job(identifier(), sequence[index]));
            suspend();
        }
    }
}
