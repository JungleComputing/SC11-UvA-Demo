package sc11.client;

import java.util.ArrayList;

import sc11.shared.FilterSequence;
import sc11.shared.Stub;

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
            
            Stub s = new Stub(daemon, port);
            long id = s.exec(job);

            String result = null;

            while (result == null) {
                try {
                    Thread.sleep(100);
                    result = s.info(id);
                } catch (Exception e) {
                    result = "Operation failed: " + e.getMessage();
                }
            }

            System.out.println("Result was:\n" + result);

            s.close();

        } catch (Exception e) {
            System.err.println("Client failed!");
            e.printStackTrace(System.err);
        }
    }
}
