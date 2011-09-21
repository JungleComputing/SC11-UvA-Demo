package sc11.processing;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;

/**
 * This activity is responsible for executing a script.
 *
 * @author jason@cs.vu.nl
 */
public class Job extends SimpleActivity {

    /** Generated */
    private static final long serialVersionUID = 6562124688034850389L;

    private final Script script;

    /**
     * Creates an activity responsible for executing a script.
     *
     * @param parent the activity to send the result to.
     * @param script the script to execute.
     */
    public Job(ActivityIdentifier parent, Script script) {
        super(parent, script.context, true);
        this.script = script;
    }

    /**
     * When activated, this activity simply calls script.execute and returns the result.
     */
    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, script.execute()));
    }
}
