package backup;
import java.net.*;
import java.io.*;
import java.util.*;

public class MulticastClient 
{
	private static int mcast_port, srvc_port;
	private static InetAddress mcast_addr, srvc_addr;
	
	private static void printCommand(final String commandMessage)
	{
		System.out.print(String.format("(%s) Command sent: %s\n",
			(new Date()).toString(), commandMessage));
	}
	
	public static void main(String[] args) throws IOException 
	{


	}
}
