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

    private HashMap<Long, ibis.deploy.Job> jobs =
            new HashMap<Long, ibis.deploy.Job>();

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
        Application a = applications.getApplication("SC11");

        if (a == null) {
            a = new Application("SC11");
            
            a.setLibs(new File("lib/sc11-application-0.2.0.jar"));

            // application.addInputFile(new
            // File("libibis-amuse-bhtree_worker.so"));
            a.setMainClass("sc11.processing.Main");
            a.setMemorySize(1000);
            a.setLog4jFile(new File("log4j.properties"));
       
            a.setSystemProperty("gat.adaptor.path", libs + "JavaGAT-2.1.1" + 
            		File.separator + "adaptors");
        
            a.setSystemProperty("sc11.config", config);
            a.setSystemProperty("sc11.tmpDir", tmpDir);
            a.setSystemProperty("sc11.scriptDir", scriptDir);        
                        
            // FIXME: hardcoded executor config!
            a.setSystemProperty("sc11.executors.master", "master");
            a.setSystemProperty("sc11.executors.slave", "slave:2,gpu");
            
            // FIXME: hardcoded version numbers!
            a.setJVMOptions("-classpath", "\"" + 
            		libs + "sc11-application-0.2.0.jar:" +
            		libs + "constellation-0.7.0.jar:" +            		
            		libs + "JavaGAT-2.1.1" + File.separator + "*:" + 
            		libs + "ipl-2.2" + File.separator + "*\"");
                        
            // application.setSystemProperty("ibis.managementclient", "false");
            // application.setSystemProperty("ibis.bytescount", "");

            applications.addApplication(a);
        }

        JobDescription j = new JobDescription("SC11-" + id);
        experiment.addJob(j);

        j.getCluster().setName(site);
        j.setProcessCount(workers);
        j.setResourceCount(workers);
        j.setRuntime(60);
        j.getApplication().setName("SC11");
        j.setPoolName("SC11-" + id);
        
        // make sure there is an output file on the other side (hack!)
        //        jobDescription.getApplication().setInputFiles(new File("output"));


        //jobDescription.getApplication().setSystemProperty("java.library.path",
         //       absCodeDir);

        ArrayList<String> arg = new ArrayList<String>();

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

        j.getApplication().setArguments(arg.toArray(new String[arg.size()]));
        ibis.deploy.Job result = deploy.submitJob(j, a, cluster, null, null);

        addJob(id, result);

        return id;
    }

    private synchronized void addJob(long id, ibis.deploy.Job job) {
        jobs.put(id, job);
    }

    private synchronized ibis.deploy.Job getJob(long id) {
        return jobs.get(id);
    }

    private synchronized ibis.deploy.Job removeJob(long id) {
        return jobs.remove(id);
    }

    public Result info(long id) {

        ibis.deploy.Job job = jobs.get(id);

        if (job == null) {
            return new Result().failed("Unknown job id: " + id);
        }

        State s = job.getState();

        if (s == State.DEPLOYED) {
            // we should ask the application!
        }

        return new Result().setState(s.name());
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
