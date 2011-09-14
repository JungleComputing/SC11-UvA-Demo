package sc11.client;

import sc11.daemon.DaemonStub;
import sc11.shared.FilterSequence;
import sc11.shared.Result;

public class Client {

    public static void main(String [] args) {

        String daemon = null;
        int port = 54672;

        for (int i=0;i<args.length;i++) {

        	if (args[i].equals("--daemon")) {
                daemon = args[++i];
            } else if (args[i].equals("--port")) {
                port = Integer.parseInt(args[++i]);
            }
        }
        
        if (daemon == null) {
            System.err.println("Host not set!");
            System.exit(1);
        }

        if (port <= 0 || port > 65535) {
            System.err.println("Illegal port: " + port);
            System.exit(1);
        }

        try {        
            FilterSequence job = FilterSequence.fromArguments(args);
        	
            System.out.println("Read FilterSequence: " + job);
            
            DaemonStub s = new DaemonStub(daemon, port);
            long id = s.exec(job);

            boolean done = false;
            
            Result result = null;
            
            while (!done) { 
            	try {
                    Thread.sleep(100);
                    result = s.info(id);                    
                    System.out.println("Current state: " + result.getState());                    
                    done = result.finished();                    
                } catch (Exception e) {
                	System.out.println("Operation failed: " + e.getMessage());
                	done = true;
                }            	
            } 

            if (result != null) { 
            	System.out.println("Result:\n" + result.getOuput() + "\n" + 
            			result.getError());
            }
            
            s.close();

        } catch (Exception e) {
            System.err.println("Client failed!");
            e.printStackTrace(System.err);
        }
    }
}
