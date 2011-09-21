package sc11.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class FilterSequence implements Serializable {

	/** Generated */
	private static final long serialVersionUID = -7475092282914188748L;
	
	/** Input URI pointing to directory */
	public final String inputDir;
	
	/** Input file suffix */ 
    public final String inputSuffix;
    
    /** Output URI pointing to directory */	
    public final String outputDir;

    /** Sequence of filters to apply to each file in the input */
    public final String [] filters;
 
    /** Site on which the filtering should be run */    
    public final String site;

    /** Number of slaves to use for filtering */    
    public final int nodes;

    /** 
     * Create custom FilterSequence.
     * 
     * @param inputDir input URI (directory).
     * @param inputSuffix input file suffix. 
     * @param outputDir output URI (directory).
     * @param filters sequence of filters to apply.
     * @param site target site on which the filtering should be run.
     * @param nodes number of slaves to use for filtering.
     */
    public FilterSequence(String inputDir, String inputSuffix, String outputDir, String[] filters, String site, int nodes) {
        super();
        this.inputDir = inputDir;
        this.inputSuffix = inputSuffix;
        this.outputDir = outputDir;
        this.filters = filters;
        this.site = site;
        this.nodes = nodes;
    }

    /** 
     * Create a FilterSequence that uses the default number of slaves for the selected site.
     * 
     * @param inputDir input URI (directory).
     * @param inputSuffix input file suffix. 
     * @param outputDir output URI (directory).
     * @param filters sequence of filters to apply.
     * @param site target site on which the filtering should be run.
     */
    public FilterSequence(String inputDir, String inputSuffix, String outputDir, String[] filters, String site) {
        this(inputDir, inputSuffix, outputDir, filters, site, 0);
    }

    /** 
     * Create a FilterSequence that uses the default site and default number of slaves.
     * 
     * @param inputDir input URI (directory).
     * @param inputSuffix input file suffix. 
     * @param outputDir output URI (directory).
     * @param filters sequence of filters to apply.
     */
    public FilterSequence(String inputDir, String inputSuffix, String outputDir, String[] filters) {
        this(inputDir, inputSuffix, outputDir, filters, null, 0);
    }


    /** 
     * Create a FilterSequence that only copies the inputDir to the outputDir.
     * 
     * @param inputDir input URI (directory).
     * @param inputSuffix input file suffix. 
     * @param outputDir output URI (directory).
     */
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
}
