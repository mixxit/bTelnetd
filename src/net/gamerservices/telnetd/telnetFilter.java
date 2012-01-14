package net.gamerservices.telnetd;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

import net.wimpi.telnetd.net.ConnectionFilter;

public class telnetFilter implements ConnectionFilter {

	@Override
	public void initialize(Properties props) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isAllowed(InetAddress ip) {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("plugins/telnetd.properties"));
			String allowedips = prop.getProperty("allowedips");
			String[] ips = allowedips.split(",");
			System.out.println("[TelnetD] Checking Filter for :" + ip);
			
			for (String curip : ips)
			{
				if (curip.equals(ip.toString()))
				{
					System.out.println("[TelnetD] "+ ip + " successfully logged in to shell");
					return true;
				}
			}
			
			

			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("[TelnetD] ALERT !!! ALERT !!! "+ ip + " FAILED TO LOGIN TO SHELL!!!!");
		return false;
	}
	

}
