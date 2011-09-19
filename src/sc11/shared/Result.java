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
    
	/** Source of this result */
	public final String source;        

	/** Time it took to produce this result */
	public final long time;
	
	/** Output message */
    public final String message;        
    
    /** Array of sub results */
    public final Result [] sub; 
    
    /** 
     * Constructor used to produce intermediate results (with state set to RUNNING).
     * 
     * @param source source of this result.
     * @param message output message (usually the current state in human readable form).
     */
    public Result(String source, String message) {
    	this.source = source;
    	this.message = message;
    	state = STATE_RUNNING;
    	time = 0;
    	sub = null;    	
    }
        
    
    /**
     * Constructor used to produce final results (with state set to DONE or ERROR).
     * 
     * @param source source of this result.
     * @param success should state be set to DONE or ERROR ?
     * @param message output message.
     * @param time time used to produce this result. 
     * @param sub array of sub results (may be null).
     */
    public Result(String source, boolean success, String message, long time, Result [] sub) {
    	
    	this.source = source;
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
    
    /**
     * Constructor used to produce final results (with state set to DONE or ERROR).
     * 
     * @param source source of this result.
     * @param success should state be set to DONE or ERROR ?
     * @param message output message.
     * @param time time used to produce this result. 
     */
    public Result(String source, boolean success, String message, long time) {
    	this(source, success, message, time, null);
    }
    
    /**
     * Constructor used to produce final results (with state set to DONE or ERROR).
     * 
     * @param source source of this result.
     * @param success should state be set to DONE or ERROR ?
     * @param message output message.
     */    
    public Result(String source, boolean success, String message) {
    	this(source, success, message, 0, null);
    }
    
    /**
     * Copy constructor.
     * 
     * @param other object to copy. 
     */
    public Result(Result other) {
    	this.source = other.source;
    	this.state = other.state;
    	this.message = other.message;
    	this.time = other.time;
    	this.sub = other.sub;    			
    }
    
    /** 
     * Check if this is a final result (state is set to DONE or ERROR).
     * 
     * @return if this is a final result. 
     */
    public boolean isFinished() {
        return (state == STATE_DONE || state == STATE_ERROR);
    }
    
    /** 
     * Check if this is a successful result (state is set to DONE).
     * 
     * @return if this is a succesful result. 
     */
    public boolean isSuccess() {
        return (state == STATE_DONE);
    }
    
    /** 
     * Check if this is result represents an error (state is set to ERROR).
     * 
     * @return if this result represents an error. 
     */
    public boolean isError() {
        return (state == STATE_ERROR);
    }
    
    /** 
     * Check if this is an intermediate result (state is set to RUNNING).
     * 
     * @return if this is an intermediate result. 
     */
    public boolean isRunning() {
        return (state == STATE_ERROR);
    }
    
    
    /** 
     * Convert to result state to a human readable form. 
     * 
     * @return a String representing the current result state.  
     */
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
        return "Result [source="+ source + ", state=" + state + ", message=" + message + ", time=" + time + "sub=" + 
        		Arrays.toString(sub) + "]";
    }

    private void prettyPrint(PrintStream out, String prefix) {
		out.println(prefix + source + " / " + state() + " / " + time + " / " + message);
		
		if (sub == null) { 
			return;
		}
		
		for (int i=0;i<sub.length;i++) {			
			if (sub[i] == null) {
				out.println(prefix + "  - empty -");
			} else {  
				sub[i].prettyPrint(out, prefix + "  ");
			}
		}
    }
    
    
    /** 
     * Print the state of this result to the given output stream. 
     * 
     * @param out the stream to print to. 
     */
	public void prettyPrint(PrintStream out) {
		prettyPrint(out, "");
	}
}
