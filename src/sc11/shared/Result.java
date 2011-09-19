package sc11.shared;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;

public class Result implements Serializable {

	/** Generated */
    private static final long serialVersionUID = -8163724771958330288L;

	private static final byte STATE_RUNNING = 1;
	private static final byte STATE_DONE    = 2;
	private static final byte STATE_ERROR   = 3;
    
    private final byte state;
    public final long time;
    public final String message;        
    
    public final Result [] sub; 
    
    public Result(String message) {    	
    	this.message = message;
    	state = STATE_RUNNING;
    	time = 0;
    	sub = null;    	
    }
        
    public Result(Result other) {
    	this.state = other.state;
    	this.message = other.message;
    	this.time = other.time;
    	this.sub = other.sub;    			
    }
    
    public Result(boolean success, String message, long time, Result [] sub) {    	
    	this.sub = sub;
    	
    	byte s = STATE_DONE;
    	long t = time;
    	
    	if (!success) { 
    		s = STATE_ERROR;
    	}
    	
    	this.message = message;
    	
    	if (sub != null) {    		
    		for (int i=0;i<sub.length;i++) { 
    			if (sub[i] != null && sub[i].state == STATE_ERROR) { 
    				s = STATE_ERROR;
    			}
    			
    			t += sub[i].time;
    		}    		
    	}
    	    	
    	this.time = t;
    	this.state = s;
    }
    
    public Result(boolean success, String message, long time) {
    	this(success, message, time, null);
    }
    
    public Result(boolean success, String message) {
    	this(success, message, 0, null);
    }
    
    
    public boolean isFinished() {
        return (state == STATE_DONE || state == STATE_ERROR);
    }
    
    public boolean success() {
        return (state == STATE_DONE);
    }
    
    public String state() { 
    	switch (state) { 
    	case STATE_RUNNING: 
    		return "RUNNING";
    	case STATE_DONE:
    		return "DONE";
    	case STATE_ERROR:
    		return "ERROR";    		
    	default:
    		return "UNKNOWN";
    	}
    }
    
    @Override
    public String toString() {
        return "Result [state=" + state + ", message=" + message + ", time=" + time + "sub=" + Arrays.toString(sub) + "]";
    }

    private void prettyPrint(PrintStream out, String prefix) {
		out.println(prefix + state() + " / " + time + " / " + message);
		
		if (sub == null) { 
			return;
		}
		
		for (int i=0;i<sub.length;i++) {			
			if (sub[i] == null) {
				out.println(prefix + "  - empty -");
			} else {  
				prettyPrint(out, prefix + "  ");
			}
		}
    }
    
	public void prettyPrint(PrintStream out) {
		out.print("Result / ");
		prettyPrint(out, "");
	}
}
