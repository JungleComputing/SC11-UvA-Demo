package sc11.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class FilterSequence implements Serializable {

	/** Generated */
	private static final long serialVersionUID = -7475092282914188748L;
	
	public final String inputDir;
    public final String inputSuffix;
    public final String outputDir;

    public final String [] filters;
 
    public final String site;

    public final int nodes;

    public FilterSequence(String inputDir, String inputSuffix, String outputDir,
            String[] filters, String site, int nodes) {
        super();
        this.inputDir = inputDir;
        this.inputSuffix = inputSuffix;
        this.outputDir = outputDir;
        this.filters = filters;
        this.site = site;
        this.nodes = nodes;
    }

    public FilterSequence(String inputDir, String inputSuffix, String outputDir,
            String[] filters, String site) {
        this(inputDir, inputSuffix, outputDir, filters, site, 0);
    }

    public FilterSequence(String inputDir, String inputSuffix, String outputDir,
            String[] filters) {
        this(inputDir, inputSuffix, outputDir, filters, null, 0);
    }

    public FilterSequence(String inputDir, String inputSuffix, String outputDir) {
        this(inputDir, inputSuffix, outputDir, null, null, 0);
    }

    @Override
	public String toString() {
		return "FilterSequence [inputDir=" + inputDir + ", inputSuffix="
				+ inputSuffix + ", outputDir=" + outputDir + ", filters="
				+ Arrays.toString(filters) + ", site=" + site + ", nodes="
				+ nodes + "]";
	}
    
    public static FilterSequence fromArguments(String [] args) throws Exception { 
    	
        String inputURI = null;
        String outputURI = null;
        String inputFileType = null;
        String site = null;
        
        int nodes = -1;
        
        ArrayList<String> filters = new ArrayList<String>();
        
        for (int i=0;i<args.length;i++) {

            if (args[i].equals("--inputURI")) {
                inputURI = args[++i];
            } else if (args[i].equals("--outputURI")) {
                outputURI = args[++i];
            } else if (args[i].equals("--inputSuffix")) {
                inputFileType = args[++i];
            } else if (args[i].equals("--filter")) {
                filters.add(args[++i]);
            } else if (args[i].equals("--site")) {
                site = args[++i];
            } else if (args[i].equals("--nodes")) {
                nodes = Integer.parseInt(args[++i]); 
            }             
            // NOTE: skip everything we don't know
        }

        if (inputURI == null) {
        	throw new Exception("InputURI not set!");
        }

        if (outputURI == null) {
            throw new Exception("OutputURI not set!");
        }

        if (inputFileType == null) {
            throw new Exception("InputSuffix not set!");
        }

        String [] tmp = filters.toArray(new String[filters.size()]);
        return new FilterSequence(inputURI, inputFileType, outputURI, tmp, 
        		site, nodes);
    }
}
