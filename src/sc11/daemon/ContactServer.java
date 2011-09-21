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

import sc11.shared.FilterSequence;
import sc11.shared.Result;

/**
 * This ContactServer servers as the a proxy for the {@link Daemon} object implementing the {@link DaemonInterface}.
 *
 * It creates an {@link Ibis} pool and exports itself as {@link DaemonInterface}. Any calls to this {@link DaemonInterface} are
 * forwarded to the associated {@link Daemon}.
 *
 * In addition, this class implements a {@link RegistryEventHandler}. This allows it to print information about the participating
 * clients.
 *
 * @author jason@cs.vu.nl
 */
public class ContactServer implements RegistryEventHandler, DaemonInterface {

    private final IbisCapabilities capabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT,
            IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

    private final Daemon parent;
    private final Ibis myIbis;
    private final boolean verbose;

    private final RemoteObject<DaemonInterface> remoteObject;

    /**
     * Create a ContactServer that represents the given {@link Daemon}.
     *
     * @param parent the {@link Daemon} to represent.
     * @param address the address of the {@link Ibis} registry to connect to.
     * @throws Exception the ContactServer failed contact the registry or failed to export the {@link DaemonInterface}.
     */
    public ContactServer(Daemon parent, String address, boolean verbose) throws Exception {

        if (verbose) {
            System.out.println("Creating Ibis Contact using server: " + address);
        }

        this.parent = parent;
        this.verbose = verbose;

        Properties p = new Properties();
        p.put("ibis.server.address", address);
        p.put("ibis.pool.name", "SC11-ContactServer");

        myIbis = IbisFactory.createIbis(capabilities, p, true, this,
                RPC.rpcPortTypes);

        myIbis.registry().elect("ContactServer");

        // Make this object remotely accessible
        remoteObject = RPC.exportObject(DaemonInterface.class, this, "ContactServer", myIbis);

        if (verbose) {
            System.out.println("Ibis Contact created!");
        }
    }

    /**
     * Terminate the ContactServer.
     */
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
        if (verbose) {
            System.out.println("Ibis died: " + id);
        }
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
        if (verbose) {
            System.out.println("Ibis joined: " + id);
        }
    }

    @Override
    public void left(IbisIdentifier id) {
        if (verbose) {
            System.out.println("Ibis left: " + id);
        }
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
    public void setStatus(long id, Result res) throws RemoteException, Exception {
        parent.setStatus(id, res);
    }

    @Override
    public void done(long id) throws RemoteException, Exception {
        parent.done(id);
    }
    /** End of DaemonInterface **/
}
