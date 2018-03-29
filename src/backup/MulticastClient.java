package backup;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.*;
import java.util.*;

public class MulticastClient 
{	
	private static void printCommand(final String commandMessage)
	{
		System.out.print(String.format("(%s) Command sent: %s\n",
			(new Date()).toString(), commandMessage));
	}
	
	public static void main(String[] args) throws IOException, NotBoundException 
	{
		 String response;
		 if(args.length!=3 && args.length!=4) {
			 System.out.println("Usage: <RMI object> <sub_protocol> <opnd_1> [opnd_2(replication degree on backup)]");
		 }
			 
		 final Registry registry = LocateRegistry.getRegistry("127.0.0.1",1098);
         final RMIBackup stub = (RMIBackup) registry.lookup(args[0]);
         if(args[1].equals("backup"))
        	 response = stub.backup(args[2], Integer.parseInt(args[3]));
         else if(args[1].equals("delete"))
         	response = stub.delete(args[2]);
         else if(args[1].equals("restore"))
        	response = stub.restore(args[2]);
         else response = stub.reclaim(Integer.parseInt(args[2]));
         printCommand(response);
	}
}