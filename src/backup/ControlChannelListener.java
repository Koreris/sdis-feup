package backup;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.FileSystemView;

class ControlChannelListener implements Runnable
{
	private MulticastSocket socket;
	MulticastServer main_server;
	private ThreadPoolExecutor control_pool;

	final static int MAX_PACKET_SIZE=64096;
	
	public ControlChannelListener(MulticastServer multicastServer) throws IOException {
		main_server=multicastServer;
		socket = new MulticastSocket(main_server.control_port);
		InetAddress control_adr = InetAddress.getByName(main_server.control_address);
		socket.joinGroup(control_adr);
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		control_pool = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, queue);
	}

	public void run()
	{
		while(true)
		{
			byte[] buf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				control_pool.execute(new ControlChannelPacketHandler(packet,main_server,socket));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initiateDelete(String filename) throws IOException, NoSuchAlgorithmException, InterruptedException
	{
		control_pool.execute(new SendDelete(filename));
		
	}
	
	public class SendDelete implements Runnable
	{
		String filename;
		public SendDelete(String filen) {
			filename=filen;
		}
		@Override
		public void run() {
			try {
				int nr_tries = 0;
				String fileID = Utils.createFileID(filename);
				byte[] delete = CreateMessages.createHeader("DELETE", main_server.protocol_version, main_server.id, fileID, 0,0);
				if(main_server.protocol_version.equals("2.0")){
					File home = FileSystemView.getFileSystemView().getHomeDirectory();
					File dir = new File(home.getAbsolutePath()+"/sdis/files");
					String peerdirs[] = dir.list();
					for(String s: peerdirs)
					{
				
					    File currentFile = new File(dir.getPath(),s);
					    if(currentFile.isDirectory()){
					    	File file_delete = new File (currentFile.getPath()+File.separator+"onhold_delete.txt");
					    	PrintWriter out = new PrintWriter(file_delete);
					    	out.println("DELETE "+fileID);
					    	out.close();
					    }
					}
				}
				InetAddress control_addr = InetAddress.getByName(main_server.control_address);
				DatagramPacket packet = new DatagramPacket(delete,0,delete.length,control_addr,8888);
				while(nr_tries<3) {
					socket.send(packet);
					Thread.sleep(300);
					nr_tries++;
				}
				System.out.println(fileID);
				Utils.deleteFile(fileID,main_server.id,main_server.records_backup,main_server.records_store);
				System.out.println("Delete terminated!");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
}

