package sc11.daemon;

import sc11.shared.FilterSequence;
import sc11.shared.Result;
import ibis.ipl.util.rpc.RemoteException;

/** 
 * Remote interface used by Constellation Master processes to query and inform 
 * the daemon. 
 */
public interface DaemonInterface {

	// Retrieve work from the daemon
	public FilterSequence getWork(long id) throws RemoteException, Exception;
	
	// Send a status update to the daemon
	public void setStatus(long id, String status) throws RemoteException, Exception;

	// Tell the daemon we are done
	public void done(long id, Result result) throws RemoteException, Exception;
}
