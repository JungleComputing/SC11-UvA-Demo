package sc11.daemon;

import java.util.ArrayList;


public class Client {

    public static void main(String [] args) {

        String host = null;
        int port = 45678;

        String input = null;
        String output = null;
        String filetype = null;
        
        ArrayList<String> filters = new ArrayList<String>();

        for (int i=0;i<args.length;i++) {

            if (args[i].equals("--input")) {
                input = args[++i];
            } else if (args[i].equals("--output")) {
                output = args[++i];
            } else if (args[i].equals("--filetype")) {
                filetype = args[++i];
            } else if (args[i].equals("--filter")) {
                filters.add(args[++i]);
            } else if (args[i].equals("--server")) {
                host = args[++i];
            } else if (args[i].equals("--port")) {
                port = Integer.parseInt(args[++i]);
            } else {
                System.err.println("Unknown parameter: " + args[i]);
                System.exit(1);
            }
        }

        if (input == null) {
            System.err.println("Input not set!");
            System.exit(1);
        }

        if (output == null) {
            System.err.println("Output not set!");
            System.exit(1);
        }

        if (filetype == null) {
            System.err.println("File type not set!");
            System.exit(1);
        }
        
        if (host == null) {
            System.err.println("Host not set!");
            System.exit(1);
        }

        if (port <= 0 || port > 65535) {
            System.err.println("Illegal port: " + port);
            System.exit(1);
        }

        try {
            String [] operations = filters.toArray(new String[filters.size()]);

            Stub s = new Stub(host, port);
            long id = s.exec(input, filetype, operations, output);

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
