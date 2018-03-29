package backup;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.FileSystemView;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;

public class MulticastServer
{
	private String id;
	private String protocol;
	private String access_point;
	private Integer storage_capacity;
	private ScheduledThreadPoolExecutor record_keeper;
	protected ServerRemoteObject rmi_object;
	protected Registry rmi_registry;
	protected ControlChannelListener control_thread;
	protected DataChannelListener data_thread;
	protected RecoveryChannelListener recovery_thread;
	ConcurrentHashMap<String,Integer> records_backup;
	ConcurrentHashMap<String,Integer> records_store;
	ConcurrentHashMap<String,Integer> records_restore;


	public MulticastServer(String ident, String proto, String ap, String control_adr, Integer control_port, String data_adr, int data_port, String recovery_adr, int recovery_port, int storage) throws IOException, AlreadyBoundException 
	{
		id=ident;
		protocol=proto;
		access_point=ap;
		storage_capacity=storage*1000;
		records_backup = new ConcurrentHashMap<String,Integer>(); //load from file data
		records_store = new ConcurrentHashMap<String,Integer>();
		records_restore = new ConcurrentHashMap<String,Integer>();
		record_keeper= new ScheduledThreadPoolExecutor(2);
		control_thread=new ControlChannelListener(id,records_backup,records_store,records_restore,control_adr,control_port,recovery_adr,recovery_port);
		data_thread=new DataChannelListener(id,records_backup,records_store,data_adr,data_port,storage_capacity,control_adr,control_port);
		recovery_thread=new RecoveryChannelListener(id,records_backup,records_store,records_restore,recovery_adr,recovery_port,control_adr,control_port);
		initializeRMI();
	}

	private void startUp() 
	{
		//ExecuteOnHoldCommands
		loadFiles();
		new Thread(control_thread).start();
		new Thread(data_thread).start();
		new Thread(recovery_thread).start();
		record_keeper.scheduleAtFixedRate(new FileMaintenanceService(), 1,5, TimeUnit.SECONDS);
	}

	@SuppressWarnings("unchecked")
	public void loadFiles() {
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		File file_backup = new File (home.getAbsolutePath()+"/sdis/files/"+id+File.separator+"backup_records");
		File file_stored = new File (home.getAbsolutePath()+"/sdis/files/"+id+File.separator+"stored_records");
		FileInputStream fis,fis2;
		if(!file_backup.exists() || !file_stored.exists())
			return;
		try {
			fis = new FileInputStream(file_backup);
			fis2 = new FileInputStream(
					file_stored);
			XMLDecoder de = new XMLDecoder(fis);
			records_backup=(ConcurrentHashMap<String, Integer>) de.readObject();
			de.close();
			fis.close();
			XMLDecoder de2 = new XMLDecoder(fis2);
			records_store=(ConcurrentHashMap<String, Integer>) de2.readObject();
			de2.close();
			fis2.close();
			Utils.printRecords(records_store);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private class FileMaintenanceService implements Runnable{

		@Override
		public void run() {
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File file_backup = new File (home.getAbsolutePath()+"/sdis/files/"+id+File.separator+"backup_records");
			File file_stored = new File (home.getAbsolutePath()+"/sdis/files/"+id+File.separator+"stored_records");
			try {
				file_backup.delete();
				file_stored.delete();
				file_backup.getParentFile().mkdirs(); // correct!
				file_backup.createNewFile();
				file_stored.createNewFile();
				FileOutputStream fos = new FileOutputStream(file_backup);
				XMLEncoder e = new XMLEncoder(fos);
				e.writeObject(records_backup);
				e.close();
				fos.close();
				FileOutputStream fos2 = new FileOutputStream(file_stored);
				XMLEncoder e2 = new XMLEncoder(fos2);
				e2.writeObject(records_store);
				e2.close();
				fos2.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void initializeRMI() throws RemoteException, AlreadyBoundException 
	{
		rmi_object = new ServerRemoteObject(this);
		RMIBackup stub = (RMIBackup) UnicastRemoteObject.exportObject(rmi_object, 0);
		// Bind the remote object's stub in the registry
		try 
		{
			rmi_registry = LocateRegistry.createRegistry(1098);
		}
		catch(Exception e) 
		{
			rmi_registry = LocateRegistry.getRegistry(1098);
		}
		rmi_registry.bind(access_point, stub);
	}
	
	public void initiateReclaim(Integer space) 
	{
		record_keeper.schedule(new ReclaimService(space), 0, TimeUnit.SECONDS);
	}
	
	public class ReclaimService implements Runnable{
		
		int space_to_occupy;
		public ReclaimService(Integer s) 
		{
			space_to_occupy=s;
		}

		@Override
		public void run() 
		{
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File peer_directory = new File(home.getAbsolutePath()+"/sdis/files/"+id);
			File restore_directory = new File(home.getAbsolutePath()+"/sdis/files/"+id+"/restore");
			
			if(peer_directory.exists()) 
			{
				long storage_occupied=Utils.checkDirectorySize(peer_directory);
			    
				if(restore_directory.exists()) 
				{
					storage_occupied-=Utils.checkDirectorySize(restore_directory);
				}
				
				if(storage_occupied<=space_to_occupy)
					return;
				else
				{
					long accumulator = 0;
					//key=fileID:chunkNo:size:desiredRepDegree value=perceivedRepDegree
					for (String key : records_store.keySet()) 
					{
						String[] keyComponents = key.split(":");
						int file_size = Integer.parseInt(keyComponents[2]);
						accumulator=accumulator+file_size;
						File to_delete= new File(peer_directory+"/"+keyComponents[0]+File.separator+keyComponents[1]);
						to_delete.delete();
						records_store.remove(key);
						if(storage_occupied-accumulator<=space_to_occupy)
							break;
					}
				}
			} 
			else return;
		}
	}
	
	public static void main(String[] args) throws IOException, AlreadyBoundException 
	{
		MulticastServer serv = new MulticastServer(args[0],args[1],args[2],args[3],Integer.parseInt(args[4]),args[5],Integer.parseInt(args[6]),args[7],Integer.parseInt(args[8]),Integer.parseInt(args[9]));
		serv.startUp();
	}

}
