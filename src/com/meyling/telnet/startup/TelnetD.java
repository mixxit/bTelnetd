/* $Id: TelnetD.java,v 1.7 2006/05/01 21:33:27 m31 Exp $
 *
 * This file is part of the project "Poor Woman's Telnet Server".
 *
 *   http://pwts.sourceforge.net/
 *
 * Copyright 2006,  Michael Meyling <michael@meyling.com>.
 *
 * "PWTS" is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.meyling.telnet.startup;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.wimpi.telnetd.BootException;
import net.wimpi.telnetd.io.terminal.TerminalManager;
import net.wimpi.telnetd.net.PortListener;
import net.wimpi.telnetd.shell.ShellManager;
import net.wimpi.telnetd.util.PropertiesLoader;
import net.wimpi.telnetd.util.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class that implements a telnet server.
 * <br>
 * This class is based on the original {@link net.wimpi.telnetd.TelnetD}.
 * Now the property file location is not much variable any longer. It should
 * be located in a file "config/telnetd.properties" or in the classpath.
 *
 * @author    Michael Meyling    (I copied and adapted the code somewhat)
 */
public class TelnetD {

    /** Trace logger. */
    private static Log trace = LogFactory.getLog(TelnetD.class);

    /** The one and only instance of this telnet server. */
    private static TelnetD instance = null;

    /** All port listeners. */
    private List listeners;

    /** Shell manager. */
    private ShellManager shellManager;

    /**
     * Constructor creating a TelnetD instance.<br>
     *
     * Private so that only the factory method can create the singleton
     * instance.
     */
    private TelnetD() {
        instance = this;
        listeners = new ArrayList(5);
    }

    /**
     * Start the telnet server.
     *
     * @param args    Not used.
     */
    public static void main(String[] args) {
        
    }

    /**
     * Start this telnet daemon, respectively all configured listeners.
     */
    public void start() {
        trace.debug("start()");
        for (int i = 0; i < listeners.size(); i++) {
            final PortListener plis = (PortListener) listeners.get(i);
            plis.start();
        }
    }

    /**
     * Stop this telnet daemon, respectively all configured listeners.
     */
    public void stop() {
        trace.debug("stop()");
        for (int i = 0; i < listeners.size(); i++) {
            final PortListener plis = (PortListener) listeners.get(i);
            // shutdown the PortListener resources
            plis.stop();
        }
    }

    /**
     * Method to prepare the ShellManager.<br>
     *
     * Creates and prepares a Singleton instance of the ShellManager, with
     * settings from the passed in Properties.
     *
     * @param settings
     *            Properties object that holds main settings.
     * @throws BootException
     *             if preparation fails.
     */
    private void prepareShellManager(Properties settings) throws BootException {
        // use factory method for creating mgr singleton
        shellManager = ShellManager.createShellManager(settings);
        if (shellManager == null) {
            trace.fatal("creation of shell manager failed");
            System.exit(1);
        }
    }

    /**
     * Method to prepare the PortListener.<br>
     *
     * Creates and prepares and runs a PortListener, with settings from the
     * passed in Properties. Yet the Listener will not accept any incoming
     * connections before startServing() has been called. this has the advantage
     * that whenever a TelnetD Singleton has been factorized, it WILL 99% not
     * fail any longer (e.g. serve its purpose).
     *
     * @param   name            Name of listner.
     * @param     settings        Properties object that holds main settings.
     * @throws     BootException    Preparation failed.
     */
    private void prepareListener(final String name, final Properties settings)
            throws BootException {

        int port = 0;
        try {
            port = Integer.parseInt(settings.getProperty(name + ".port"));
            ServerSocket socket = new ServerSocket(port);
            socket.close();
        } catch (NumberFormatException e) {
            trace.fatal(e, e);
            throw new BootException("Failure while parsing port number for \"" + name + ".port\": " 
                + e.getMessage());
        } catch (IOException e) {
            trace.fatal(e, e);
            throw new BootException("Failure while starting listener for port number " + port + ": "
                + e.getMessage());
        }
        // factorize PortListener
        final PortListener listener = PortListener.createPortListener(name, settings);
        // start the Thread derived PortListener
        try {
            listeners.add(listener);
        } catch (Exception e) {
            trace.fatal(e, e);
            throw new BootException("Failure while starting PortListener thread: "
                + e.getMessage());
        }

    }

    private void prepareTerminals(final Properties terminals) throws BootException {
        TerminalManager.createTerminalManager(terminals);
    }

    /**
     * Factory method to create a TelnetD Instance.
     *
     * @param     main    Properties object with settings for the TelnetD.
     * @return     TenetD instance that has been properly set up according to
     *             the passed in properties, and is ready to start serving.
     * @throws    BootException    Setup process failed.
     */
    public static TelnetD createTelnetD(final Properties main) throws BootException {

        if (instance == null) {
            final TelnetD td = new TelnetD();
            td.prepareShellManager(main);
            td.prepareTerminals(main);
            final String[] listnames = StringUtil.split(
                main.getProperty("listeners"), ",");
            for (int i = 0; i < listnames.length; i++) {
                td.prepareListener(listnames[i], main);
            }
            return td;
        } else {
            throw new BootException("Singleton already instantiated.");
        }

    }

    /**
     * Factory method to create a TelnetD singleton instance, loading the
     * standard properties files from the location
     *   <code>config/telnetd.properties</code>.
     *
     * @return     TenetD instance that has been properly set up according to the
     *             passed in properties, and is ready to start serving.
     * @throws     BootException    Setup process failed.
     */
    public static TelnetD createTelnetD() throws BootException {
        try {
            Properties properties = new Properties();
            // try to load properties from classpath
            properties.load(new FileInputStream("plugins/telnetd.properties"));
            return createTelnetD(properties);
        } catch (IOException e) {
            trace.fatal(e, e);
            throw new BootException("Failed to load configuration file.");
        }
    }

    /**
     * Accessor method for the Singleton instance of this class.
     *
     * @return TelnetD singleton instance reference.
     */
    public synchronized static TelnetD getInstance() {
        if (instance != null) {
            return ((TelnetD) instance);
        } else {
            return null;
        }
    }

}
