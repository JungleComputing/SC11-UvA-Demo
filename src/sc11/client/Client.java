package sc11.client;

import java.util.ArrayList;

import sc11.daemon.DaemonStub;
import sc11.shared.FilterSequence;
import sc11.shared.Result;

/**
 * Simple example Client that uses a {@link DaemonStub} to execute a {@link FilterSequence} (specified on the command line) on
 * a remote Daemon.
 *
 * @author jason@cs.vu.nl
 */
public class Client {

     public static void main(String [] args) {

        String daemon = null;
        int port = 54672;

        String inputURI = null;
        String outputURI = null;
        String inputFileType = null;
        String site = null;

        int nodes = -1;

        ArrayList<String> filters = new ArrayList<String>();

        for (int i=0;i<args.length;i++) {

            if (args[i].equals("--daemon")) {
                daemon = args[++i];
            } else if (args[i].equals("--port")) {
                port = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--inputURI")) {
                inputURI = args[++i];
            } else if (args[i].equals("--outputURI")) {
                outputURI = args[++i];
            } else if (args[i].equals("--inputSuffix")) {
                inputFileType = args[++i];
            } else if (args[i].equals("--filter")) {
                filters.add(args[++i]);
            } else if (args[i].equals("--site")) {
                site = args[++i];
            } else if (args[i].equals("--nodes")) {
                nodes = Integer.parseInt(args[++i]);
            } else {
                System.err.println("Unkown option: " + args[i]);
                System.exit(1);
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

        if (inputURI == null) {
            System.err.println("InputURI not set!");
            System.exit(1);
        }

        if (outputURI == null) {
            System.err.println("OutputURI not set!");
            System.exit(1);
        }

        if (inputFileType == null) {
            System.err.println("InputSuffix not set!");
            System.exit(1);
        }

        try {
            String [] tmp = filters.toArray(new String[filters.size()]);

            FilterSequence job = new FilterSequence(inputURI, inputFileType, outputURI, tmp, site, nodes);

            System.out.println("Read FilterSequence: " + job);

            DaemonStub s = new DaemonStub(daemon, port);
            long id = s.exec(job);

            boolean done = false;

            Result result = null;

            String oldMessage = null;

            while (!done) {
                try {
                    Thread.sleep(1000);
                    result = s.info(id);

                    done = result.isFinished();

                    if (!done) {

                        if (oldMessage == null || !oldMessage.equals(result.message)) {
                            oldMessage = result.message;
                            System.out.println("Current state: " + result.message);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Operation failed: " + e.getMessage());
                    done = true;
                }
            }

            if (result != null) {
                System.out.println("DONE -- Result:");
                result.prettyPrint(System.out);
            }

            s.close();

        } catch (Exception e) {
            System.err.println("Client failed!");
            e.printStackTrace(System.err);
        }
    }
}
