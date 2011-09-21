package sc11.daemon;

public interface Protocol {

	public static byte OPCODE_EXEC    = 31;
	public static byte OPCODE_INFO    = 33;
	public static byte OPCODE_GOODBYE = 35;
	
	public static byte OPCODE_ACCEPT  = 53;
	public static byte OPCODE_ERROR   = 55;
	public static byte OPCODE_RESULT  = 57;	
}
