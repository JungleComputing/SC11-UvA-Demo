package sc11.daemon;

import sc11.client.Client;
import sc11.processing.Master;
import sc11.shared.FilterSequence;
import sc11.shared.Result;
import ibis.ipl.util.rpc.RemoteException;

/** 
 * Remote interface used for communication between the {@link Master} and the application's {@link Daemon}.
 * 
 * Once the application is started the {@link Master} uses this remote interface to retrieve work from the {@link Daemon}. It is  
 * therefore the 'back-end' communication.
 *
 * The 'front-end' communication between the {@link Client} and the {@link Daemon} is implemented by the {@link DaemonStub} and 
 * {@link DaemonProxy} classes.  
 */
public interface DaemonInterface {

	/** Retrieve work from the {@link Daemon} */
	public FilterSequence getWork(long id) throws RemoteException, Exception;
	
	/** Send a status update to the {@link Daemon} */
	public void setStatus(long id, Result res) throws RemoteException, Exception;

	/** Tell the {@link Daemon} we are done */
	public void done(long id) throws RemoteException, Exception;
}
