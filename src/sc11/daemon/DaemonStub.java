package sc11.daemon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import sc11.shared.FilterSequence;
import sc11.shared.Result;

public class DaemonStub {

    private Socket s;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public DaemonStub(String host, int port) throws UnknownHostException, IOException {

        s = new Socket(host, port);

        in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
        out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
        out.flush();
    }

    public long exec(FilterSequence job) throws IOException {

        System.out.println("Stub write: " + job);
    	
        out.write(Protocol.OPCODE_EXEC);
        out.writeObject(job);
        out.flush();

        int opcode = in.read();

        switch (opcode) {
        case -1:
            throw new IOException("Connection to daemon lost!");           
        case Protocol.OPCODE_ACCEPT:
            return in.readLong();
        case Protocol.OPCODE_ERROR:
            throw new IOException(in.readUTF());
        default:
            throw new IOException("Unexpected reply: " + opcode);
        }
    }

    public Result info(long id) throws Exception {

        out.write(Protocol.OPCODE_INFO);
        out.writeLong(id);
        out.flush();

        int opcode = in.read();

        switch (opcode) {
        case -1:
            throw new IOException("Connection to daemon lost!");           
        case Protocol.OPCODE_ERROR:
            throw new IOException(in.readUTF());
        case Protocol.OPCODE_RESULT:
        	return (Result) in.readObject();
        default:
            throw new IOException("Unexpected reply: " + opcode);
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
