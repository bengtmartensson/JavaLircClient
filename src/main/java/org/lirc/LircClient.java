/*
Copyright (C) 2016 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.lirc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for a <a href="http://www.lirc.org">LIRC</a> client.
 * Functionally, it resembles the command line program irsend.
 */
public abstract class LircClient implements Closeable {

    public final static int defaultTimeout = 5000; // WinLirc can be really slow...
    private final static int P_BEGIN = 0;
    private final static int P_MESSAGE = 1;
    private final static int P_STATUS = 2;
    private final static int P_DATA = 3;
    private final static int P_N = 4;
    private final static int P_DATA_N = 5;
    private final static int P_END = 6;
    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();
    private static int EXITSUCCESS = 0;
    private static int EXITUSAGEERROR = 1;
    private static int EXITIOERROR = 2;
    private static int EXITFATALERROR = 3;
    private static String VERSION = "LircClient 0.1.0";
    protected static final String encodingName = "US-ASCII";

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder(256);
        argumentParser.usage(str);

        (exitcode == 0 ? System.out : System.err).println(str);
        doExit(exitcode); // placifying FindBugs...
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    private static void doExit(boolean success) {
        if (!success)
            System.err.println("Failed");
        System.exit(success ? EXITSUCCESS : EXITIOERROR);
    }
    private static void doExit(String message, int exitcode) {
        System.err.println(message);
        System.exit(exitcode);
    }

