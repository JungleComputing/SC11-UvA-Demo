package sc11.daemon;

import java.io.IOException;
import java.util.Properties;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.RegistryEventHandler;

public class Contact implements RegistryEventHandler, MessageUpcall {

	private final IbisCapabilities capabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT);

	public static final PortType portType = new PortType(
			PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
			PortType.RECEIVE_AUTO_UPCALLS, PortType.CONNECTION_ONE_TO_ONE);
	
	private final Ibis myIbis;
	
	public Contact(String address) throws IbisCreationFailedException { 

		System.out.println("Creating Ibis Contact using server: " + address);
		
		Properties p = new Properties();
		p.put("ibis.server.address", address);
		p.put("ibis.pool.name", "SC11-Server");
				
		myIbis = IbisFactory.createIbis(capabilities, p, true, this, portType);
		
		System.out.println("Ibis Contact created!");
	}

	public void addClient(long id) throws IOException { 
		
		ReceivePort rp = myIbis.createReceivePort(portType, "X-" + id, this);
		
		
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
	
	/** End of registry interface **/

	
	@Override
	public void upcall(ReadMessage arg0) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}
}
