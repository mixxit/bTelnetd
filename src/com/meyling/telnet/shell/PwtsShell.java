/* $Id: PwtsShell.java,v 1.7 2006/05/01 17:11:56 m31 Exp $
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
package com.meyling.telnet.shell;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.TerminalIO;
import net.wimpi.telnetd.io.terminal.BasicTerminal;
import net.wimpi.telnetd.net.Connection;
import net.wimpi.telnetd.net.ConnectionData;
import net.wimpi.telnetd.net.ConnectionEvent;
import net.wimpi.telnetd.shell.Shell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains the meat of this project. After the successfull connection
 * with the TelnetD framework from Dieter Wimberger this shell gets the baton.
 * <br>
 * It tries to start a external system shell and redirects the <code>stdin</code>,
 * <code>stdout</code> and <code>stderr</code> streams from the external shell
 * process to the telnet terminal connection.
 * <br>
 * The input from the terminal is line buffered to enable simple line editing
 * functionality. A drawback of this behaviour is that read requests of the external
 * process that wait for a single character are not directly supported. If
 * the external process waits for an 'y' you have to type in "y<code>CR</code>".
 * This should be no problem for getting the 'y'. But a subsequent read character
 * request from the external process gets the <code>CR</code> character.
 * <br>
 * The <code>stderr</code> output is merged into the terminal output (in red color)
 * as long stderr characters are available.
 *
 * @author    Michael Meyling
 */
public final class PwtsShell implements Shell {

    /** Trace logger. */
    private static Log trace = LogFactory.getLog(PwtsShell.class);

    /** ESC sequece for deleting a character. */
    private static final byte[] deleteChar
//        = new byte[] {BasicTerminal.ESC, BasicTerminal.LSB, 'X'};
        = new byte[] {BasicTerminal.ESC, BasicTerminal.LSB, '1', 'P'};

    /** ESC sequece for deleting a character. */
    private static final byte[] insertChar
        = new byte[] {BasicTerminal.ESC, BasicTerminal.LSB, '1', '@'};

    /** ESC sequece for moving back a character. */
    private static final byte[] moveBack
        = new byte[] {BasicTerminal.ESC, BasicTerminal.LSB, '1', 'D'};

    /** ESC sequece for saving cursor position. */
    private static final byte[] saveCursor
        = new byte[] {BasicTerminal.ESC, BasicTerminal.LSB, 's'};

    /** ESC sequece for restoring cursor position. */
    private static final byte[] restoreCursor
        = new byte[] {BasicTerminal.ESC, BasicTerminal.LSB, 'u'};

    /** Connection this shell works on. */
    private Connection connection;

    /** For low level terminal IO. */
    private ShellIo shellIo;

    /** Line buffer for stderr output. */
    private StringBuffer errorBuffer = new StringBuffer();

    /** Output handler. */
    private OutputStreamGobbler outputGobbler;

    private Process process;

    private Thread shellThread;

    public void run(final Connection con) {
        connection = con;
        shellIo = new ShellIo(connection);
        connection.addConnectionListener(this);

        try {
            shellIo.eraseScreen();
            shellIo.homeCursor();
            shellIo.setBold(true);
            shellIo.setForegroundColor(BasicTerminalIO.RED);
            shellIo.write("Poor Woman's Telnet Server");
            shellIo.setForegroundColor(BasicTerminalIO.GREEN);
            shellIo.setBold(false);
            shellIo.write(" by ");
            shellIo.setForegroundColor(BasicTerminalIO.CYAN);
            shellIo.setItalic(true);
            shellIo.write("Michael Meyling\r\n\r\n");
            shellIo.setItalic(false);
            final String[] welcome = Welcome.getRandomPicture(46);
            for (int i = 0; i < welcome.length; i++) {
                shellIo.write(welcome[i]);
            }
            shellIo.write("\r\n");
            shellIo.setForegroundColor(BasicTerminalIO.GREEN);
            final ConnectionData cd = connection.getConnectionData();
            final String connected = "Welcome " + cd.getHostName() +
            " [" + cd.getHostAddress() + ":" + cd.getPort() + "]";
            trace.error(connected);
            shellIo.write(connected + "\r\n\r\n");
            shellIo.setBold(false);
            shellIo.resetAttributes();
            shellIo.flush();
            executeShell();
        } catch (Exception e) {
            trace.fatal(e, e);
        } catch (Error e) {
            trace.fatal(e, e);
            throw e;
        } catch (Throwable e) {
            trace.fatal(e, e);
        } finally {
            // close shell process if connection is closed
            if (process != null) {
                process.destroy();
            }
        }
    }

