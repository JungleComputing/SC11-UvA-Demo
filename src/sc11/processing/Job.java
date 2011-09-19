package sc11.processing;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;

public class Job extends SimpleActivity {

    /** Generated */
    private static final long serialVersionUID = 6562124688034850389L;

    private final Script script;

    public Job(ActivityIdentifier parent, Script script) {
        super(parent, script.context, true);
        this.script = script;
    }

    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, script.execute()));
    }
}
