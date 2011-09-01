package sc11;

import java.io.Serializable;

public class Result implements Serializable {

	/** Generated */
	private static final long serialVersionUID = -8163724771958330288L;

	private boolean finished = false;
	private boolean success = false;

	private String state = "UNKNOWN";
	private String output = "";
	private String error = "";
	
	public synchronized void setState(String state) { 
		this.state = state;
	}
	
	public synchronized void failed(String output, String error) {
		finished = true;
		state = "ERROR";
		this.output = output;
		this.error = output;
	}
	
	public synchronized void success(String output, String error) {
		finished = true;
		success = true;
		state = "DONE";
		this.output = output;
		this.error = output;
	}
	
	public synchronized boolean finished() {
		return finished;
	}

	public synchronized String getState() {
		return state;
	}

	public synchronized boolean success() {
		return success;
	}

	public synchronized String getOuput() {
		return output;
	}

	public synchronized String getError() {
		return error;
	}

	public static Result merge(Result [] results) { 
    
    	StringBuilder out = new StringBuilder();
    	StringBuilder err = new StringBuilder();
    	
    	Result last = null;
    	
    	for (int i=0;i<results.length;i++) { 
    		
    		if (results[i] != null) { 
    			out.append(results[i].getOuput());
    			out.append("\n");
    		
    			err.append(results[i].getError());
    			err.append("\n");
    			
    			last = results[i];
    		}
    	}
    	
    	Result tmp = new Result();
    	
    	if (last != null && last.success()) { 
    		tmp.success(out.toString(), err.toString());
    	} else { 
    		tmp.failed(out.toString(), err.toString());
    	}
    		
    	return tmp;
    }

	
}
