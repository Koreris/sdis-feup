package backup;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

public class MulticastServer
{
	private String id;
	private String protocol;
	private String access_point;
	private Integer storage_capacity;
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
		storage_capacity=storage;
		records_backup = new ConcurrentHashMap<String,Integer>(); //load from file data
		records_store = new ConcurrentHashMap<String,Integer>();
		records_restore = new ConcurrentHashMap<String,Integer>();
		control_thread=new ControlChannelListener(id,records_backup,records_store,records_restore,control_adr,control_port,recovery_adr,recovery_port);
		data_thread=new DataChannelListener(id,records_backup,records_store,data_adr,data_port,storage_capacity,control_adr,control_port);
		recovery_thread=new RecoveryChannelListener(id,records_backup,records_store,records_restore,recovery_adr,recovery_port,control_adr,control_port);
		initializeRMI();
	}
	
	private void startUp() 
	{
		new Thread(control_thread).start();
		new Thread(data_thread).start();
		new Thread(recovery_thread).start();
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
