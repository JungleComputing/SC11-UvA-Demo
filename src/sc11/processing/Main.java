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

public class Main {

    private static final IbisCapabilities capabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT);

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

    private static String getProperty(Properties p, String name) {

        String tmp = p.getProperty(name);

        if (tmp == null) {
            LocalConfig.println("No " + name + " property specified!");
            System.exit(1);
        }

        return tmp;
    }

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
                LocalConfig.println("Creating Ibis ContactClient using server: " + adres);

                Properties prop = new Properties();
                prop.put("ibis.server.address", adres);
                prop.put("ibis.pool.name", "SC11-ContactServer");

                Ibis myIbis = IbisFactory.createIbis(capabilities, prop, false, null, RPC.rpcPortTypes);

                IbisIdentifier server = null;

                while (server == null) {
                    LocalConfig.println("Attempting to find ContactServer....");
                    server = myIbis.registry().getElectionResult("ContactServer", 1000);
                }

                // Create proxy to remote object
                DaemonInterface daemon = RPC.createProxy(DaemonInterface.class, server, "ContactServer", myIbis);

                LocalConfig.println("Ibis ContactClient created!");

                Master m = new Master(executors, config);

                FilterSequence f = daemon.getWork(id);

                long pid = m.exec(f);

                Result res = null;

                do {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        // ignored
                    }

                    res = m.info(pid);

                    daemon.setStatus(id, res);
                } while (!res.isFinished());

                daemon.done(id);
                myIbis.end();

                m.done();
           } else {
                Slave s = new Slave(executors);
                s.run();
            }
       } catch (Exception e) {
    	   LocalConfig.println("Processing failed!", e);
       }
    }
}
