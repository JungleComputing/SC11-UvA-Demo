package sc11.processing;

import ibis.constellation.Constellation;
import ibis.constellation.ConstellationFactory;
import ibis.constellation.Executor;
import ibis.constellation.SimpleExecutor;
import ibis.constellation.StealPool;
import ibis.constellation.StealStrategy;
import ibis.constellation.context.UnitWorkerContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import sc11.shared.FilterSequence;

//import sc11.daemon.Proxy;

public class Master {

    private final Constellation constellation;

    private final HashMap<String, ScriptDescription> descriptions =
            new HashMap<String, ScriptDescription>();

    private final HashMap<Long, BulkOperation> active =
            new HashMap<Long, BulkOperation>();

    private final HashMap<Long, BulkOperation> terminated =
            new HashMap<Long, BulkOperation>();

    private long id = 0;

    private boolean done = false;

    public Master(String [] executors, String filterConfig) throws Exception {

        readFilterConfig(filterConfig);
        
        StealPool master = new StealPool("master");
        StealStrategy st = StealStrategy.SMALLEST;

        Executor [] e = new Executor[executors.length];

        for (int i=0;i<executors.length;i++) {
            e[i] = new SimpleExecutor(master, master,
                    new UnitWorkerContext(executors[i]), st, st, st);
        }

        constellation = ConstellationFactory.createConstellation(e);
    }

    private synchronized long getID() {
        return (id++) << 16;
    }
    
    private void readFilterConfig(String file) throws Exception {

        BufferedReader r = new BufferedReader(new FileReader(new File(file)));

        String tmp = r.readLine();

        while (tmp != null) {

            tmp = tmp.trim();

            if (tmp.length() > 0) {
                ScriptDescription s = ScriptDescription.parseScriptDescription(tmp);
                descriptions.put(s.operation, s);
            }

            tmp = r.readLine();
        }

        r.close();
    }

    private void submit(BulkOperation o) {
        synchronized (this) {
            active.put(o.getID(), o);
        }

        constellation.submit(o);
    }

    public void done(BulkOperation o) {

        BulkOperation tmp;

        synchronized (this) {
            tmp = active.remove(o.getID());

            if (tmp == null) {
                System.err.println("Failed to find operation: " + o.getID());
                return;
            }

            terminated.put(o.getID(), o);
        }

        System.out.println("Operation " + o.getID() + " terminated:\n" +
                o.getResult().getOuput());
    }

    public Result info(long id) throws Exception {

        BulkOperation o = null;

        synchronized (this) {

            o = active.get(id);

            if (o == null) {
                o = terminated.remove(id);
            }
        }

        if (o == null) {
            throw new Exception("Operation " + id + " not found!");
        }

        return o.getResult();
    }

    public long exec(FilterSequence fs) throws Exception {

        long id = getID();

        ScriptDescription [] scripts = null;

        if (fs.filters != null && fs.filters.length > 0) {

            scripts = new ScriptDescription[fs.filters.length];

            for (int i=0;i<fs.filters.length;i++) {

                scripts[i] = descriptions.get(fs.filters[i]);

                if (scripts[i] == null) {
                    throw new Exception("Operation not found: " + fs.filters[i]);
                }
            }
        }

        BulkOperation o = new BulkOperation(this, id, fs.inputDir, 
        		fs.inputSuffix, scripts, fs.outputDir);

        //Operation o = new Operation(this, id, in, scripts, out);

        submit(o);

        return id;
    }

    /*
    public int [] exec(String [] in, String [] ops, String [] out) {

        // Various sanity checks
        if (in == null || in.length == 0) {
            throw new IllegalArgumentException("Illegal in list: " + in);
        }

        if (out == null || out.length == 0) {
            throw new IllegalArgumentException("Illegal out list: " + in);
        }

        if (in.length != out.length) {
            throw new IllegalArgumentException("Mismatch between in and out " +
                    "list: " + in.length + " != " + out.length);
        }


        // Execute the operations for each of the input files
        for (int i=0;i<in.length;i++) {
            exec(in[i], ops, out[i]);
        }
    }
    */

    public synchronized void done() {
        constellation.done();
        done = true;
        notifyAll();
    }

    public synchronized void waitUntilDone() {

        while (!done) {
            try {
                wait();
            } catch (Exception e) {
                // ignored
            }
        }
    }
    
    public void run(Job job) { 
    	try { 
            constellation.activate();
            
    		
    		
    	} catch (Exception e) {
			// TODO: handle exception
		}
    }
    

/*
    public static void main(String [] args) {

        try {
            int port = 45678;
            int executorCount = 1;
            String config = null;
            String tmpdir = null;
            String scriptdir = null;

            for (int i=0;i<args.length;i++) {

                if (args[i].startsWith("--exec")) {
                    executorCount = Integer.parseInt(args[++i]);
                } else if (args[i].startsWith("--config")) {
                    config = args[++i];
                } else if (args[i].startsWith("--scriptdir")) {
                    scriptdir = args[++i];
                } else if (args[i].startsWith("--tmpdir")) {
                    tmpdir = args[++i];
              //  } else if (args[i].startsWith("--port")) {
              //     port = Integer.parseInt(args[++i]);
                } else {
                    System.err.println("Unknown option " + args[i]);
                    System.exit(1);
                }
            }

            if (executorCount <= 0) {
                System.err.println("Illegal executor count: " + executorCount);
                System.exit(1);
            }

            if (config == null) {
                System.err.println("No configuration specified!");
                System.exit(1);
            }

            if (tmpdir == null) {
                System.err.println("No tmpdir specified!");
                System.exit(1);
            }

            if (scriptdir == null) {
                System.err.println("No scriptdir specified!");
                System.exit(1);
            }

            if (port <= 0 || port > 65535) {
                System.err.println("Illegal port specified! " + port);
                System.exit(1);
            }

            LocalConfig.set(tmpdir, scriptdir);

            Master m = new Master(executorCount, config);

            Proxy p = new Proxy(m, port);
            p.start();
            m.waitUntilDone();

        } catch (Exception e) {
            System.err.println("Master failed!");
            e.printStackTrace(System.err);
        }
    }
*/    
}


