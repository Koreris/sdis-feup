package backup;

import java.io.IOException;


public abstract class CreateMessages 
{
	protected static String CRLF = "\r\n";
	
	public static byte[] createHeader(String msgType, String version, String senderID, String fileID, int chunkNr, int repDeg) throws IOException 
	{	
		String temp;
		if (msgType.equals("STORED") || msgType.equals("GETCHUNK") || msgType.equals("CHUNK") || msgType.equals("REMOVED"))
		{
			temp = msgType + " " + version + " " + senderID + " " + fileID + " " + chunkNr + " " + CRLF + CRLF;
			return temp.getBytes();
		}

		else if (msgType.equals("DELETE"))
		{
			temp = msgType + " " + version + " " + senderID + " " + fileID + " " + CRLF + CRLF;
			return temp.getBytes();
		}

		temp = msgType + " " + version + " " + senderID + " " + fileID + " " + chunkNr + " " + repDeg + CRLF + CRLF;
		return temp.getBytes();
	}

}
