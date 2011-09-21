package sc11.processing;

import java.util.Arrays;
import java.util.StringTokenizer;

import ibis.constellation.ActivityContext;
import ibis.constellation.context.OrActivityContext;
import ibis.constellation.context.UnitActivityContext;

/**
 * A generic description of a script, containing an abstract name, the script name, the in and output file extension (may be a 
 * wildcard), and a list of tags needed to create the Constellation context for running the script.  
 * 
 * @author jason@cs.vu.nl
 */
public class ScriptDescription {

    public final String operation;
    public final String script;
    public final String inSuffix;
    public final String outSuffix;
    public final String [] tags;

    /** 
     * Parse a ScriptDescription from a single line of text.
     * 
     * @param line the input line.
     * @return the ScriptDescription.
     * @throws Exception if the input line could not be parsed.
     */
    public static ScriptDescription parseScriptDescription(String line) throws Exception {

        StringTokenizer tok = new StringTokenizer(line);

        if (tok.countTokens() < 3) {
            throw new Exception("Illegal ScriptDescription!");
        }

        String op = tok.nextToken();
        String script = tok.nextToken();

        String in = tok.nextToken();
        String out = tok.nextToken();

        int c = tok.countTokens();

        String [] tags = new String[c];

        for (int i=0;i<c;i++) {
            tags[i] = tok.nextToken();
        }

        return new ScriptDescription(op, tags, in, out, script);
    }

    /** 
     * Create a ScriptDescription.
     * 
     * @param op the abstract name for the operation.
     * @param tags the tags needed to generate the Constellation context.
     * @param in the input file extension.
     * @param out the output file extension.
     * @param script the script.
     */
    public ScriptDescription(String op, String [] tags, String in, String out, String script) {
        this.operation = op;
        this.tags = tags;
        this.inSuffix = in;
        this.outSuffix = out;
        this.script = script;
    }

    /** 
     * Create a specific instance of a script by adding an input and output file and a rank. 
     * 
     * @param input the name of the input file.
     * @param output the name of the output file.
     * @param rank the rank (using in the Constellation context).
     * @return a {@link Script} that can be executed. 
     */
    public Script createScript(String input, String output, long rank) {

        ActivityContext context = null;

        if (tags.length == 1) {
            context = new UnitActivityContext(tags[0], rank);
        } else {
            UnitActivityContext [] c = new UnitActivityContext[tags.length];

            for (int i=0;i<tags.length;i++) {
                c[i] = new UnitActivityContext(tags[i], rank);
            }

            context = new OrActivityContext(c);
        }

        return new Script(script, input, output, inSuffix, outSuffix, context);
    }

    @Override
    public String toString() {
        return "ScriptDescription [operation=" + operation + ", script="
                + script + ", inSuffix=" + inSuffix +", outSuffix=" + outSuffix
                +", tags=" + Arrays.toString(tags) + "]";
    }
}
