package sc11.daemon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import sc11.processing.Result;
import sc11.shared.FilterSequence;
import sc11.shared.Protocol;

public class Proxy extends Thread {

    private final ServerSocket ss;
    private final Daemon master;

    private boolean done = false;

    private class Connection extends Thread {
        private Socket s;

        private DataInputStream in;
        private DataOutputStream out;

        public Connection(Socket s) throws IOException {
            this.s = s;
            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        }

        private void handleExec() throws IOException {

            FilterSequence job = FilterSequence.read(in);

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

        private void handleInfo() throws IOException {

            long id = in.readLong();

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
            } else if (!res.finished()) {
                out.write(Protocol.OPCODE_RUNNING);
                out.writeUTF(res.getState());
            } else if (res.success()) {
                out.write(Protocol.OPCODE_DONE);
                out.writeUTF(res.getOuput());
            } else {
                out.write(Protocol.OPCODE_ERROR);
                out.writeUTF(res.getError());
            }

            out.flush();
        }

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
                        handleExec();
                        break;

                    case Protocol.OPCODE_INFO:
                        handleInfo();
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

    public Proxy(Daemon master, int port) throws IOException {
        this.master = master;
        ss = new ServerSocket(port);
    }

    public synchronized long exec(FilterSequence job) throws Exception {
        return master.exec(job);
    }

    public synchronized Result info(long id) throws Exception {
        return master.info(id);
    }

    private synchronized boolean getDone() {
        return done;
    }

    public synchronized void done() {
        done = true;
    }

    public void run() {

        try {
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
