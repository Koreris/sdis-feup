package backup;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class CreateMessages 
{
	/*
	<FileId>
	This is the file identifier for the backup service. As stated above, it is supposed to be obtained 
	by using the SHA256 cryptographic hash function. As its name indicates its length is 256 bit, 
	i.e. 32 bytes, and should be encoded as a 64 ASCII character sequence. 
	The encoding is as follows: each byte of the hash value is encoded by the two ASCII characters 
	corresponding to the hexadecimal representation of that byte. 
	E.g., a byte with value 0xB2 should be represented by the two char sequence 'B''2' 
	(or 'b''2', it does not matter). The entire hash is represented in big-endian order, i.e. 
	from the MSB (byte 31) to the LSB (byte 0).
	*/
	
	protected static String CRLF = "\r\n";

	public static byte[] createHeader(String msgType, String version, String senderID, String filePath, int chunkNr, int repDeg) throws IOException 
	{	
		MessageDigest digest;
		String temp;
		try 
		{
			if (msgType.equals("STORED"))
			{
				temp = msgType + " " + version + " " + senderID + " " + filePath + " " + chunkNr + " " + CRLF + CRLF;
				return temp.getBytes();
			}
			
			Path file = Paths.get(filePath);
			BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
			FileTime creationTime = attr.creationTime();
			long fileSize = attr.size();
			String tempID=filePath+creationTime.toString()+fileSize;
			digest = MessageDigest.getInstance("SHA-256");
			byte[] fileID = digest.digest(tempID.getBytes(StandardCharsets.UTF_8));

			temp = msgType + " " + version + " " + senderID + " " + fileID + " " + chunkNr + " " + repDeg + CRLF + CRLF;
			return temp.getBytes(); 
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
