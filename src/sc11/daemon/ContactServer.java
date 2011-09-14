package sc11.daemon;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.util.rpc.RPC;
import ibis.ipl.util.rpc.RemoteException;
import ibis.ipl.util.rpc.RemoteObject;

import java.util.Properties;

import sc11.shared.DaemonInterface;
import sc11.shared.FilterSequence;
import sc11.shared.Result;

public class ContactServer implements RegistryEventHandler, DaemonInterface {

	private final IbisCapabilities capabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT, 
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

	private final Daemon parent;
	private final Ibis myIbis;
	
	private final RemoteObject<DaemonInterface> remoteObject;
	
	public ContactServer(Daemon parent, String address) throws Exception { 

		System.out.println("Creating Ibis Contact using server: " + address);
		
		this.parent = parent;
		
		Properties p = new Properties();
		p.put("ibis.server.address", address);
		p.put("ibis.pool.name", "SC11-ContactServer");
				
		myIbis = IbisFactory.createIbis(capabilities, p, true, this, 
				RPC.rpcPortTypes);
	
		myIbis.registry().elect("ContactServer");
		
		// Make this object remotely accessible
		remoteObject = RPC.exportObject(DaemonInterface.class, this, "Contact", myIbis);
		
		System.out.println("Ibis Contact created!");
		
	}

	public void terminate() {
		
		try { 
			// Cleanup, object no longer remotely accessible
			remoteObject.unexport();
		} catch (Exception e) {
			System.err.println("Failed to retract remote object!");
			e.printStackTrace(System.err);
		}
		
		try { 
			myIbis.end();
		} catch (Exception e) {
			System.err.println("Failed to end Ibis!");
			e.printStackTrace(System.err);
		}
	}
	
	/** Registry interface **/

	@Override
	public void died(IbisIdentifier id) {
		System.out.println("Ibis died: " + id);
	}

	@Override
	public void electionResult(String txt, IbisIdentifier id) {
		// ignored
	}

	@Override
	public void gotSignal(String txt, IbisIdentifier id) {
		// ignored
	}

	@Override
	public void joined(IbisIdentifier id) {
		System.out.println("Ibis joined: " + id);
	}

	@Override
	public void left(IbisIdentifier id) {
		System.out.println("Left joined: " + id);
	}

	@Override
	public void poolClosed() {
		// ignored
	}

	@Override
	public void poolTerminated(IbisIdentifier id) {
		// ignored
	}
	
	/** End of Registry interface **/
	
	/** DaemonInterface **/

	@Override
	public FilterSequence getWork(long id) throws RemoteException, Exception {
		return parent.getWork(id);
	}

	@Override
	public void setStatus(long id, String status) throws RemoteException, Exception {
		parent.setStatus(id, status);		
	}

	@Override
	public void done(long id, Result result) throws RemoteException, Exception {
		parent.done(id, result);
	}

	/** End of DaemonInterface **/
}
