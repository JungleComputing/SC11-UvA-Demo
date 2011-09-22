package sc11.daemon;

import java.io.File;
import java.util.HashMap;

import sc11.shared.FilterSequence;
import sc11.shared.Result;

import ibis.deploy.Application;
import ibis.deploy.ApplicationSet;
import ibis.deploy.Cluster;
import ibis.deploy.Deploy;
import ibis.deploy.Experiment;
import ibis.deploy.Grid;
import ibis.deploy.JobDescription;
import ibis.deploy.State;
import ibis.deploy.Workspace;

/**
 * This Daemon class is responsible for deploying and managing the necessary resources to process one or more
 * {@link FilterSequence}s.
 *
 * These {@link FilterSequence}s are typically provided by a {@link DaemonProxy}, with in turn receives it from a remote
 * {@link DaemonStub}. This Daemon then uses Ibis {@link Deploy} to deploy an application to the specified cluster which then
 * performs the necessary file transfers and processing.
 *
 * @author jason@cs.vu.nl
 */
public class Daemon {

    private final int defaultSize;
    private final String defaultSite;

    private final Deploy deploy;
    // private final GUI gui;

    private final ContactServer server;

    private final Grid grid;
    private final ApplicationSet applications;
    private final Experiment experiment;

    private final boolean verbose;

    private long id = 0;

    // This class contains all information of a single running FilterSequence.
    private class ProcessingJob {

        final long id;

        final FilterSequence work;

        final ibis.deploy.Job master;
        final ibis.deploy.Job slaves;

        Result result = null;

        boolean done = false;

        ProcessingJob(long id, FilterSequence work, ibis.deploy.Job master, ibis.deploy.Job slaves) {
            this.id = id;
            this.work = work;
            this.master = master;
            this.slaves = slaves;
        }

        public synchronized void setStatus(Result result) {
            this.result = result;
        }

        public synchronized Result applicationState() {
            return result;
        }

        public synchronized void done() {
            done = true;
        }

        public synchronized boolean isDone() {
            return done;
        }
    }

    private HashMap<Long, ProcessingJob> jobs = new HashMap<Long, ProcessingJob>();

    /**
     * Creates a new Daemon capable of deploying to the sites described in "gridFile".
     *
     * @param gridFile the Ibis Deploy configuration file to use.
     * @param defaultSize the default number of nodes to deploy to.
     * @param defaultSite the default grid site to use.
     * @param verbose be verbose ?
     * @throws Exception if the IbisDeploy configuration was not found.
     */
    public Daemon(String gridFile, int defaultSize, String defaultSite, boolean verbose) throws Exception {

        this.defaultSize = defaultSize;
        this.defaultSite = defaultSite;
        this.verbose = verbose;

        grid = new Grid(new File(gridFile));
        experiment = new Experiment("SC11-UvA-Demo");
        applications = new ApplicationSet();

        Workspace workspace = new Workspace(grid, applications, experiment);

        deploy = new Deploy(new File("deploy-workspace"), verbose, false, 0,
                null, null, true);

        server = new ContactServer(this, deploy.getServerAddress(), verbose);

        /*
        if (useGui) {
            gui = new GUI(deploy, workspace, Mode.MONITOR, logos);
        } else {
            gui = null;
        }
         */
    }

    // Get a unique ID
    private synchronized long getID() {
        return id++;
    }

