package sc11.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class Job {

    final String inputDir;
    final String inputSuffix;
    final String outputDir;

    final String [] filters;

    final String site;

    final int nodes;

    public Job(String inputDir, String inputSuffix, String outputDir,
            String[] filters, String site, int nodes) {
        super();
        this.inputDir = inputDir;
        this.inputSuffix = inputSuffix;
        this.outputDir = outputDir;
        this.filters = filters;
        this.site = site;
        this.nodes = nodes;
    }

    public Job(String inputDir, String inputSuffix, String outputDir,
            String[] filters, String site) {
        this(inputDir, inputSuffix, outputDir, filters, site, 0);
    }

    public Job(String inputDir, String inputSuffix, String outputDir,
            String[] filters) {
        this(inputDir, inputSuffix, outputDir, filters, null, 0);
    }

    public Job(String inputDir, String inputSuffix, String outputDir) {
        this(inputDir, inputSuffix, outputDir, null, null, 0);
    }

    public static Job read(DataInputStream in) throws IOException {

        String inputDir = in.readUTF();
        String inputSuffix = in.readUTF();
        String outputDir = in.readUTF();

        int count = in.readInt();

        String [] filters = new String[count];

        for (int i=0;i<count;i++) {
            filters[i] = in.readUTF();
        }

        String site = in.readUTF();
        int nodes = in.readInt();

        return new Job(inputDir, inputSuffix, outputDir, filters, site, nodes);
    }

    public static void write(Job job, DataOutputStream out) throws IOException {

        out.writeUTF(job.inputDir);
        out.writeUTF(job.inputSuffix);
        out.writeUTF(job.outputDir);

        if (job.filters == null) {
            out.writeInt(0);
        } else {
            out.writeInt(job.filters.length);

            for (int i=0;i<job.filters.length;i++) {
                out.writeUTF(job.filters[i]);
            }
        }

        out.writeUTF(job.site);
        out.writeInt(job.nodes);
    }



}
