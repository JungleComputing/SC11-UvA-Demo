package sc11;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;
import ibis.constellation.context.UnitActivityContext;

public class Copy extends SimpleActivity {

	private final String in;
	private final String out;
	
	public Copy(ActivityIdentifier parent, String in, String out) {
		super(parent, new UnitActivityContext("master"), true);
	
		this.in = in;
		this.out = out;
	}

	/** Generated */
	private static final long serialVersionUID = -6441545366616186882L;

	private Result copy() { 
		
		System.out.println("TODO: copy file " + in + " to " + out);
		
		Result r = new Result();
		r.success("hiephoi", "");
		return r;
	}
	
	@Override
	public void simpleActivity() throws Exception {
		executor.send(new Event(identifier(), parent, copy()));
	}
}
