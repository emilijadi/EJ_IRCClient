import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;


public class IRCClient implements Runnable
{
	private String IP;
	private String Nickname;
	private int Port;
	private String Channel;
	private Socket clientSocket;
	private PrintWriter Out;
	private BufferedReader In;
	private Scanner IOIn;
	private String RoomName="";
	
	private ArrayList<String> queries = new ArrayList<String>();
	private ArrayList<String> ServerMSG = new ArrayList<String>();	
	private Hashtable<String, ArrayList<String>> ChatRooms = new Hashtable<String, ArrayList<String>>();
	
	volatile boolean Waited = false;
	
	public IRCClient(String ip, int port, String nickname, String ch)
	{
		Port = port;
		IP = ip;
		Nickname = nickname;
		Channel = ch;
	}
	
	private void sendMessage(String ff)
	{
		if(!clientSocket.isConnected())
			return;
		
		Out.write(ff + "\r\n");
		Out.flush();			
	}
	private void serverInfo(String ss)
	{
		if(!clientSocket.isConnected())
			return;
		
		Out.write(ss + "\r\n");
		Out.flush();
		ServerMSG.add(ss);
	}
	
	private void addMessage(String roomName, String ss)
	{
		if(!ChatRooms.containsKey(roomName))
			return;
		
		ChatRooms.get(roomName).add(ss);		
		if(roomName.equals(RoomName))
			System.out.println(ss);
	}
	
	private void addMessageServer(String ss)
	{
		ServerMSG.add(ss);
		if(RoomName.length() == 0)
			System.out.println(ss);
	}
	private void addqueries(String hh)
	{
		queries.add(hh);
	}
	public final static void clearConsole()
	{
	    try {
	        if (System.getProperty("os.name").contains("Windows"))
	            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
	        else
	            Runtime.getRuntime().exec("clear");
	    } catch (IOException | InterruptedException ex) {}
	}
	private void printMessages()
	{
		
		if(RoomName.length() == 0)
		{
			printServerMessages();
			return;
		}
			
		ArrayList<String> messages = ChatRooms.get(RoomName);
		for(String e : messages)
			System.out.println(e);			
	}
	
	private void printServerMessages()
	{
		for(String e : ServerMSG)
			System.out.println(e);
	}
	
	public void start() throws UnknownHostException, IOException
	{		
		
		clientSocket = new Socket(IP, Port);
		
		Out = new PrintWriter(clientSocket.getOutputStream(), true);
        In = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        IOIn = new Scanner(System.in);
        
        new Thread(this).start();
        
        while(!Waited) {}
        
        System.out.println("[System] Connected!");

        serverInfo("NICK " + Nickname);
        serverInfo("USER " + Nickname + " 0 * :" + Nickname);
        serverInfo("JOIN " + "#" + Channel);
        
        
        String lastMessage = "";
        while(!lastMessage.equals("quit"))
        {
        	lastMessage = IOIn.nextLine();
        	if(lastMessage.startsWith("/"))
        	{
        		String query = lastMessage.substring(1, lastMessage.length());
        		addMessageServer(query);
        		addqueries(query);
        		if(query.contains("join"))
        		{       			
        			String curRoomName = query.substring(query.indexOf(' ')+1,query.length());
        			if(ChatRooms.containsKey(curRoomName))
        			{
        				
            			clearConsole();
        				RoomName = curRoomName;
        				System.out.println("[System] Switched to " + RoomName + " !");
        				printMessages();
        			}
        			else
        			{
        				
        				sendMessage(query);
        			}
        		}     
        		else if(query.startsWith("server"))
        		{
        			
        			clearConsole();
        			
        			RoomName = "";
        			System.out.println("[System] Switched to Server!");
        			printServerMessages();
        			continue;
        		}
        		
        		else
    			{
    				sendMessage(query);
    			}
        	}
        	
        	else
        	{
        		if(RoomName.equals(""))
        			continue;
        		
        		if(queries.size() != 0)
        		{
        			for(String e : queries)
        			{
        				if(e.contains("nick"))
        				{
        					String newNick = e.substring(e.indexOf("nick ")+5, e.length());
        					if(!newNick.equals(Nickname))
        					{
        						Nickname = newNick;
        						queries.remove(queries.indexOf(e));
        						break;
        					}
        				}
        			}
        			
        		}
        		sendMessage("PRIVMSG " + RoomName + " :" + lastMessage); 
        		
        		addMessage(RoomName, "[" + Nickname + "]: " + lastMessage);
        	}
        	
        }
        if(lastMessage.contains("quit"))
		{
        	this.clientSocket.close();
        	
    		System.exit(-1);
			
		}
        
        
        
	}
	
	public void run()
	{
		
		while(clientSocket.isConnected())
		{
			String read = "";
			try 
			{
				read = In.readLine();
				if(read == null)
					return;
				
				addMessageServer(read);
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			if(read.contains("Nickname is already in use."))
			{
					Nickname = Nickname.replaceAll(Nickname, Nickname+"_");
					try {
						start();
					} catch (IOException e) {
						System.out.println("");
					}
				
			}
			if(read.contains("Erroneous Nickname"))
			{
					
					try {
						start();
					} catch (IOException e) {
						
					}
				
			}
			
			if(read.contains("No Ident response"))
				Waited = true; 
			
			
			
			if(read.contains("PING"))
			{
				String send = read.replace("PING", "PONG");
				sendMessage(send);
				
	
			}
			if(read.contains(" 353 "))
			{
				int roomfind = read.indexOf("@ #");
				if(roomfind == -1)
					continue;
				
				String roomname = read.substring(roomfind+2, read.length());
				RoomName = roomname.substring(0, roomname.indexOf(' '));
				ChatRooms.put(RoomName, new ArrayList<String>());
				clearConsole();
				System.out.println("[System] Joined " + RoomName);			
			}
			if(read.contains("Cannot join channel"))
			{
				System.out.println("Cannot join channel... You must be invited ");
				
			}
			
			if(read.contains("JOIN #"))
			{
				String name = read.substring(read.indexOf("JOIN #") + 5, read.length());
				
				String user = read.substring(read.indexOf(':') +1, read.indexOf('!'));
				
				addMessage(name,"[" +user + "] Joined "+ name + " chat:)" );
			}
			
			else if(read.contains("PRIVMSG #"))
			{
				
				String sub1 = read.substring(read.indexOf("PRIVMSG #") + 8);
				String roomname = sub1.substring(0, sub1.indexOf(':')-1);
				String message = sub1.substring(sub1.indexOf(':')+1);
				
				String user = read.substring(read.indexOf(':') +1, read.indexOf('!'));
				addMessage(roomname, "[" + user + "]: " + message);
		
				
			}
			if(read.contains("quit"))
			{
				String sub1 = read.substring(read.indexOf("PRIVMSG #") + 8);
				String roomname = sub1.substring(0, sub1.indexOf(':')-1);
		
				String user = read.substring(read.indexOf(':') +1, read.indexOf('!'));
				addMessage(roomname, "[System]: " +user+"left the chat!");
			}
			
		}
		
	}
}
