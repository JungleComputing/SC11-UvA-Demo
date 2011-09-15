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

    private String cleanup(byte [] output) {
        return new String(output).trim();
    }

    public Result execute() {

        LocalConfig config = LocalConfig.get();

        String [] command = new String [] {
                config.scriptdir + File.separator + script,
                config.tmpdir + File.separator + input,
                config.tmpdir + File.separator + output,
        };

        System.out.println("Executing: " + Arrays.toString(command));

        RunProcess p = new RunProcess(command);
        p.run();

        System.out.println("Done: " + Arrays.toString(command));

        Result r = new Result();

        if (p.getExitStatus() != 0) {
            r.failed(cleanup(p.getStdout()), cleanup(p.getStderr()));
        } else {
            r.success(cleanup(p.getStdout()), cleanup(p.getStderr()));
        }

        return r;
    }

    @Override
    public String toString() {
        return "Script [context=" + context + ", script=" + script + ", input="
                + input + ", output=" + output + "]";
    }
}
