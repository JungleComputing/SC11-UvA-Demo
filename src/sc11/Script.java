package sc11;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import ibis.constellation.ActivityContext;
import ibis.util.RunProcess;

public class Script implements Serializable {

    /** Generated */
    private static final long serialVersionUID = -5809701669382342293L;

    public final ActivityContext context;

    public final String script;
    public final String input;
    public final String output;

    public Script(String script, String input, String output, ActivityContext context) {

        super();
        this.script = script;
        this.input = input;
        this.output = output;
        this.context = context;
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
            r.failed(new String(p.getStdout()), new String(p.getStderr()));
        } else {
              r.success(new String(p.getStdout()), new String(p.getStderr()));
        }

        return r;
    }
}
