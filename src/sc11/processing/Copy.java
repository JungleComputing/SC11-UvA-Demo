package sc11.processing;

import org.gridlab.gat.io.File;

import sc11.shared.Result;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;
import ibis.constellation.context.UnitActivityContext;

/**
 * This activity is responsible for copying one file to another using the JavaGAT.
 *
 * @author jason@cs.vu.nl
 */
public class Copy extends SimpleActivity {

    /** Generated */
    private static final long serialVersionUID = -6441545366616186882L;

    private final File in;
    private final File out;

    /**
     * Creates a new Activity to copy a file.
     *
     * @param parent the activity to send the result to.
     * @param rank our relative rank (used to order the copy operations).
     * @param in the input file.
     * @param out the output file.
     */
    public Copy(ActivityIdentifier parent, long rank, File in, File out) {
        super(parent, new UnitActivityContext("master", rank), true);

        this.in = in;
        this.out = out;
    }

    // Performs the actual copy.
    private Result copy() {

        String txt = "COPY(" + in + " -> " + out + ")";

        try {
            long start = System.currentTimeMillis();

            in.copy(out.toGATURI());

            long end = System.currentTimeMillis();

            LocalConfig.println(txt + ": OK " + (end-start));

            return new Result(txt, true, "OK" , (end-start));
        } catch (Exception e) {
            LocalConfig.println(txt + ": ERROR " + e.getMessage(), e);
            return new Result(txt, false, "ERROR " + e.getMessage(), 0);
        }
    }

    /**
     * When activated, this activity simply calls copy and returns the result.
     */
    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, copy()));
    }
}