    /** Execute the shell. */
    private void executeShell() throws IOException {
        final List list = new ArrayList();
        // get the operating system
        final String os = System.getProperty("os.name").toLowerCase(Locale.US);
        if (os.indexOf("windows") != -1) {
            if (os.indexOf("windows 9") != -1) {
                list.add(0, "command.com");
                list.add(1, "/A/E:1900");
            } else {
                list.add(0, "cmd.exe");
                list.add(1, "/A/E:ON/F:ON/Q");
            }
        } else {
            list.add(0, "/bin/sh");
        }
        // TODO mime 20060414: if no system shell is found what do we do?
        execSynchronized((String[]) list.toArray(new String[] {}));
    }

    public void connectionTimedOut(ConnectionEvent ce) {
        try {
            shellIo.setBold(true);
            shellIo.setForegroundColor(BasicTerminalIO.RED);
            shellIo.write("\r\n");
            shellIo.write("CONNECTION TIMEDOUT");
            shellIo.write("\r\n");
            shellIo.write("Bye bye");
            shellIo.write("\r\n");
            shellIo.flush();
            if (shellThread != null) {
                shellThread.interrupt();
            }
        } catch (Exception e) {
            trace.fatal(e, e);
        }
        connection.close();
    }

    public void connectionIdle(ConnectionEvent ce) {
        try {
            shellIo.write("\r\n");
            shellIo.write("CONNECTION IDLE (ignored)");
            shellIo.write("\r\n");
            shellIo.flush();
        } catch (Exception e) {
            trace.fatal(e, e);
        }
    }

    public void connectionLogoutRequest(ConnectionEvent ce) {
        try {
            shellIo.setBold(false);
            shellIo.setForegroundColor(BasicTerminalIO.GREEN);
            shellIo.write("\r\n");
            shellIo.write("CONNECTION LOGOUTREQUEST");
            shellIo.write("\r\n");
            shellIo.write("Bye bye");
            shellIo.write("\r\n");
            shellIo.flush();
            if (shellThread != null) {
                shellThread.interrupt();
            }
        } catch (Exception e) {
            trace.fatal(e, e);
        }
        connection.close();
    }

    public void connectionSentBreak(ConnectionEvent ce) {
        try {
            shellIo.write("\r\n");
            shellIo.write("CONNECTION BREAK (ignored)");
            shellIo.write("\r\n");
            shellIo.flush();
        } catch (Exception e) {
            trace.fatal(e, e);
        }
    }

    /**
     * Execute external process.
     *
     * @param   commandLineParameters   Comand line parameters. The first parameter must be an
     *                 executable program.
     * @return  Exit code of external program.
     * @throws     IOException  External call failed.
     */
    private int execSynchronized(String[] commandLineParameters)
            throws IOException {
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < commandLineParameters.length; i++) {
            if (i > 0) {
                buffer.append(" ");
            }
            buffer.append(commandLineParameters[i]);
        }
        try {
            trace.info("executing: " + buffer.toString());
            final File startDir = new File("/");
            process = Runtime.getRuntime().exec(commandLineParameters, (String[]) null, 
                startDir.getCanonicalFile());
        } catch (IOException e) {
            throw e;
        }
        // thread for process error stream
        final ErrorStreamGobbler errorGobbler = new
            ErrorStreamGobbler(process.getErrorStream());

        // thread for process output stream
        outputGobbler = new
                    OutputStreamGobbler(process.getInputStream());

        // thread for process input stream
        final InputStreamGobbler inputGobbler = new
            InputStreamGobbler(process.getOutputStream());

        // start them all
        errorGobbler.start();
        outputGobbler.start();
        inputGobbler.start();

