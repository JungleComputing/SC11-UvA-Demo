package sc11.processing;

import org.gridlab.gat.io.File;

import sc11.shared.Result;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;
import ibis.constellation.context.UnitActivityContext;

public class Copy extends SimpleActivity {

    private final File in;
    private final File out;

    public Copy(ActivityIdentifier parent, long id, File in, File out) {
        super(parent, new UnitActivityContext("master", id), true);

        this.in = in;
        this.out = out;
    }

    /** Generated */
    private static final long serialVersionUID = -6441545366616186882L;

    private Result copy() {

        Result r = new Result();

        try {        	
        	long start = System.currentTimeMillis();        	
            
        	in.copy(out.toGATURI());

            long end = System.currentTimeMillis();        	
            
            r.success("File copied: " + in + " -> " + out + " " + (end-start));
        } catch (Exception e) {
            r.failed("Copy failed: " + in + " -> " + out + "\n" + e.getMessage());
        }

        return r;
    }

    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, copy()));
    }
}
