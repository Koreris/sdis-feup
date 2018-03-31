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

	private String access_point;
	private ScheduledThreadPoolExecutor record_keeper;
	private ServerRemoteObject rmi_object;
	private Registry rmi_registry;
	protected Integer storage_capacity;
	protected String id;
	protected  String protocol_version;
	protected ControlChannelListener control_thread;
	protected DataChannelListener data_thread;
	protected RecoveryChannelListener recovery_thread;
	protected ConcurrentHashMap<String,Integer> records_backup;
	protected ConcurrentHashMap<String,Integer> records_store;
	protected ConcurrentHashMap<String,Integer> records_restore;
	protected ConcurrentHashMap<String,Integer> volatile_store_records;
	protected String control_address,data_address,recovery_address;
	protected int control_port,data_port,recovery_port;


	public MulticastServer(String ident, String proto, String ap, String control_adr, Integer controlport, String data_adr, int dataport, String recovery_adr, int recoveryport, int storage) throws IOException, AlreadyBoundException 
	{
		id=ident;
		protocol_version=proto;
		access_point=ap;
		storage_capacity=storage*1000;
		control_address=control_adr;
		data_address=data_adr;
		recovery_address=recovery_adr;
		control_port=controlport;
		data_port=dataport;
		recovery_port=recoveryport;
		records_backup = new ConcurrentHashMap<String,Integer>(); //load from file data
		records_store = new ConcurrentHashMap<String,Integer>();
		records_restore = new ConcurrentHashMap<String,Integer>();
		volatile_store_records = new ConcurrentHashMap<String,Integer>();
		record_keeper= new ScheduledThreadPoolExecutor(2);
		control_thread=new ControlChannelListener(this);
		data_thread=new DataChannelListener(this);
		recovery_thread=new RecoveryChannelListener(this);
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
	
	
	public static void main(String[] args) throws IOException, AlreadyBoundException 
	{
		MulticastServer serv = new MulticastServer(args[0],args[1],args[2],args[3],Integer.parseInt(args[4]),args[5],Integer.parseInt(args[6]),args[7],Integer.parseInt(args[8]),Integer.parseInt(args[9]));
		serv.startUp();
	}

}
