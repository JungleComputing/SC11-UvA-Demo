package sc11.shared;

public interface Protocol {

	public static byte OPCODE_EXEC    = 33;
	public static byte OPCODE_INFO    = 34;
	public static byte OPCODE_GOODBYE = 35;
	
	public static byte OPCODE_ACCEPT  = 53;
	public static byte OPCODE_DONE    = 54;
	public static byte OPCODE_RUNNING = 55;
	public static byte OPCODE_ERROR   = 56;
	
}
