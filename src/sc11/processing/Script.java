package sc11.processing;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import sc11.shared.Result;

import ibis.constellation.ActivityContext;
import ibis.util.RunProcess;

/** 
 * This class represents a specific instance of a script that needs to be executed. 
 * 
 * Unlike {@link ScriptDescription}, which contains a generic description of a script, this class represents a specific instance 
 * of a script, including the names of its in and output files, and the desired output file extension. 
 * 
 * @author jason@cs.vu.nl
 *
 */
public class Script implements Serializable {

    /** Generated */
    private static final long serialVersionUID = -5809701669382342293L;

    public final ActivityContext context;

    public final String script;
    public final String input;
    public final String output;
    public final String inSuffix;
    public final String outSuffix;

    /** 
     * Creates a new Script instance. 
     * 
     * @param script the script to execute.
     * @param input the input file name.
     * @param output the output file name.
     * @param inSuffix the input file extension.
     * @param outSuffix the output file extension.
     * @param context the context in which this script needs to run. 
     */
    public Script(String script, String input, String output, String inSuffix, String outSuffix, ActivityContext context) {
        super();
        this.script = script;
        this.input = input;
        this.output = output;
        this.inSuffix = inSuffix;
        this.outSuffix = outSuffix;
        this.context = context;
    }

    // Converts the stdout and stderr produced by the script into a single string. 
    private String output(RunProcess p) {

    	String out = new String(p.getStdout()).trim();
    	String err = new String(p.getStderr()).trim();

    	String result = "";
    	
    	if (out.length() > 0) { 
    		result = "stdout: " + out + " ";
    	}

    	if (err.length() > 0) { 
    		result += "stderr: " + err;
    	}
    	
    	return result; 
    }
    
    /** 
     * Executes the script and returns the result.
     * 
     * @return the result of executing the script.
     */
    public Result execute() {

    	LocalConfig config = LocalConfig.get();

        String [] command = new String [] {
                config.scriptdir + File.separator + script,
                config.tmpdir + File.separator + input,
                config.tmpdir + File.separator + output,
        };

        String txt = "SCRIPT" + Arrays.toString(command);
                
        LocalConfig.println(txt + ": Executing");
        
        long start = System.currentTimeMillis();
        
        RunProcess p = new RunProcess(command);
        p.run();

        long end = System.currentTimeMillis();        
        
        boolean ok = (p.getExitStatus() == 0);
        
        LocalConfig.println(txt + ": " + (ok ? "OK ":"ERROR ") + (end-start));
        
        return new Result(txt, ok, (ok ? "OK ":"ERROR ") + output(p), (end-start));
    }

    @Override
    public String toString() {
        return "Script [context=" + context + ", script=" + script + ", input="
                + input + ", output=" + output + "]";
    }
}
