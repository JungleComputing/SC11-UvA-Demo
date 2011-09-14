package sc11.daemon;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import sc11.processing.Result;
import sc11.shared.FilterSequence;

import ibis.deploy.Application;
import ibis.deploy.ApplicationSet;
import ibis.deploy.Cluster;
import ibis.deploy.Deploy;
import ibis.deploy.Experiment;
import ibis.deploy.Grid;
import ibis.deploy.JobDescription;
import ibis.deploy.State;
import ibis.deploy.Workspace;

public class Daemon {

    private final int defaultSize;
    private final String defaultSite;

    private final Deploy deploy;

    // private final GUI gui;

    private final Grid grid;
    private final ApplicationSet applications;
    private final Experiment experiment;

    private long id = 0;

    private class ProcessingJob { 

    	final long id;
    	
    	final ibis.deploy.Job master;
    	final ibis.deploy.Job slaves;
    	
    	ProcessingJob(long id, ibis.deploy.Job master, ibis.deploy.Job slaves) { 
    		this.id = id;
    		this.master = master;
    		this.slaves = slaves;    		
    	}
    }

    private HashMap<Long, ProcessingJob> jobs =
    		new HashMap<Long, ProcessingJob>();
    
    public Daemon(String gridname, int size, String site, boolean verbose)
            throws Exception {

        defaultSize = size;
        defaultSite = site;

        grid = new Grid(new File(gridname));
        experiment = new Experiment("SC11-UvA-Demo");
        applications = new ApplicationSet();

        Workspace workspace = new Workspace(grid, applications, experiment);

        deploy = new Deploy(new File("deploy-workspace"), verbose, false, 0, 
        		null, null, true);

        /*
        if (useGui) {
            gui = new GUI(deploy, workspace, Mode.MONITOR, logos);
        } else {
            gui = null;
        }
         */
    }

    private synchronized long getID() {
        return id++;
    }

    private Application createApplication(String name, String libs, 
    		String config, String tmpDir, String scriptDir, String master, 
    		String executors, String [] args) throws Exception { 
    
    	Application m = applications.getApplication(name);

        if (m == null) {
            m = new Application(name);
            
            m.setLibs(new File("lib/sc11-application-0.2.0.jar"));
            m.setMainClass("sc11.processing.Main");
            m.setMemorySize(1000);
            m.setLog4jFile(new File("log4j.properties"));
       
            m.setSystemProperty("gat.adaptor.path", libs + "JavaGAT-2.1.1" + 
            		File.separator + "adaptors");
                                         
            m.setSystemProperty("ibis.constellation.master", master);
            m.setSystemProperty("sc11.config", config);
            m.setSystemProperty("sc11.tmpDir", tmpDir);
            m.setSystemProperty("sc11.scriptDir", scriptDir);        
                        
            // FIXME: hardcoded executor config!
            m.setSystemProperty("sc11.executors", executors);
            
            // FIXME: hardcoded version numbers!
            m.setJVMOptions("-classpath", 
            		libs + "sc11-application-0.2.0.jar:" +
            		libs + "constellation-0.7.0.jar:" +            		
            		libs + "JavaGAT-2.1.1" + File.separator + "*:" + 
            		libs + "ipl-2.2" + File.separator + "*");

            if (args != null && args.length > 0) { 
            	m.setArguments(args);
            }
            applications.addApplication(m);
        }
    	
        return m;
    }
    
