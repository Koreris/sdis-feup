package backup;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.FileSystemView;

public class RecoveryChannelPacketHandler implements Runnable 
{
	DatagramPacket data;
	String server_id;
	ConcurrentHashMap<String, Integer> records_restore;
	ScheduledThreadPoolExecutor scheduling_pool;

	public RecoveryChannelPacketHandler(DatagramPacket packet, String servid, ConcurrentHashMap<String, Integer> records_restore, ScheduledThreadPoolExecutor scheduling_pool) 
	{
		data=packet;
		server_id=servid;
		this.records_restore=records_restore;
		this.scheduling_pool=scheduling_pool;
	}

	@Override
	public void run() 
	{

		String packetString = new String(data.getData(),0,data.getLength());

		String[] lines = packetString.split(System.getProperty("line.separator"));
		String header = lines[0];

		String[] headerComponents = header.split(" ");
		byte[] actual_data = new byte[data.getLength()];
		System.arraycopy(data.getData(), data.getOffset(), actual_data, 0, data.getLength());
		int filler_length = lines[0].length()+lines[1].length()+4;
		
		switch(headerComponents[0]) {
		case "CHUNK":
			handleChunk(headerComponents,Arrays.copyOfRange(actual_data, filler_length, actual_data.length));
			break;
		default:
			break;
		}
	}

	private void handleChunk(String[] headerComponents, byte[] filedata) {
		if(headerComponents[2].equals(server_id)) {
			if(records_restore.containsKey(headerComponents[3]+headerComponents[4])) {
				scheduling_pool.schedule(()->records_restore.remove(headerComponents[3]+headerComponents[4]), 1, TimeUnit.SECONDS);
			}
			return;
		}
			
	
		if(records_restore.containsKey(headerComponents[3])) {
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File chunk = new File(home.getAbsolutePath()+"/sdis/files/"+server_id+"/restored/"+headerComponents[3]+File.separator+headerComponents[4]);
			
			if(chunk.exists()) {
				System.out.println("I already recovered this chunk!");
				return;
			}
			
			FileOutputStream out;
			try {
				File newfile = new File("/"+home.getAbsolutePath()+"/sdis/files/"+server_id+"/restored/"+headerComponents[3]+File.separator+headerComponents[4]);
				newfile.getParentFile().mkdirs(); // correct!
				if (!newfile.exists()) {
				    newfile.createNewFile();
				} 
				out = new FileOutputStream(newfile);
				out.write(filedata);
				out.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else { 
			records_restore.put(headerComponents[3]+headerComponents[4],0);
			scheduling_pool.schedule(()->records_restore.remove(headerComponents[3]+headerComponents[4]), 1, TimeUnit.SECONDS);
		}
    }

}