package sc11.daemon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Stub {

    private Socket s;

    private DataInputStream in;
    private DataOutputStream out;

    public Stub(String host, int port) throws UnknownHostException, IOException {

        s = new Socket(host, port);

        in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
    }

    public long exec(FilterSequence job) throws IOException {

        out.write(Protocol.OPCODE_EXEC);
        FilterSequence.write(job, out);
        out.flush();

        // TODO: may return -1 on EOS
        int opcode = in.read();

        switch (opcode) {
        case Protocol.OPCODE_ACCEPT:
            return in.readLong();
        case Protocol.OPCODE_ERROR:
            throw new IOException(in.readUTF());
        default:
            throw new IOException("Unexpected reply! " + opcode);
        }
    }

    public String info(long id) throws IOException {

        out.write(Protocol.OPCODE_INFO);
        out.writeLong(id);
        out.flush();

        // TODO: may return -1 on EOS
        int opcode = in.read();

        switch (opcode) {
        case -1:
            throw new IOException("Connection lost!");
        case Protocol.OPCODE_RUNNING:
            System.out.println("Current state: " + in.readUTF());
            return null;
        case Protocol.OPCODE_DONE:
            return in.readUTF();
        case Protocol.OPCODE_ERROR:
            throw new IOException(in.readUTF());
        default:
            throw new IOException("Unexpected reply! " + opcode);
        }
    }

    public void close() {

        try {
            out.write(Protocol.OPCODE_GOODBYE);
            out.flush();
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
}
