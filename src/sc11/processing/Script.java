package sc11.processing;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import sc11.shared.Result;

import ibis.constellation.ActivityContext;
import ibis.util.RunProcess;

public class Script implements Serializable {

    /** Generated */
    private static final long serialVersionUID = -5809701669382342293L;

    public final ActivityContext context;

    public final String script;
    public final String input;
    public final String output;
    public final String inSuffix;
    public final String outSuffix;

    public Script(String script, String input, String output, String inSuffix,
            String outSuffix, ActivityContext context) {
        super();
        this.script = script;
        this.input = input;
        this.output = output;
        this.inSuffix = inSuffix;
        this.outSuffix = outSuffix;
        this.context = context;
    }

    private String output(RunProcess p) {

    	String out = new String(p.getStdout()).trim();
    	String err = new String(p.getStderr()).trim();

    	String result = "";
    	
    	if (out.length() > 0) { 
    		result = "stdout: " + out + " ";
    	}

    	if (err.length() > 0) { 
    		result = "stderr: " + err;
    	}
    	
    	return result; 
    }
    
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
        
        LocalConfig.println(txt + ": OK" + (end-start));
        
        return new Result(txt, p.getExitStatus() == 0, output(p), (end-start));
    }

    @Override
    public String toString() {
        return "Script [context=" + context + ", script=" + script + ", input="
                + input + ", output=" + output + "]";
    }
}
