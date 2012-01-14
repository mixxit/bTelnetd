package net.gamerservices.telnetd;

import org.bukkit.plugin.java.JavaPlugin;

import com.meyling.telnet.startup.TelnetD;

public class telnetd extends JavaPlugin {

	TelnetD myTD = null;
	@Override
	public void onDisable() {
		myTD.stop();
		System.out.println("Ended TelnetD");
	}

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub
		System.out.println("Initialised TelnetD");
		

        try {
            myTD = TelnetD.createTelnetD();
            myTD.start();
        } catch (Exception e) {
            
            e.printStackTrace();
           
        }
	}

}
