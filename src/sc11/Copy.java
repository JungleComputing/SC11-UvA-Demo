package sc11;

import org.gridlab.gat.io.File;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;
import ibis.constellation.context.UnitActivityContext;

public class Copy extends SimpleActivity {

    private final File in;
    private final File out;

    public Copy(ActivityIdentifier parent, File in, File out) {
        super(parent, new UnitActivityContext("master"), true);

        this.in = in;
        this.out = out;
    }

    /** Generated */
    private static final long serialVersionUID = -6441545366616186882L;

    private Result copy() {

        Result r = new Result();

        try {
            in.copy(out.toGATURI());
            r.success();
        } catch (Exception e) {
            r.failed(e.getMessage());
        }

        return r;
    }

    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, copy()));
    }
}
