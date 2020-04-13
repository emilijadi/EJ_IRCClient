import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main
{
	public static void main(String[] args) throws UnknownHostException, IOException 
	{
		Scanner input = new Scanner(System.in);
		System.out.println("IRC Client");
		System.out.print("Enter your unique nickname: ");
		String nickname = input.nextLine();
		//System.out.print("Enter your IRC server: "); //chat.freenode.net
		//String hostname = input.nextLine();
		System.out.print("Enter your channel: "); 
		String channel = input.nextLine();
		
		IRCClient cl = new IRCClient("irc.freenode.net",6667,nickname, channel);
		cl.start();
	}

}
