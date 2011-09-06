package sc11;

import java.util.StringTokenizer;

import ibis.constellation.ActivityContext;
import ibis.constellation.context.OrActivityContext;
import ibis.constellation.context.UnitActivityContext;

public class ScriptDescription {

    public final String operation;
    public final String script;
    public final String [] tags;
   
    public static ScriptDescription parseScriptDescription(String line)
            throws Exception {

        StringTokenizer tok = new StringTokenizer(line);

        if (tok.countTokens() < 3) {
            throw new Exception("Illegal ScriptDescription!");
        }

        String op = tok.nextToken();
        String script = tok.nextToken();

        int c = tok.countTokens();

        String [] tags = new String[c];
        
        for (int i=0;i<c;i++) { 
        	tags[i] = tok.nextToken();
        }
        
        return new ScriptDescription(op, tags, script);
    }

    public ScriptDescription(String op, String [] tags, String script) {
        this.operation = op;
        this.tags = tags;
        this.script = script;
    }

    public Script createScript(String input, String output, long id) {
    	
    	ActivityContext context = null;
    	
    	if (tags.length == 1) { 
    		context = new UnitActivityContext(tags[0], id);
    	} else { 
    		UnitActivityContext [] c = new UnitActivityContext[tags.length];
    		
    		for (int i=0;i<tags.length;i++) { 
    			c[i] = new UnitActivityContext(tags[i], id);
    		}
    
    		context = new OrActivityContext(c);
    	}
    	
        return new Script(script, input, output, context);
    }
}
