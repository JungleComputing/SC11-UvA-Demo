package sc11.processing;

import java.util.Arrays;
import java.util.Properties;

public class Main {
    public static void main(String [] args) {

        System.out.println("TEST: " + Arrays.toString(args));

        Properties p = System.getProperties();

        System.out.println("adaptors : " + p.getProperty("gat.adaptor.path"));
        System.out.println("config   : " + p.getProperty("sc11.config"));
        System.out.println("scriptDir: " + p.getProperty("sc11.scriptDir"));
        System.out.println("tmpDir   : " + p.getProperty("sc11.tmpDir"));

    }
}