    // Create an application description.
    private Application createApplication(String name, String libs, String config, String tmpDir, String scriptDir, String master,
            long id, String executors) throws Exception {

        Application m = new Application(name);

        // HACK: deploy demands libs to be set ?
        m.setLibs(new File("lib/dummy"));
        m.setMainClass("sc11.processing.Main");
        m.setMemorySize(1000);
        m.setLog4jFile(new File("log4j.properties"));

        m.setSystemProperty("gat.adaptor.path", libs + "javagat" +
                File.separator + "adaptors");

        m.setSystemProperty("ibis.constellation.master", master);
        m.setSystemProperty("ibis.constellation.steal.size", "10");
        m.setSystemProperty("ibis.constellation.steal.delay", "5");

        m.setSystemProperty("sc11.config", config);
        m.setSystemProperty("sc11.tmpDir", tmpDir);
        m.setSystemProperty("sc11.scriptDir", scriptDir);
        m.setSystemProperty("sc11.ID", "" + id);
        m.setSystemProperty("sc11.executors", executors);
        m.setSystemProperty("sc11.verbose", "true");

        m.setJVMOptions("-classpath", libs + "sc11-application-0.2.0.jar:" + libs + "constellation-0.7.0.jar:" +
                        libs + "javagat" + File.separator + "*:" + libs + "ipl" + File.separator + "*");
        return m;
    }

    /**
     * Execute the given FilterSequence by deploying the necessary application to the preferred site.
     *
     * @param job the FilterSequence to execute.
     * @return a unique ID that can be used to retrieve information on the execution status of the FilterSequence.
     * @throws Exception if the deployment failed.
     */
    public long exec(FilterSequence job) throws Exception {

        // First we get an unique ID.
        long id = getID();

        // Next, we extract some information about the job
        int workers = defaultSize;

        // Ensure that the node count and site have a valid value.
        if (job.nodes > 0) {
            workers = job.nodes;
        }

        String site = defaultSite;

        if (job.site != null) {
            site = job.site;
        }

        if (verbose) {
            System.out.println("Daemon executing Job [" + id + "] on " + site + "/" + workers + " : " + job);
        }

        // Next retrieve the cluster we will run on.
        Cluster cluster = grid.getCluster(site);

        if (cluster == null) {
            throw new Exception("Cluster \"" + site + "\"not found in grid description file.");
        }

        // Get some info from the cluster.
        String location = cluster.getProperties().getProperty("sc11.location");

        if (location == null) {
            throw new Exception("sc11.location property not set for cluster \"" + site + "\" in grid description file.");
        }

        String tmpDir = cluster.getProperties().getProperty("sc11.tmp");

        if (tmpDir == null) {
            tmpDir = location + File.separator + "tmp";
        }

        String execM = cluster.getProperties().getProperty("sc11.executors.master");

        if (execM == null) {
            execM = "master:24";
        }

        String execS = cluster.getProperties().getProperty("sc11.executors.slave");

        if (execS == null) {
            execS = "slave:16";
        }

        String config = location + File.separator + "scripts" +
                File.separator + "configuration";

        String scriptDir = location + File.separator + "scripts";

        String libs = location + File.separator + "lib" + File.separator;

        Application m = createApplication("SC11-Master", libs, config, tmpDir,
                scriptDir, "true", id, execM);

        JobDescription jm = new JobDescription("SC11-Master-" + id);
        experiment.addJob(jm);

        jm.getCluster().setName(site);
        jm.setProcessCount(1);
        jm.setResourceCount(1);
        jm.setRuntime(60);
        jm.getApplication().setName("SC11-Master");
        jm.setPoolName("SC11-" + id);

        ibis.deploy.Job master = deploy.submitJob(jm, m, cluster, null, null);

        // Only submit the slaves if there is some processing to be done.
        if (job.filters != null && job.filters.length > 0) {
            Application s = createApplication("SC11-Slave", libs, config, tmpDir, scriptDir, "false", id, execS);

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

            addJob(new ProcessingJob(id, job, master, slaves));
        } else {
            addJob(new ProcessingJob(id, job, master, null));
        }

        return id;
    }

    // Add a ProcessingJob to the hash.
    private synchronized void addJob(ProcessingJob job) {
        jobs.put(job.id, job);
    }

    // Find a ProcessingJob in the hash.
    private synchronized ProcessingJob getJob(long id) {
        return jobs.get(id);
    }

    // Remove a ProcessingJob from the hash.
    private synchronized ProcessingJob removeJob(long id) {
        return jobs.remove(id);
    }

