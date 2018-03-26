package backup;

import java.util.concurrent.ConcurrentHashMap;

public class Utils 
{
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public static String bytesToHex(byte[] bytes) 
	{

		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) 
		{
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static void printRecords(ConcurrentHashMap<String,Integer> recs) 
	{
		for (String key : recs.keySet()) 
		{
			System.out.println("key: " + key + " value: " + recs.get(key));
		}
	}
}

