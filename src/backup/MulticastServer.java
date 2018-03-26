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
	protected ServerRemoteObject rmi_object;
	protected Registry rmi_registry;
	protected ControlChannelListener control_thread;
	protected DataChannelListener data_thread;
	ConcurrentHashMap<String,Integer> records_backup;
	ConcurrentHashMap<String,Integer> records_store;
	
	
	public MulticastServer(String ident, String proto, String ap) throws IOException, AlreadyBoundException 
	{
		id=ident;
		protocol=proto;
		access_point=ap;
		records_backup = new ConcurrentHashMap<String,Integer>(); //load from file data
		records_store = new ConcurrentHashMap<String,Integer>();
		control_thread=new ControlChannelListener(id,records_backup,records_store);
		data_thread=new DataChannelListener(id,records_backup,records_store);
		initializeRMI();
	}
	
	private void startUp() 
	{
		new Thread(control_thread).start();
		new Thread(data_thread).start();
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
		MulticastServer serv = new MulticastServer(args[0],args[1],args[2]);
		serv.startUp();
		
	}

}
