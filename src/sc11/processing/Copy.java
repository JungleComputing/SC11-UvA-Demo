package sc11.processing;

import org.gridlab.gat.io.File;

import sc11.shared.Result;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;
import ibis.constellation.context.UnitActivityContext;

public class Copy extends SimpleActivity {

	/** Generated */
    private static final long serialVersionUID = -6441545366616186882L;

	private final File in;
    private final File out;

    public Copy(ActivityIdentifier parent, long id, File in, File out) {
        super(parent, new UnitActivityContext("master", id), true);

        this.in = in;
        this.out = out;
    }

    private Result copy() {

    	String txt = "COPY(" + in + " -> " + out + "): ";
    	    	
        try {        	
        	long start = System.currentTimeMillis();        	
            
        	in.copy(out.toGATURI());

            long end = System.currentTimeMillis();        	

            LocalConfig.println(txt + "Done " + (end-start));

            return new Result(true, txt + "Done" , (end-start));
        } catch (Exception e) {        	
        	LocalConfig.println(txt + " Failed", e);
            return new Result(false, txt + "Failed - " + e.getMessage(), 0);
        }
    }

    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, copy()));
    }
}
