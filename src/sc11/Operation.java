package sc11;

import java.io.File;

import ibis.constellation.Activity;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class Operation extends Activity {

	private static final int STATE_INIT     = 0;
	private static final int STATE_COPY_IN  = 1;
	private static final int STATE_FILTER   = 2;
	private static final int STATE_COPY_OUT = 3;
	private static final int STATE_DONE     = 4;
	private static final int STATE_ERROR    = 99;
	
	/** Generated */
	private static final long serialVersionUID = -5895535272566557335L;

	private final Master parent;
	private final long id;
	
	private final String in;
	private final String out;
	
	private final String firstTmp;
	private final String lastTmp;
	
	private final Script [] ops;
	private final Result [] results;
	
	private int state = STATE_INIT;
	
	private String errorMessage;
	
	public Operation(Master parent, long id, String in, ScriptDescription [] sd,
			String out) {
	
		super(new UnitActivityContext("master", id), true, true);
		
		this.parent = parent;
		this.id = id;
		
		this.in = in;
		this.out = out;

		this.results = new Result[3];

		if (sd == null || sd.length == 0) { 
			this.ops = null;
			this.firstTmp = this.lastTmp = generateTempFile(in, 0);
		} else { 
	
			String [] tmp = new String[sd.length+1];
			
			for (int i=0;i<sd.length+1;i++) { 
				tmp[i] = generateTempFile(in, i);
			}
			
			firstTmp = tmp[0];
			lastTmp = tmp[tmp.length-1];
			
			ops = new Script[sd.length];
			
			for (int i=0;i<sd.length;i++) { 
				ops[i] = sd[i].createScript(tmp[i], tmp[i+1]);
			}
		}
	}

	private String generateTempFile(String filename, int count) { 
		return "TMP-" + id + "-" + count + "-" + filename;
	}

	public boolean success() { 
		return (state == STATE_DONE);
	}
	
	public String getErrorMessage() { 
		return errorMessage;
	}
		
	@Override
	public void cancel() throws Exception {
		// Ignored
	}

	@Override
	public void cleanup() throws Exception {
		parent.done(this);
	}

	private synchronized void setState(int state) { 
		this.state = state;
	}

	private synchronized void error(String message) { 
		this.state = STATE_ERROR;
		this.errorMessage = message;
	}

	public synchronized boolean finished() { 
		return state == STATE_DONE || state == STATE_ERROR;
	}
	
	public synchronized Result getResult() {

		if (finished()) { 
			return Result.merge(results);
		} else { 
			Result res = new Result();
			
			switch (state) { 
			case STATE_INIT:
				res.setState("INIT");
				break;	
			case STATE_COPY_IN:
				res.setState("COPY IN");
				break;
			case STATE_FILTER:
				res.setState("FILTER");
				break;
			case STATE_COPY_OUT:
				res.setState("COPY OUT");
				break;
			default:
				res.setState("UNKNOWN");
			}

			return res;
		}
	}

	private synchronized void setResult(int index, Result res) {
		results[index] = res;
	}
	
	@Override
	public void initialize() throws Exception {
		
		setState(STATE_COPY_IN);
		
		LocalConfig config = LocalConfig.get();
	
		executor.submit(new Copy(identifier(), in, 
				config.tmpdir + File.separator + firstTmp));

		suspend();
	}
	
	@Override
	public void process(Event e) throws Exception {
		
		Result res = (Result) e.data;
		
		switch (state) { 
		case STATE_COPY_IN: 
	
			setResult(0, res);
			
			if (res.success()) { 
				setState(STATE_FILTER);
				executor.submit(new Sequence(identifier(), ops));
				suspend();
			} else { 
				error("Failed to copy input file!");
				finish();
			}
			break;

		case STATE_FILTER:
			
			setResult(1, res);
			
			if (res.success()) { 
				LocalConfig config = LocalConfig.get();
				setState(STATE_COPY_OUT);
				executor.submit(new Copy(identifier(), 
						config.tmpdir + File.separator + lastTmp, out));
				suspend();
			} else { 
				error("Failed to execute filter!");
				finish();
			}
			break;

		case STATE_COPY_OUT:
		
			setResult(2, res);
			
			if (e.data == null) { 
				setState(STATE_DONE);
				finish();
			} else { 
				error("Failed to copy output file!");
				finish();
			}
			break;
	
		default:
			error("Operation in illegal state! " + state);
			finish();
		}
	}

	public Long getID() {
		return id;
	}
}