    public long exec(FilterSequence job) throws Exception {

    	// First we get an unique ID.
        long id = getID();
    	
        // Next, we extract some information about the job
        int workers = defaultSize;

        if (job.nodes > 0) {
            workers = job.nodes;
        }

        String site = defaultSite;

        if (job.site != null) {
            site = job.site;
        }

        System.out.println("Daemon executing Job [" + id + "] on " + site + "/" 
        		+ workers + " : " + job);
        
        // Next retrieve the cluster we will run on.
        Cluster cluster = grid.getCluster(site);

        if (cluster == null) {
            throw new Exception("Cluster \"" + site + "\"not found in grid " +
                    "description file.");
        }

        // Get some info from the cluster.              
        String location = cluster.getProperties().getProperty("sc11.location");

        if (location == null) {
            throw new Exception("sc11.location property not set for cluster \""
                    + site + "\" in grid description file.");
        }
        
        String tmpDir = cluster.getProperties().getProperty("sc11.tmp");

        if (tmpDir == null) {
        	tmpDir = location + File.separator + "tmp";
        }
        
        String config = location + File.separator + "scripts" + 
        		File.separator + "configuration";
        
    	String scriptDir = location + File.separator + "scripts";
    
    	String libs = location + File.separator + "lib" + File.separator;
                
        // Next retrieve/create a description of the application.
        ArrayList<String> arg = new ArrayList<String>();

        arg.add("--master");
        arg.add("--inputURI");
        arg.add(job.inputDir);
        arg.add("--inputSuffix");
        arg.add(job.inputSuffix);
        arg.add("--outputURI");
        arg.add(job.outputDir);

        if (job.filters != null && job.filters.length > 0) {
            for (int i=0;i<job.filters.length;i++) {
                arg.add(job.filters[i]);
            }
        }
                
        String [] args = arg.toArray(new String[arg.size()]);            
    	
        Application m = createApplication("SC11-Master", libs, config, tmpDir, 
        		scriptDir, "true", "master", args);
        
        Application s = createApplication("SC11-Slave", libs, config, tmpDir, 
        		scriptDir, "false", "slave:2,gpu", new String [] { "--slave" });
        
        JobDescription jm = new JobDescription("SC11-Master-" + id);
        experiment.addJob(jm);

        jm.getCluster().setName(site);
        jm.setProcessCount(1);
        jm.setResourceCount(1);
        jm.setRuntime(60);
        jm.getApplication().setName("SC11-Master");
        jm.setPoolName("SC11-" + id);
        
        ibis.deploy.Job master = deploy.submitJob(jm, m, cluster, null, null);
        
        JobDescription js = new JobDescription("SC11-Slave-" + id);
        experiment.addJob(js);

        js.getCluster().setName(site);
        js.setProcessCount(workers);
        js.setResourceCount(workers);
        js.setRuntime(60);
        js.getApplication().setName("SC11-Slave");
        js.setPoolName("SC11-" + id);

        jm.getApplication().setArguments(new String[] {"--slave"});
        ibis.deploy.Job slaves = deploy.submitJob(js, s, cluster, null, null);
        
        addJob(new ProcessingJob(id, master, slaves));
        return id;
    }

    private synchronized void addJob(ProcessingJob job) {
        jobs.put(job.id, job);
    }

    private synchronized ProcessingJob getJob(long id) {
        return jobs.get(id);
    }

    private synchronized ProcessingJob removeJob(long id) {
        return jobs.remove(id);
    }

    public Result info(long id) {

    	ProcessingJob job = jobs.get(id);

        if (job == null) {
            return new Result().failed("Unknown job id: " + id);
        }

        State m = job.master.getState();
        State s = job.slaves.getState();
        
        return new Result().setState(m.name() + " | " + s.name());
    }

    public static void fatal(String message) {
        fatal(message, null);
    }

    public static void fatal(String message, Exception e) {
        System.err.println(message);

        if (e != null) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }

        System.exit(1);
    }

    public static void main(String [] args) {

        int port = 54672;

        String grid = null;
        String site = "carrot";

        boolean verbose = false;
        int size = 1;

        for (int i=0;i<args.length;i++) {

            if (args[i].equals("--grid")) {
                grid = args[++i];
            } else if (args[i].equals("--verbose")) {
                verbose = true;
            } else if (args[i].equals("--port")) {
                port = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--defaultsize")) {
                size = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--defaultsite")) {
                site = args[++i];
            } else {
                fatal("Unknown option: " + args[i]);
            }
        }

        if (grid == null) {
            fatal("No grid configuration specified!");
        }

        try {
            Daemon d = new Daemon(grid, size, site, verbose);
            Proxy p = new Proxy(d, port);

            System.out.println("Daemon waiting for input!");

            p.run(); // Note: we intentionally use run here (not start).
        } catch (Exception e) {
            fatal("Daemon died!", e);
        }
    }
}
