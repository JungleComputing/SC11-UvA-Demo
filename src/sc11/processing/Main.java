package sc11.processing;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.util.rpc.RPC;

import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import sc11.daemon.DaemonInterface;
import sc11.shared.FilterSequence;
import sc11.shared.Result;

/**
 * This is the main application's main class.
 *
 *
 * @author jason@cs.vu.nl
 *
 */
public class Main {

    private static final IbisCapabilities capabilities = new IbisCapabilities(IbisCapabilities.ELECTIONS_STRICT);

    // Parse the executor config provided in "config".
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
            LocalConfig.println("Failed to parse executor list!");
            LocalConfig.println("    " + config);
            System.exit(1);
        }


        return executors.toArray(new String[executors.size()]);
    }

    // Retrieves a property from the Properties argument, printing an error and exiting if it is not found.
    private static String getProperty(Properties p, String name) {

        String tmp = p.getProperty(name);

        if (tmp == null) {
            System.err.println("No " + name + " property specified!");
            System.exit(1);
        }

        return tmp;
    }

    /**
     * The main method of the main class.
     *
     * This method retrieves various configuration properties set on the command line, and then starts a Master or Slave,
     * depending on this configuration.
     *
     * @param args the command line arguments (not used!).
     */
    public static void main(String [] args) {

        Properties p = System.getProperties();

        String config    = getProperty(p, "sc11.config");
        String tmpdir    = getProperty(p, "sc11.tmpDir");
        String scriptdir = getProperty(p, "sc11.scriptDir");
        String jobid     = getProperty(p, "sc11.ID");
        String master    = getProperty(p, "ibis.constellation.master");
        String adres     = getProperty(p, "ibis.server.address");
        String verbose   = getProperty(p, "sc11.verbose");

        boolean isMaster = Boolean.parseBoolean(master);
        boolean isVerbose = Boolean.parseBoolean(verbose);

        long id = Long.parseLong(jobid);

        String [] executors = parseExecutorConfig(getProperty(p, "sc11.executors"));

        // Store some 'global' configuration
        LocalConfig.set(isVerbose, tmpdir, scriptdir);

        LocalConfig.println("Starting sc11.processing.Main ---");

        try {

            if (isMaster) {

                // The master contacts the Deamon using Ibis RPC to retrieve the FilterSequence that needs to be processed.
                LocalConfig.println("Creating Ibis ContactClient using server: " + adres);

                // Create an Ibis for the RPC.
                Properties prop = new Properties();
                prop.put("ibis.server.address", adres);
                prop.put("ibis.pool.name", "SC11-ContactServer");

                Ibis myIbis = IbisFactory.createIbis(capabilities, prop, false, null, RPC.rpcPortTypes);

                IbisIdentifier server = null;

                while (server == null) {
                    LocalConfig.println("Attempting to find ContactServer....");
                    server = myIbis.registry().getElectionResult("ContactServer", 1000);
                }

                // Create an RPC Proxy to remote object
                DaemonInterface daemon = RPC.createProxy(DaemonInterface.class, server, "ContactServer", myIbis);

                LocalConfig.println("Ibis ContactClient created!");

                // Retrieve the work.
                FilterSequence f = null;

                try {
                    f = daemon.getWork(id);
                } catch (Exception e) {
                    System.err.println("Failed to retrieve work from Daemon!");
                }

                // Start the master (always do this, even if we have no work -- needed for termination of slaves).
                Master m = new Master(executors, config);

                if (f != null) {
                    // Hand over the work to the master.
                    long pid = m.exec(f);

                    // Start polling the master for the output
                    Result res = null;

                    do {
                        try {
                            // Poll every second.
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            // ignored
                        }

                        // Get the current status.
                        res = m.info(pid);

                        // Forward the status to the daemon.
                        daemon.setStatus(id, res);
                    } while (!res.isFinished());

                    // Inform the damon that we are done.
                    daemon.done(id);
                }

                // Terminate the master (and slaves).
                m.done();

                // Terminate the Ibis RPC connection.
                myIbis.end();
            } else {
                // Start a slave.
                new Slave(executors).run();
            }
       } catch (Exception e) {
           LocalConfig.println("Processing failed!", e);
       }
    }
}
