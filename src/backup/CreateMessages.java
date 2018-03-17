package backup;

public abstract class CreateMessages 
{
	/*<MessageType>
	This is the type of the message. Each subprotocol specifies its own message types. This field determines the format of the message and what actions its receivers should perform. This is encoded as a variable length sequence of ASCII characters. 
	<Version>
	This is the version of the protocol. It is a three ASCII char sequence with the format <n>'.'<m>, where <n> and <m> are the ASCII codes of digits. For example, version 1.0, the one specified in this document, should be encoded as the char sequence '1''.''0'.
	<SenderId>
	This is the id of the server that has sent the message. This field is useful in many subprotocols. This is encoded as a variable length sequence of ASCII digits. 
	<FileId>
	This is the file identifier for the backup service. As stated above, it is supposed to be obtained 
	by using the SHA256 cryptographic hash function. As its name indicates its length is 256 bit, 
	i.e. 32 bytes, and should be encoded as a 64 ASCII character sequence. 
	The encoding is as follows: each byte of the hash value is encoded by the two ASCII characters 
	corresponding to the hexadecimal representation of that byte. 
	E.g., a byte with value 0xB2 should be represented by the two char sequence 'B''2' 
	(or 'b''2', it does not matter). The entire hash is represented in big-endian order, i.e. 
	from the MSB (byte 31) to the LSB (byte 0).
	<ChunkNo>
	This field together with the FileId specifies a chunk in the file. The chunk numbers are integers and should be assigned sequentially starting at 0. It is encoded as a sequence of ASCII characters corresponding to the decimal representation of that number, with the most significant digit first. The length of this field is variable, but should not be larger than 6 chars. Therefore, each file can have at most one million chunks. Given that each chunk is 64 KByte, this limits the size of the files to backup to 64 GByte. 
	<ReplicationDeg>
	This field contains the desired replication degree o*/
	
	protected static String CRLF = "\r\n";

	public static byte[] createHeader(String msgType, String version, String senderID, String fileID, int chunkNr, int repDeg)
	{
		String temp = msgType + " " + version + " " + senderID + " " + fileID + " " + chunkNr + " " + repDeg + CRLF + CRLF;
		
		return temp.getBytes(); 
	}

}
