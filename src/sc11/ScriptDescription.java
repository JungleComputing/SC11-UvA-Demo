package sc11;

import java.util.StringTokenizer;

import ibis.constellation.ActivityContext;
import ibis.constellation.context.OrActivityContext;
import ibis.constellation.context.UnitActivityContext;

public class ScriptDescription {

    public final String operation;
    public final String script;

    public final ActivityContext context;

    public static ScriptDescription parseScriptDescription(String line)
            throws Exception {

        StringTokenizer tok = new StringTokenizer(line);

        if (tok.countTokens() < 3) {
            throw new Exception("Illegal ScriptDescription!");
        }

        String op = tok.nextToken();
        String script = tok.nextToken();

        ActivityContext context = null;

        int c = tok.countTokens();

        if (c == 1) {
            context = new UnitActivityContext(tok.nextToken());
        } else {
            UnitActivityContext [] cs = new UnitActivityContext[c];

            for (int i=0;i<c;i++) {
                cs[i] = new UnitActivityContext(tok.nextToken());
            }

            context = new OrActivityContext(cs, true);
        }

        return new ScriptDescription(op, script, context);
    }

    public ScriptDescription(String op, String script, ActivityContext c) {
        this.operation = op;
        this.script = script;
        this.context = c;
    }

    public Script createScript(String input, String output) {
        return new Script(script, input, output, context);
    }
}
