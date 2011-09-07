package sc11.daemon;

import java.io.File;

import ibis.deploy.ApplicationSet;
import ibis.deploy.Deploy;
import ibis.deploy.Experiment;
import ibis.deploy.Grid;
import ibis.deploy.Workspace;

public class Daemon {

    private final int defaultSize;

    private final Deploy deploy;

    // private final GUI gui;

    private final Grid grid;
    private final ApplicationSet applications;
    private final Experiment experiment;

    public Daemon(String gridname, int size, boolean verbose) throws Exception {

        defaultSize = size;

        grid = new Grid(new File(gridname));
        experiment = new Experiment("SC11-UvA-Demo");
        applications = new ApplicationSet();

        Workspace workspace = new Workspace(grid, applications, experiment);

        deploy = new Deploy(new File("deploy"), verbose, false, 0, null, null,
                true);

        /*
        if (useGui) {
            gui = new GUI(deploy, workspace, Mode.MONITOR, logos);
        } else {
            gui = null;
        }
        */
    }

    public void run() {


        
        
        



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

        String grid = null;
        boolean verbose = false;
        int size = 8;

        for (int i=0;i<args.length;i++) {

            if (args[i].equals("--grid")) {
                grid = args[++i];
            } else if (args[i].equals("--verbose")) {
                verbose = true;
            } else if (args[i].equals("--defaultsize")) {
                size = Integer.parseInt(args[++i]);
            } else {
                fatal("Unknown option: " + args[i]);
            }
        }

        if (grid == null) {
            fatal("No grid configuration specified!");
        }

        try {
            new Daemon(grid, size, verbose).run();
        } catch (Exception e) {
            fatal("Daemon died!", e);
        }
    }
}
