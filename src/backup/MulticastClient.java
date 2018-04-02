package backup;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.*;
import java.net.InetAddress;
import java.util.*;

import javax.swing.filechooser.FileSystemView;

public class MulticastClient 
{	
	private static void printCommand(final String commandMessage)
	{
		System.out.print(String.format("(%s) Command sent: %s\n",
			(new Date()).toString(), commandMessage));
	}
	
	public static void main(String[] args) throws IOException, NotBoundException 
	{	
		 File home = FileSystemView.getFileSystemView().getHomeDirectory();
	 	 System.setProperty("java.rmi.server.codebase","file:\\"+home.getAbsolutePath()+"\\");
	 	 System.setProperty("java.security.policy","security.policy");
		 String response;
		 if(args.length!=3 && args.length!=4 && args.length!=2) {
			 System.out.println("Usage: <RMI object> <sub_protocol> <opnd_1> [opnd_2(replication degree on backup)]");
		 }
		 String[] argComponents = args[0].split("/");
		 final Registry registry = LocateRegistry.getRegistry(InetAddress.getByName(argComponents[2]).getHostAddress(),1099);
         final RMIBackup stub = (RMIBackup) registry.lookup(argComponents[3]);
         if(args[1].equals("backup"))
        	 response = stub.backup(args[2], Integer.parseInt(args[3]));
         else if(args[1].equals("delete"))
         	response = stub.delete(args[2]);
         else if(args[1].equals("restore"))
        	response = stub.restore(args[2]);
         else if(args[1].equals("reclaim"))
        	response = stub.reclaim(Integer.parseInt(args[2]));
         else response = stub.state();
         printCommand(response);
	}
}