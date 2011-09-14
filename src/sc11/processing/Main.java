package sc11.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringTokenizer;

import sc11.shared.FilterSequence;
import sc11.shared.Result;

public class Main {
    
	private static String [] parseExecutorConfig(String config) { 
		
		StringTokenizer tok = new StringTokenizer(config, ",");
		
		ArrayList<String> executors = new ArrayList<String>();
		
		while (tok.hasMoreTokens()) { 
			
			int count = 1;
			
			String t = tok.nextToken().trim();
			
			int index = t.indexOf(':');
			
			if (index > 0) { 
				count = Integer.parseInt(t.substring(index+1)); 
				t = t.substring(0, index);
			}
	
			for (int i=0;i<count;i++) { 
				executors.add(t);
			}
		}
	
		if (executors.size() == 0) { 
			System.err.println("Failed to parse executor list!");
			System.err.println("    " + config);
			System.exit(1);
		}
		
		
		return executors.toArray(new String[executors.size()]);
	}
	
	public static void main(String [] args) {        

		System.out.println("Starting sc11.processing.Main ---");	       
     
        Properties p = System.getProperties();
        
        String config = p.getProperty("sc11.config");

        if (config == null) {
            System.err.println("No sc11.conf property specified!");
            System.exit(1);
        }

        String tmpdir = p.getProperty("sc11.tmpDir");

        if (tmpdir == null) {
            System.err.println("No sc11.tmpDir property specified!");
            System.exit(1);
        }

        String scriptdir = p.getProperty("sc11.scriptDir");
        
        if (scriptdir == null) {
            System.err.println("No sc11.scriptDir property specified!");
            System.exit(1);
        }
        
        /* For debugging */
		System.out.println("Command line args: " + Arrays.toString(args));
        System.out.println("  gat.adaptor.path=" + p.getProperty("gat.adaptor.path"));
        /* End debugging */
        
        String [] executors = parseExecutorConfig(p.getProperty("sc11.executors"));
		
        // Store some 'global' configuration
        LocalConfig.set(tmpdir, scriptdir);

        try { 
        
        	if (args.length > 0 && args[0].equals("--master")) { 
        
        		FilterSequence f = FilterSequence.fromArguments(args);
            
        		Master m = new Master(executors, config);
        		
        		long id = m.exec(f);
        		
        		Result res = null;
        		
        		do { 
        			try { 
        				Thread.sleep(100);
        			} catch (Exception e) {
						// ignored
					}
        		
        			res = m.info(id);
        			
        			System.out.println("Current state: " + res.getState());
        			
        		} while (!res.finished());
        
        		m.done();
        		
           } else { 
        		Slave s = new Slave(executors);
        		s.run();
        	} 
       } catch (Exception e) {
    	   System.err.println("Processing failed!");
    	   e.printStackTrace(System.err);
       }
    }
}