        try {
            shellThread = Thread.currentThread();
            process.waitFor();
            trace.debug("exit code: " + process.exitValue());
            return process.exitValue();
        } catch (InterruptedException e) {
            trace.fatal("execution interupted. Called was: " + buffer);
            throw new IOException("process execution interrupted.");
        } finally {
            // close all those streams
            try {
                process.getInputStream().close();
            } catch (IOException e) {
                // ignore
            }
            try {
                process.getErrorStream().close();
            } catch (IOException e) {
                // ignore
            }
            try {
                process.getOutputStream().close();
            } catch (IOException e) {
                // ignore
            }
            process.destroy();
        }
    }

    public static Shell createShell() {
        return new PwtsShell();
    }


    /**
     * Passes shell output stream to terminal.
     */
    class OutputStreamGobbler extends Thread {

        /** Work on this stream. */
        final InputStream is;

        /**
         * Constructor.
         *
         * @param   is      Work on this stream.
         */
        OutputStreamGobbler(final InputStream is) {
            this.is = new BufferedInputStream(is);
        }

        public void run() {
            try {
                final StringBuffer outputBuffer = new StringBuffer();
                int c = '\0';
                while (-1 != (c = is.read()) && connection.isActive()) {
                    if (trace.isDebugEnabled()) {
                        trace.debug("STDOUT>" + (char) c);
                    }
                    outputBuffer.append((char) c);
                    if (0 == is.available()) {
                        // if there are some error messages, write
                        // them first
                        synchronized (errorBuffer) {
                            if (errorBuffer.length() > 0) {
                                try {
                                    errorBuffer.wait();
                                } catch (InterruptedException e) {
                                    trace.fatal(e, e);
                                }
                            }
                            shellIo.write(outputBuffer.toString());
                            shellIo.flush();
                            outputBuffer.setLength(0);
                        }
                    }
                }
            } catch (IOException e) {
                trace.warn(e, e);
            }
        }
    }

    /**
     * Passes shell error stream to terminal. Error output is in light red.
     */
    class ErrorStreamGobbler extends Thread {

        /** Work on this stream. */
        final InputStream is;

        /**
         * Constructor.
         *
         * @param   is      Work on this stream.
         */
        ErrorStreamGobbler(final InputStream is) {
            this.is = new BufferedInputStream(is);
        }

        public void run() {
            try {
                int c = '\0';
                while (-1 != (c = is.read()) && connection.isActive()) {
                    if (trace.isDebugEnabled()) {
                        trace.debug("STDERR>" + (char) c);
                    }
                    errorBuffer.append((char) c);
                    if (0 == is.available()) {
                        // we try to synchronize with the OutputStreamGobbler
                        synchronized (errorBuffer) {
                            shellIo.setForegroundColor(BasicTerminalIO.RED);
                            shellIo.setBold(true);
                            shellIo.write(errorBuffer.toString());
                            shellIo.setBold(false);
                            shellIo.resetAttributes();
                            shellIo.flush();
                            errorBuffer.setLength(0);
                            errorBuffer.notify();
                        }
                    }
                }
            } catch (SocketException e) {
                trace.warn("connection closed");
            } catch (IOException e) {
                trace.warn(e, e);
            }
        }
    }


    /**
     * Passes input stream to shell.
     */
    class InputStreamGobbler extends Thread {

        /** Work on this stream. */
        final OutputStream os;

        /**
         * Constructor.
         *
         * @param    os    Work on this stream.
         */
        InputStreamGobbler(final OutputStream os) {
            this.os = new BufferedOutputStream(os);
// TODO: any help or unecessary?
//            try {
//                this.os = new PrintStream(new BufferedOutputStream(os), false, "cp850");
//            } catch (UnsupportedEncodingException e) {
//                throw new RuntimeException(e);
//            }
        }

        public void run() {
            try {
                // list of previous entered lines
                final List lines = new ArrayList();
                // position within lines
                int lineNumber = -1;
                // line buffer
                final StringBuffer inputBuffer = new StringBuffer();
                // position within line buffer
                int cursor = 0;
                do {
                    int c = shellIo.read();
                    if (trace.isDebugEnabled()) {
                        trace.debug("STDIN> " + c + " " + (char)c);
                    }
                    switch (c) {
                    case BasicTerminalIO.DELETE:
                        trace.debug("STDIN> DELETE");
                        if (cursor < inputBuffer.length()) {
                            inputBuffer.deleteCharAt(cursor);
                            shellIo.write(deleteChar);
                        }
                        break;
                    case BasicTerminalIO.BACKSPACE:
                        trace.debug("STDIN> BACKSPACE");
                        if (cursor > 0) {
                            inputBuffer.deleteCharAt(cursor - 1);
                            cursor--;
                            shellIo.write((char) TerminalIO.BS);
//                            shellIo.moveLeft(1);
                            shellIo.write(deleteChar);
                        }
                        break;
                    case BasicTerminalIO.LEFT:
                        trace.debug("STDIN> LEFT");
                        if (cursor > 0) {
                            cursor--;
                            shellIo.write((char) TerminalIO.BS);
                        }
                        break;
                    case BasicTerminalIO.RIGHT:
                        trace.debug("STDIN> RIGHT");
                        if (cursor < inputBuffer.length()) {
                            shellIo.moveRight(1);
//                            m_IO.write(inputBuffer.charAt(cursor));
                            cursor++;
                        }
                        break;
                    case BasicTerminalIO.UP:
                        trace.debug("STDIN> UP");
                        if (lines.size() > 0 && lineNumber >= 0) {
                            if (inputBuffer.length() > cursor) {
//                                for (int i = cursor; i < inputBuffer.length(); i++) {
//                                    shellIo.write(deleteChar);
//                                }
                                shellIo.write(deleteChars(inputBuffer.length() - cursor));
                            }
                            for (int i = 0; i < cursor; i++) {
                                shellIo.write((char) TerminalIO.BS);
                                shellIo.write(deleteChar);
                            }
                            final String line = (String) lines.get(lineNumber);
                            if (lineNumber > 0) {
                                lineNumber--;
                            }
                            shellIo.write(line);
                            inputBuffer.setLength(0);
                            inputBuffer.append(line);
                            cursor = line.length();
                        }
                        break;
                    case BasicTerminalIO.DOWN:
                        trace.debug("STDIN> DOWN");
                        if (lineNumber >= 0 && lineNumber < lines.size()) {
                            if (inputBuffer.length() > cursor) {
                                shellIo.write(deleteChars(inputBuffer.length() - cursor));
                            }
                            for (int i = 0; i < cursor; i++) {
                                shellIo.write((char) TerminalIO.BS);
                                shellIo.write(deleteChar);
                            }
                            final String line = (String) lines.get(lineNumber);
                            if (lineNumber + 1 < lines.size()) {
                                lineNumber++;
                            }
                            shellIo.write(line);
                            inputBuffer.setLength(0);
                            inputBuffer.append(line);
                            cursor = line.length();
                        }
                        break;
                    case BasicTerminalIO.ENTER:
                        trace.debug("STDIN> ENTER");
                        shellIo.write(BasicTerminalIO.CRLF);
                        os.write(inputBuffer.toString().getBytes());
                        os.write((char) c);
                        os.flush();
                        cursor = 0;
                        lines.add(inputBuffer.toString());
                        lineNumber = lines.size() - 1;
                        inputBuffer.setLength(0);
                        break;
                    default:
                        if (c < 256) {
                            trace.debug("STDIN> no special char");
                            shellIo.write(insertChar);
                            shellIo.write((char) c);
                            inputBuffer.insert(cursor, (char) c);
                            cursor++;
                        } else {
                            trace.debug("STDIN> unknown char");
                        }
                    }
                } while (connection.isActive());
            } catch (SocketException e) {
                trace.warn("connection closed");
            } catch (IOException e) {
                trace.warn(e, e);
            }
        }
    }

    /**
     * Get ESC sequece for deleting <code>number</code> characters.
     * 
     *  @param  number  Delete this amount of characters.
     */
    private static final byte[] deleteChars(final int number) {
        final StringBuffer result = new StringBuffer();
        result.append((char) BasicTerminal.ESC);
        result.append((char) BasicTerminal.LSB);
        result.append(number);
        result.append('P');
        System.out.println(">>>" + result.toString());
        return result.toString().getBytes();
    }
    
}
