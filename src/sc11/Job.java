package sc11;

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

        System.out.println("Created new job with context " + script.context);
    }

    @Override
    public void simpleActivity() throws Exception {

        System.out.println("Executing job " + identifier());

        executor.send(new Event(identifier(), parent, script.execute()));
    }
}
