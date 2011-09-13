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

	private final Constellation cn;
	
	public Slave(String [] executors) throws Exception  { 

		StealPool master = new StealPool("master");
		StealStrategy st = StealStrategy.SMALLEST;

		Executor [] e = new Executor[executors.length];

		for (int i=0;i<executors.length;i++) {
			System.out.println("Creating executor with context \"" +
					executors[i] + "\"");

			e[i] = new SimpleExecutor(StealPool.NONE, master,
					new UnitWorkerContext(executors[i]), st, st, st);
		}

		cn = ConstellationFactory.createConstellation(e);
	} 
	
	public void run() { 
		
		try { 
			cn.activate();
			cn.done();
        } catch (Exception e) {
            System.err.println("Slave failed!");
            e.printStackTrace(System.err);
        }
    }
}