    /**
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setCaseSensitiveOptions(false);
        argumentParser.setAllowAbbreviatedOptions(true);
        argumentParser.setProgramName("LircClient");

        CommandSendOnce cmdSendOnce = new CommandSendOnce();
        argumentParser.addCommand("send_once", cmdSendOnce);

        CommandSendStart cmdSendStart = new CommandSendStart();
        argumentParser.addCommand("send_start", cmdSendStart);

        CommandSendStop cmdSendStop = new CommandSendStop();
        argumentParser.addCommand("send_stop", cmdSendStop);

        CommandList cmdList = new CommandList();
        argumentParser.addCommand("list", cmdList);

        CommandSetInputLog cmdSetInputLog = new CommandSetInputLog();
        argumentParser.addCommand("set_inputlog", cmdSetInputLog);

        CommandSetDriverOption cmdSetDriverOption = new CommandSetDriverOption();
        argumentParser.addCommand("set_driver_option", cmdSetDriverOption);

        CommandSetTransmitters cmdSetTransmitters = new CommandSetTransmitters();
        argumentParser.addCommand("set_transmitters", cmdSetTransmitters);

        CommandSimulate cmdSimulate = new CommandSimulate();
        argumentParser.addCommand("simulate", cmdSimulate);

        CommandVersion cmdVersion = new CommandVersion();
        argumentParser.addCommand("version", cmdVersion);

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(EXITUSAGEERROR);
        }

        if (commandLineArgs.helpRequested)
            usage(EXITSUCCESS);

        if (commandLineArgs.versionRequested) {
            System.out.println(VERSION);
            doExit(true);
        }

        try (LircClient lircClient = newLircClient(commandLineArgs)) {
            if (argumentParser.getParsedCommand() == null)
                usage(EXITUSAGEERROR);

            boolean success = true;
            switch (argumentParser.getParsedCommand()) {
                case "send_once":
                    String remote = cmdSendOnce.commands.get(0);
                    cmdSendOnce.commands.remove(0);
                    for (String command : cmdSendOnce.commands) {
                        success = lircClient.sendIrCommand(remote, command, cmdSendOnce.count);
                        if (!success)
                            break;
                    }
                    break;
                case "send_start":
                    success = lircClient.sendIrCommandRepeat(cmdSendStart.args.get(0), cmdSendStart.args.get(1));
                    break;
                case "send_stop":
                    success = lircClient.stopIr(cmdSendStop.args.get(0), cmdSendStop.args.get(1));
                    break;
                case "list":
                    String[] result = cmdList.remote.isEmpty() ? lircClient.getRemotes()
                            : lircClient.getCommands(cmdList.remote.get(0));
                    for (String s : result)
                        System.out.println(s);
                    break;
                case "set_transmitters":
                    if (cmdSetTransmitters.transmitters.size() < 1)
                        doExit("Command \"set_transmitters\" requires at least one argument", EXITUSAGEERROR);

                    //LircTransmitter xmitter = new LircTransmitter(cmdSetTransmitters.transmitters);
                    success = lircClient.setTransmitters(cmdSetTransmitters.transmitters);
                    break;
                case "version":
                    System.out.println(lircClient.getVersion());
                    break;
                case "simulate":
                case "set_driver_option":
                case "set_input_log":
                    doExit("Command " + argumentParser.getParsedCommand() + " not yet implemented", EXITUSAGEERROR);
                    break;
                default:
                    doExit("Unknown command", EXITUSAGEERROR);
            }
            doExit(success);
        } catch (IOException ex) {
            doExit(ex.getMessage(), EXITFATALERROR);
        } catch (IndexOutOfBoundsException ex) {
            doExit("Too few arguments to command", EXITUSAGEERROR);
        }
    }

    private static LircClient newLircClient(CommandLineArgs commandLineArgs) throws IOException {
        return commandLineArgs.socketPathname != null
                ? new UnixDomainSocketLircClient(commandLineArgs.socketPathname, commandLineArgs.verbose, commandLineArgs.timeout)
                : new TcpLircClient(commandLineArgs.address, commandLineArgs.port, commandLineArgs.verbose, commandLineArgs.timeout);
    }

    protected boolean verbose = false;
    protected int timeout = defaultTimeout;

    private String lastRemote = null;
    private String lastCommand = null;
    protected OutputStream outToServer;
    protected BufferedReader inFromServer;

    protected LircClient(boolean verbose, int timeout) {
        this.timeout = timeout;
        this.verbose = verbose;
    }

    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public void close() throws IOException {
        outToServer.close();
        inFromServer.close();
    }

    private void sendBytes(byte[] cmd) throws IOException {
        outToServer.write(cmd);
    }

    private void sendString(String cmd) throws IOException {
        sendBytes(cmd.getBytes(encodingName));
    }

    private final String[] sendCommand(String packet, boolean oneWord) throws IOException {
        if (verbose)
            System.err.println("Sending command `" + packet + "' to Lirc@" + socketName());

        sendString(packet + '\n');

        ArrayList<String> result = new ArrayList<>(8);
        int status = 0;
        try {
            int state = P_BEGIN;
            int n = 0;
            boolean done = false;
            int dataN = -1;

            while (!done) {
                String string = inFromServer.readLine();
                if (verbose)
                    System.err.println("Received `" + string + "'");
                if (string == null) {
                    done = true;
                    status = -1;
                } else {
                    OUTER:
                    switch (state) {
                        case P_BEGIN:
                            if (!string.equals("BEGIN")) {
                                System.err.println("!begin");
                                continue;
                            }
                            state = P_MESSAGE;
                            break;
                        case P_MESSAGE:
                            if (!string.trim().equalsIgnoreCase(packet)) {
                                state = P_BEGIN;
                                continue;
                            }
                            state = P_STATUS;
                            break;
                        case P_STATUS:
                            switch (string) {
                                case "SUCCESS":
                                    status = 0;
                                    break;
                                case "END":
                                    status = 0;
                                    done = true;
                                    break;
                                case "ERROR":
                                    System.err.println("command failed: " + packet);
                                    status = -1;
                                    break;
                                default:
                                    throw new BadPacketException();
                            }
                            state = P_DATA;
                            break;
                        case P_DATA:
                            switch (string) {
                                case "END":
                                    done = true;
                                    break OUTER;
                                case "DATA":
                                    state = P_N;
                                    break OUTER;
                            }
                            throw new BadPacketException();
                        case P_N:
                            //errno = 0;
                            dataN = Integer.parseInt(string);
                            //result = new String[data_n];

                            state = dataN == 0 ? P_END : P_DATA_N;
                            break;
                        case P_DATA_N:
                            // Different LIRC servers seems to deliver commands in different
                            // formats. Just take the last word.
                            result.add(oneWord ? string.replaceAll("\\S*\\s+", "") : string);
                            n++;
                            if (n == dataN) {
                                state = P_END;
                            }
                            break;
                        case P_END:
                            if (string.equals("END")) {
                                done = true;
                            } else {
                                throw new BadPacketException();
                            }
                            break;
                        default:
                            assert false : "Unhandled case";
                            break;
                    }
                }
            }
        } catch (BadPacketException e) {
            System.err.println("bad return packet");
            status = -1;
        } catch (SocketTimeoutException e) {
            System.err.println("Sockettimeout Lirc: " + e.getMessage());
            result = null;
            status = -1;
        } catch (IOException e) {
            System.err.println("Couldn't read from " + socketName());
            status = -1;
        }
        if (verbose)
            System.err.println("Lirc command " + (status == 0 ? "succeded." : "failed."));

        return status == 0 && result != null ? result.toArray(new String[result.size()]) : null;
    }

    public boolean sendIrCommand(String remote, String command, int count) throws IOException {
        this.lastRemote = remote;
        this.lastCommand = command;
        return sendCommand("SEND_ONCE " + remote + " " + command + " " + (count - 1), false) != null;
    }

    public boolean sendIrCommandRepeat(String remote, String command) throws IOException {
        this.lastRemote = remote;
        this.lastCommand = command;
        return sendCommand("SEND_START " + remote + " " + command, false) != null;
    }

    public boolean stopIr(String remote, String command) throws IOException {
            return sendCommand("SEND_STOP " + remote + " " + command, false) != null;
    }

    public boolean stopIr() throws IOException {
        return stopIr(lastRemote, lastCommand);
    }

    public String[] getRemotes() throws IOException {
        return sendCommand("LIST", false);
    }

    public String[] getCommands(String remote) throws IOException {
        if (remote == null || remote.isEmpty())
            throw new NullPointerException("Null remote");
        return sendCommand("LIST " + remote, true);
    }

    /**
     * Sends the SET_TRANSMITTER command to the LIRC server.
     * @param transmitters
     * @return
     * @throws IOException
     */
    public boolean setTransmitters(List<Integer> transmitters) throws IOException {
        long mask = 0L;
        for (int transmitter : transmitters)
            mask |= (1L << (transmitter - 1));

        return setTransmitters(mask);
    }

