package backup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.filechooser.FileSystemView;

public class Utils 
{
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public static void deleteFile(String fileID,String server_id,ConcurrentHashMap<String,Integer> records_backup,ConcurrentHashMap<String,Integer> records_store) 
	{
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		File dir = new File(home+"/sdis/files/"+server_id+"/"+fileID);
		if (dir.exists())
		{
			System.out.println("Directory exists: "+fileID);
			String[]entries = dir.list();
			for(String s: entries)
			{
			    File currentFile = new File(dir.getPath(),s);
			    currentFile.delete();
			}
			dir.delete();
			for (String key : records_store.keySet()) 
			{
				String[] keyComponents = key.split(":");
				if (keyComponents[0].equals(fileID))
					records_store.remove(key);
			}
		}
		else
		{
			for (String key : records_backup.keySet()) 
			{
				String[] keyComponents = key.split(":");
				if (keyComponents[1].equals(fileID))
				{
					File original = new File(home+keyComponents[0]);
					original.delete();
				}
			}
		}
	}
	
	public static long checkDirectorySize(File directory) {
		long length = 0;
	    for (File file : directory.listFiles()) {
	        if (file.isFile())
	            length += file.length();
	        if(file.isDirectory())
            length += checkDirectorySize(file);
	    }
        return length;
    }
	
	public static String createFileID(String filename) throws IOException, NoSuchAlgorithmException
	{
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		Path file = Paths.get(home.getAbsolutePath()+filename);
		BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
		FileTime creationTime = attr.creationTime();
		long fileSize = attr.size();
		String tempID=filename+creationTime.toString()+fileSize;

		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] fileID = digest.digest(tempID.getBytes(StandardCharsets.UTF_8));
		
		return Utils.bytesToHex(fileID); 
	}
	
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

