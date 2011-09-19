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
import sc11.shared.Result;

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
        	LocalConfig.println("MASTER: Creating executor with context \"" + executors[i] + "\"");
        	
            e[i] = new SimpleExecutor(master, master,
                    new UnitWorkerContext(executors[i]), st, st, st);
        }

        constellation = ConstellationFactory.createConstellation(e);
        constellation.activate();        
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

    public void done(BulkOperation o, Result r) {

        BulkOperation tmp;

        synchronized (this) {
            tmp = active.remove(o.getID());

            if (tmp == null) {
                LocalConfig.println("MASTER: Failed to find operation: " + o.getID());
                return;
            }

            terminated.put(o.getID(), o);
        }

        LocalConfig.println("MASTER: BulkOperation " + o.getID() + " terminated:\n" + r);
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
                
                LocalConfig.println("MASTER: Adding filter script " + scripts[i]);
            }
        } else { 
         	LocalConfig.println("MASTER: No filter scripts added.");
        }

        BulkOperation o = new BulkOperation(this, id, fs.inputDir, fs.inputSuffix, scripts, fs.outputDir);

        LocalConfig.println("MASTER: Submit BulkOperation(" + id + ", " + fs.inputDir + ", " + fs.inputSuffix + ", " + 
        		fs.outputDir + ")");
        
        submit(o);

        return id;
    }

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
}