    private boolean setTransmitters(long mask) throws IOException {
        String s = "SET_TRANSMITTERS " + Long.toString(mask);
        return sendCommand(s, false) != null;
    }

    public String getVersion() throws IOException {
        String[] result = sendCommand("VERSION", false);
        String version = (result == null || result.length == 0) ? null : result[0];
        return version;
    }

    protected abstract String socketName();

    private static class BadPacketException extends Exception {
        BadPacketException() {
            super();
        }

        BadPacketException(String message) {
            super(message);
        }
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-a", "--address"}, description = "IP name or address of lircd host")
        private String address = "localhost";

        @Parameter(names = {"-d", "--device"}, description = "Path name of lircd socket")
        private String socketPathname = null; // /var/run/lirc/lircd

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-p", "--port"}, description = "Port of lircd")
        private int port = 8765;

        @Parameter(names = {"-t", "--timeout"}, description = "Timeout in milliseconds")
        private int timeout = 5000;

        @Parameter(names = {"--version"}, description = "Display version information for this program")
        private boolean versionRequested;

        @Parameter(names = {"-v", "--verbose"}, description = "Have some commands executed verbosely")
        private boolean verbose;
    }

    @Parameters(commandDescription = "Send one or many commands")
    private final static class CommandSendOnce {
        @Parameter(names = {"-#", "-c", "--count"}, description = "Number of times to send command in send_once")
        private int count = 1;

        @Parameter(description = "remote command...")
        private List<String> commands = new ArrayList<>(16);
    }

    @Parameters(commandDescription = "Send one commandm possibly several many times")
    private final static class CommandSendStart {
        @Parameter(arity = 2, description = "remote command")
        private List<String> args = new ArrayList<>(2);
    }

    @Parameters(commandDescription = "Send one commands many times")
    private final static class CommandSendStop {
        @Parameter(arity = 2, description = "remote command")
        private List<String> args = new ArrayList<>(2);
    }

    @Parameters(commandDescription = "Inquire either the list of remotes, or the list of commands in a remote")
    private final static class CommandList {
        @Parameter(required = false, description = "[remote]")
        private List<String> remote = new ArrayList<>(1);
    }

    @Parameters(commandDescription = "Set input logging")
    private final static class CommandSetInputLog {
        @Parameter(required = false, description = "Path to log file")
        private String logfilePath = null;
    }

    @Parameters(commandDescription = "Set driver options")
    private final static class CommandSetDriverOption {
        @Parameter(arity = 2, description = "key value")
        private List<String> driverOptions = new ArrayList<>(2);
    }

    @Parameters(commandDescription = "Fake reception of IR signals")
    private final static class CommandSimulate {
        // not yet implemented
    }

    @Parameters(commandDescription = "Set transmitters")
    private final static class CommandSetTransmitters {
        @Parameter(description = "transmitter...")
        private List<Integer> transmitters = new ArrayList<>(8);
    }

    @Parameters(commandDescription = "Inquire version of lircd")
    private final static class CommandVersion {
    }
}
