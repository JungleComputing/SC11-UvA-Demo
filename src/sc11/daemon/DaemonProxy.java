package sc11.daemon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import sc11.shared.FilterSequence;
import sc11.shared.Result;

/**
 * A DaemonProxy that can accept incoming request from remote {@link DaemonStub}s and forward them to a {@link Daemon}. Any 
 * results produced are returned to the {@link DaemonStub}.
 * 
 * @author jason@cs.vu.nl
 */
public class DaemonProxy extends Thread {

    private final ServerSocket ss;
    private final Daemon master;

    private boolean done = false;

    /** 
     * This class represents a single connection with a {@link DaemonStub}. By extending {@link Thread} it can actively read and
     * write requests from and to the underlying network connection.      
     * 
     * @author jason@cs.vu.nl
     */
    class Connection extends Thread {
    
    	private Socket s;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public Connection(Socket s) throws IOException {
            this.s = s;
            out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out.flush();
            
            in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
        }

        // Forward an exec request to the daemon. 
        private void handleExec(FilterSequence job) throws IOException {

            //System.out.println("PROXY read: " + job);
            
            long id = -1;
            Exception ex = null;

            try {
                id = exec(job);
            } catch (Exception e) {
                ex = e;
            }

            if (ex != null) {
                out.write(Protocol.OPCODE_ERROR);
                out.writeUTF(ex.getMessage());
            } else {
                out.write(Protocol.OPCODE_ACCEPT);
                out.writeLong(id);
            }

            out.flush();
        }

        // Forward an info request to the daemon.
        private void handleInfo(long id) throws IOException {

            Result res = null;
            Exception ex = null;

            try {
                res = info(id);
            } catch (Exception e) {
                ex = e;
            }

            if (ex != null) {
                out.write(Protocol.OPCODE_ERROR);
                out.writeUTF(ex.getMessage());
            } else { 
            	out.write(Protocol.OPCODE_RESULT);
                out.writeObject(res);
            } 

            out.flush();
        }

        // Close the connection. 
        private void close() {
            try {
                out.close();
            } catch (Exception e) {
                // ignored
            }

            try {
                in.close();
            } catch (Exception e) {
                // ignored
            }

            try {
                s.close();
            } catch (Exception e) {
                // ignored
            }
        }

        /** 
         * Runs the connection handler thread for a single connection to a {@link DaemonStub}.
         */
        public void run() {

            boolean done = false;

            while (!done) {

                try {

                    int opcode = in.read();

                    switch (opcode) {
                    case -1:
                        System.err.println("Connection lost!");
                        done = true;
                        break;

                    case Protocol.OPCODE_EXEC:
                        handleExec((FilterSequence) in.readObject());
                        break;

                    case Protocol.OPCODE_INFO:
                        handleInfo(in.readLong());
                        break;

                    case Protocol.OPCODE_GOODBYE:
                        done = true;
                        break;

                    default:
                        System.err.println("Unknown opcode: " + opcode);
                        done = true;
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Connection failed: " + e.getMessage());
                    e.printStackTrace(System.err);
                    done = true;
                }
            }

            close();
        }
    }

    /** 
     * Create a DaemonProxy that can accept incoming remote request and forward them to the given {@link Daemon}.
     * 
     * @param master the {@link Daemon} to forward the request to. 
     * @param port the network port on which incoming connections are accepted. 
     * @throws IOException creation of the network socket failed. 
     */
    public DaemonProxy(Daemon master, int port) throws IOException {
        this.master = master;
        ss = new ServerSocket(port);
    }

    // Forward an exec request to the daemon.
    private synchronized long exec(FilterSequence job) throws Exception {
        return master.exec(job);
    }

    // Forward an info request to the daemon.
    private synchronized Result info(long id) throws Exception {
        return master.info(id);
    }

    // Retrieves the done flag. 
    private synchronized boolean getDone() {
        return done;
    }
   
    /**
     * Tell the DaemonProxy to terminate. 
     */
    public synchronized void done() {
        done = true;
    }

    /** 
     * Runs the main loop of the Thread accepting the incoming connections from the {@link DaemonStub}. 
     */
    public void run() {

        try {
        	// Periodically wake up (every 1000ms). This simplifies termination.   
        	ss.setSoTimeout(1000);

            while (!getDone()) {

                Socket s = null;

                try {
                    s = ss.accept();
                } catch (SocketTimeoutException e) {
                    // ignored
                }

                if (s != null) {
                    new Connection(s).start();
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to accept incoming connection!");
            e.printStackTrace(System.err);
        }

        try {
            ss.close();
        } catch (Exception e) {
            // ignored
        }
    }
}
