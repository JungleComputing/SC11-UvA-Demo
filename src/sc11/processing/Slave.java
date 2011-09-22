package sc11.processing;

import ibis.constellation.Constellation;
import ibis.constellation.ConstellationFactory;
import ibis.constellation.Executor;
import ibis.constellation.SimpleExecutor;
import ibis.constellation.StealPool;
import ibis.constellation.StealStrategy;
import ibis.constellation.context.UnitWorkerContext;

/** 
 * The application's Slave. 
 * 
 * @author jason
 */
public class Slave {

	private final Constellation cn;
	
	/**
	 * Create a slave.
	 * 
	 * @param executors the executor configuration to use for Constellation. 
	 * @throws Exception if the creation of Constellation failed.  
	 */	
	public Slave(String [] executors) throws Exception  { 

		StealPool master = new StealPool("master");
		StealStrategy st = StealStrategy.SMALLEST;

		Executor [] e = new Executor[executors.length];

		for (int i=0;i<executors.length;i++) {
			
			LocalConfig.println("SLAVE: Creating executor with context \"" + executors[i] + "\"");

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
        	LocalConfig.println("SLAVE: Get exception!", e);
        }
    }
}


