package sc11.processing;

import java.util.ArrayList;

import ibis.constellation.Constellation;
import ibis.constellation.ConstellationFactory;
import ibis.constellation.Executor;
import ibis.constellation.SimpleExecutor;
import ibis.constellation.StealPool;
import ibis.constellation.StealStrategy;
import ibis.constellation.context.UnitWorkerContext;

public class Slave {

    private static ArrayList<String> executors = new ArrayList<String>();

    private static void parseArguments(String [] args) {

        String tmpdir = null;
        String scriptdir = null;

        for (int i=0;i<args.length;i++) {

            if (args[i].startsWith("--exec")) {

                if (i+2 >= args.length) {
                    System.err.println("Insufficient parameters for --exec");
                    System.exit(1);
                }

                String context = args[++i];
                int count = Integer.parseInt(args[++i]);

                for (int j=0;j<count;j++) {
                    executors.add(context);
                }
            } else if (args[i].startsWith("--scriptdir")) {
                scriptdir = args[++i];
            } else if (args[i].startsWith("--tmpdir")) {
                tmpdir = args[++i];
            } else {
                System.err.println("Unknown option " + args[i]);
                System.exit(1);
            }
        }

        if (executors.size() == 0) {
            System.err.println("No executors defined!");
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

        LocalConfig.set(tmpdir, scriptdir);
    }

    public static void main(String [] args) {

        try {

            parseArguments(args);

            StealPool master = new StealPool("master");
            StealStrategy st = StealStrategy.SMALLEST;

            Executor [] e = new Executor[executors.size()];

            for (int i=0;i<executors.size();i++) {
                System.out.println("Creating executor with context \"" +
                        executors.get(i) + "\"");

                e[i] = new SimpleExecutor(StealPool.NONE, master,
                        new UnitWorkerContext(executors.get(i)), st, st, st);
            }

            Constellation cn = ConstellationFactory.createConstellation(e);
            cn.activate();
            cn.done();

        } catch (Exception e) {
            System.err.println("Slave failed!");
            e.printStackTrace(System.err);
        }
    }
}


