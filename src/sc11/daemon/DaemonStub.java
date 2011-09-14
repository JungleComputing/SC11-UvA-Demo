package sc11.daemon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import sc11.shared.FilterSequence;
import sc11.shared.Result;

public class DaemonStub {

    private Socket s;

    private DataInputStream in;
    private DataOutputStream out;

    public DaemonStub(String host, int port) throws UnknownHostException, IOException {

        s = new Socket(host, port);

        in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
    }

    public long exec(FilterSequence job) throws IOException {

        System.out.println("Stub write: " + job);
    	
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

    public Result info(long id) throws IOException {

        out.write(Protocol.OPCODE_INFO);
        out.writeLong(id);
        out.flush();

        // TODO: may return -1 on EOS
        int opcode = in.read();

        switch (opcode) {
        case -1:
        	return new Result().failed("Connection to lost!");
        case Protocol.OPCODE_RUNNING:
        	return new Result().setState(in.readUTF());
        case Protocol.OPCODE_DONE:
        	return new Result().success(in.readUTF());
        case Protocol.OPCODE_ERROR:
        	return new Result().failed(in.readUTF());
        default:
        	return new Result().failed("Unexpected reply! " + opcode);
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