    // Terminate a running job by killing the master and slaves.
    private void terminateJob(ProcessingJob job) {

        if (verbose) {
            System.out.println("Terminating job " + job.id);
        }

        if (!job.master.isFinished()) {
            try {
                job.master.kill();
            } catch (Exception e) {
                System.err.println("Failed to terminate master of " + job.id);
                e.printStackTrace(System.err);
            }
        }

        if (job.slaves != null && !job.slaves.isFinished()) {
            try {
                job.slaves.kill();
            } catch (Exception e) {
                System.err.println("Failed to terminate slaves of " + job.id);
                e.printStackTrace(System.err);
            }
        }

        removeJob(job.id);
    }

    /**
     * Retrieve the current state or result for the {@link FilterSequence} identified by "id".
     *
     * @param id the unique ID of the {@link FilterSequence}.
     * @return the current state, the final results, or an error if the ID is not known.
     */
    public Result info(long id) {

        ProcessingJob job = getJob(id);

        if (job == null) {
            return new Result("DAEMON", false, "Unknown job id: " + id);
        }

        Result tmp = job.applicationState();

        if (tmp == null) {
            // Job is still staging in/out or has failed!
            State m = job.master.getState();
            State s = job.slaves == null ? null : job.slaves.getState();

            if (m == State.DONE || m == State.ERROR) {
                terminateJob(job);
                return new Result("DAEMON", false, "Job terminated unexpectedly!");
            }

            if (s != null && (s == State.DONE || s == State.ERROR)) {
                terminateJob(job);
                return new Result("DAEMON", false, "Job terminated unexpectedly!");
            }

            return new Result("DAEMON", "DEPLOYING: " + m.name() +
                    (s == null ? "" : (" | " + s.name())));
        }

        if (job.isDone()) {
            removeJob(id);
            return tmp;
        } else {
            return new Result("DAEMON", "RUNNING: " + tmp.message);
        }
    }

    // Print an error message and exit.
    private static void fatal(String message) {
        fatal(message, null);
    }

    // Print an error message (with stacktrace) and exit.
    private static void fatal(String message, Exception e) {
        System.err.println(message);

        if (e != null) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }

        System.exit(1);
    }

    /**
     * This call is part of the {@link DaemonInterface} and is used by the application {@link Master} to retrieve a
     * {@link FilterSequence} that is will subsequently execute.
     *
     * @param id a unique ID for the {@link Master} sending the request. This ID determines which {@link FilterSequence} will be
     *         returned.
     * @return the {@link FilterSequence} that needs to be executed.
     * @throws Exception if the ID is not known.
     */
    public FilterSequence getWork(long id) throws Exception {

        ProcessingJob p = getJob(id);

        if (p == null) {
            throw new Exception("Job " + id + " not found!");
        }

        if (verbose) {
            System.out.println("Get work for [" + id + "]: " + p.work);
        }

        return p.work;
    }

    /**
     * This call is part of the {@link DaemonInterface} and is used by the application {@link Master} to inform the Daemon of its
     * current execution status. It is also use to return the final result.
     *
     * @param id a unique ID for the {@link Master} sending the request.
     * @param res the current execution status or final result of the {@link FilterSequence} execution.
     */
    public void setStatus(long id, Result res) {

        if (verbose) {
            System.out.println("Set status for [" + id + "]: " + res);
        }

        ProcessingJob p = getJob(id);

        if (p == null) {
            return;
        }

        p.setStatus(res);
    }

    /**
     * This call is part of the {@link DaemonInterface} and is used by the application {@link Master} to inform the Daemon that it
     * has finished processing.
     *
     * @param id a unique ID for the {@link Master} sending the request.
     */
    public void done(long id) {

        if (verbose) {
            System.out.println("Work done for [" + id + "]");
        }

        ProcessingJob p = getJob(id);

        if (p == null) {
            return;
        }

        p.done();
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
            DaemonProxy p = new DaemonProxy(d, port);

            if (verbose) {
                System.out.println("Daemon waiting for input!");
            }

            p.run(); // Note: we intentionally use run here (not start).
        } catch (Exception e) {
            fatal("Daemon died!", e);
        }
    }
}
