package sc11.shared;

import java.io.Serializable;

public class Result implements Serializable {
    
	/** Generated */
    private static final long serialVersionUID = -8163724771958330288L;

    private boolean finished = false;
    private boolean success = false;

    private String state = "UNKNOWN";
    private String output = "";
    private String error = "";

    public synchronized Result setState(String state) {
        this.state = state;
        return this;
    }

    public Result failed(String error) {
        return failed("", error);
    }

    public synchronized Result failed(String output, String error) {
        finished = true;
        state = "ERROR";
        this.output = output;
        this.error = error;

        return this;
    }

    public Result success(String output) {
        return success(output, "");
    }

    public synchronized Result success(String output, String error) {
        finished = true;
        success = true;
        state = "DONE";
        this.output = output;
        this.error = error;

        return this;
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

    public synchronized Result copy(Result other) {
    	finished = other.finished;
    	success = other.success;
        state = other.state;
        output = other.output;
        error = other.output;
        return this;
    }
    
    @Override
	public String toString() {
		return "Result [finished=" + finished + ", success=" + success
				+ ", state=" + state + ", output=" + output + ", error="
				+ error + "]";
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
