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
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for a <a href="http://www.lirc.org">LIRC</a> client.
 * Functionally, it resembles the command line program irsend.
 */
public abstract class LircClient implements Closeable {

    public static final int defaultTimeout = 5000; // WinLirc can be really slow...
    public static final int EXITSUCCESS = 0;
    public static final int EXITUSAGEERROR = 1;
    public static final int EXITEXECUTIONERROR = 2;
    public static final String VERSION = "LircClient 0.1.0";
    public static final String encodingName = "US-ASCII";

    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();

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
        System.exit(success ? EXITSUCCESS : EXITEXECUTIONERROR);
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
        argumentParser.addCommand("set_input_log", cmdSetInputLog);

        CommandSetDriverOption cmdSetDriverOption = new CommandSetDriverOption();
        argumentParser.addCommand("set_driver_option", cmdSetDriverOption);

        CommandSetTransmitters cmdSetTransmitters = new CommandSetTransmitters();
        argumentParser.addCommand("set_transmitters", cmdSetTransmitters);

//        CommandSimulate cmdSimulate = new CommandSimulate();
//        argumentParser.addCommand("simulate", cmdSimulate);

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
                    if (cmdSendOnce.args.size() < 2)
                        doExit("Command \"send_once\" requires at least two arguments", EXITUSAGEERROR);
                    String remote = cmdSendOnce.args.get(0);
                    for (int i = 1; i < cmdSendOnce.args.size(); i++)
                        lircClient.sendIrCommand(remote, cmdSendOnce.args.get(i), cmdSendOnce.count); //  throw exception by failure
                    break;
                case "send_start":
                    lircClient.sendIrCommandRepeat(cmdSendStart.args.get(0), cmdSendStart.args.get(1));
                    break;
                case "send_stop":
                    if (cmdSendStop.args.isEmpty())
                        lircClient.stopIr();
                    else
                        lircClient.stopIr(cmdSendStop.args.get(0), cmdSendStop.args.get(1));
                    break;
                case "list":
                    List<String> result = cmdList.remote == null ? lircClient.getRemotes()
                            : lircClient.getCommands(cmdList.remote);
                    result.stream().forEach(System.out::println);
                    break;
                case "set_transmitters":
                    if (cmdSetTransmitters.transmitters.size() < 1)
                        doExit("Command \"set_transmitters\" requires at least one argument", EXITUSAGEERROR);
                    lircClient.setTransmitters(cmdSetTransmitters.transmitters);
                    break;
                case "version":
                    System.out.println(lircClient.getVersion());
                    break;
                case "set_driver_option":
                    if (cmdSetDriverOption.args.size() != 2)
                        doExit("Command \"set_driver_option\" requires at exactly two arguments", EXITUSAGEERROR);
                    lircClient.setDriverOption(cmdSetDriverOption.args.get(0), cmdSetDriverOption.args.get(1));
                    break;
                case "set_input_log":
                    lircClient.setInputLog(cmdSetInputLog.logfilePath.isEmpty() ? null : cmdSetInputLog.logfilePath.get(0));
                    break;
//                case "simulate":
//                    doExit("Command " + argumentParser.getParsedCommand() + " not implemented", EXITUSAGEERROR);
//                    break;
                default:
                    doExit("Unknown command", EXITUSAGEERROR);
            }
            doExit(success);
        } catch (IOException ex) {
            doExit(ex.getMessage(), EXITEXECUTIONERROR);
        } catch (IndexOutOfBoundsException ex) {
            doExit("Too few arguments to command " + argumentParser.getParsedCommand(), EXITUSAGEERROR);
        }
    }

    private static LircClient newLircClient(CommandLineArgs commandLineArgs) throws IOException {
        return commandLineArgs.socketPathname != null
                ? new UnixDomainSocketLircClient(commandLineArgs.socketPathname, commandLineArgs.verbose)
                : new TcpLircClient(commandLineArgs.address, commandLineArgs.port, commandLineArgs.verbose, commandLineArgs.timeout);
    }

    protected boolean verbose;

    private String lastRemote;
    private String lastCommand;
    protected OutputStream outToServer;
    protected BufferedReader inFromServer;

    protected LircClient(boolean verbose) {
        this.lastCommand = null;
        this.lastRemote = null;
        this.verbose = verbose;
    }

    protected LircClient() {
        this(false);
    }

    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    @Override
    public void close() throws IOException {
        outToServer.close();
        inFromServer.close();
    }

    private void sendBytes(byte[] cmd) throws IOException {
        outToServer.write(cmd);
        outToServer.flush(); // just to be safe
    }

    private void sendString(String cmd) throws IOException {
        sendBytes(cmd.getBytes(encodingName));
    }

    private List<String> sendCommand(String command) throws IOException {
        if (verbose)
            System.err.println("Sending command `" + command + "' to Lirc@" + socketName());

        sendString(command + '\n');

        ArrayList<String> result = new ArrayList<>(8);
        State state = State.BEGIN;
        int linesReceived = 0;
        int linesExpected = -1;

        while (state != State.DONE) {
            String line = inFromServer.readLine();
            if (verbose)
                System.err.println("Received \"" + line + "\"");

            if (line == null) {
                state = State.DONE;
                continue;
            }
            switch (state) {
                case BEGIN:
                    if (line.equals("BEGIN"))
                        state = State.MESSAGE;
                    break;
                case MESSAGE:
                    state = line.trim().equalsIgnoreCase(command) ? State.STATUS : State.BEGIN;
                    break;
                case STATUS:
                    switch (line) {
                        case "SUCCESS":
                            state = State.DATA;
                            break;
                        case "END":
                            state = State.DONE;
                            break;
                        case "ERROR":
                            throw new LircServerException("command failed: " + command);
                        default:
                            throw new BadPacketException("unknown response: " + command);
                    }
                    break;
                case DATA:
                    switch (line) {
                        case "END":
                            state = State.DONE;
                            break;
                        case "DATA":
                            state = State.N;
                            break;
                        default:
                            throw new BadPacketException("unknown response: " + command);
                    }
                    break;
                case N:
                    try {
                        linesExpected = Integer.parseInt(line);
                    } catch (NumberFormatException ex) {
                        throw new BadPacketException("integer expected; got: " + command);
                    }
                    state = linesExpected == 0 ? State.END : State.DATA_N;
                    break;
                case DATA_N:
                    result.add(line);
                    linesReceived++;
                    if (linesReceived == linesExpected)
                        state = State.END;
                    break;
                case END:
                    if (line.equals("END"))
                        state = State.DONE;
                    else
                        throw new BadPacketException("\"END\" expected but \"" + line + "\" received");
                    break;
                case DONE:
                    break;
                default:
                    throw new RuntimeException("Unhandled case (programming error)");
            }
        }
        if (verbose)
            System.err.println("Lirc command succeded.");

        return result;
    }

    public void sendIrCommand(String remote, String command, int count) throws IOException {
        this.lastRemote = remote;
        this.lastCommand = command;
        sendCommand("SEND_ONCE " + remote + " " + command + " " + (count - 1));
    }

    public void sendIrCommandRepeat(String remote, String command) throws IOException {
        this.lastRemote = remote;
        this.lastCommand = command;
        sendCommand("SEND_START " + remote + " " + command);
    }

    public void stopIr(String remote, String command) throws IOException {
        sendCommand("SEND_STOP " + remote + " " + command);
    }

    public void stopIr() throws IOException {
        sendCommand("SEND_STOP " + lastRemote + " " + lastCommand);
    }

    public List<String> getRemotes() throws IOException {
        return sendCommand("LIST");
    }

    public List<String> getCommands(String remote) throws IOException {
        if (remote == null || remote.isEmpty())
            throw new NullPointerException("Null remote");
        List<String> output = sendCommand("LIST " + remote);
        List<String> result = new ArrayList<>(output.size());
        output.stream().forEach((s) -> {
            result.add(s.replaceAll("\\S*\\s+", ""));
        });
        return result;
    }

    public void setTransmitters(List<Integer> transmitters) throws IOException {
        long mask = 0L;
        for (int transmitter : transmitters)
            mask |= (1L << (transmitter - 1));

        setTransmitters(mask);
    }

    public void setTransmitters(long mask) throws IOException {
        sendCommand("SET_TRANSMITTERS " + Long.toString(mask));
    }

    public String getVersion() throws IOException {
        List<String> result = sendCommand("VERSION");
        if (result.isEmpty())
            throw new LircServerException();
        return result.get(0);
    }

    public void setInputLog() throws IOException {
        setInputLog("null");
    }

    public void setInputLog(String logPath) throws IOException {
        sendCommand("SET_INPUTLOG " + (logPath == null ? "null" : logPath));
    }

    public void setDriverOption(String key, String value) throws IOException {
        sendCommand("DRV_OPTION " + key + " " + value);
    }

//    public boolean simulate(String key, String data) throws IOException {
//        return sendCommand("SIMULATE " + key + " " + data, false) != null;
//    }

    protected abstract String socketName();

    private static enum State {
        BEGIN,
        MESSAGE,
        STATUS,
        DATA,
        N,
        DATA_N,
        END,
        DONE // new
    }

    private static class BadPacketException extends IOException {
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
        private List<String> args = new ArrayList<>(16);
    }

    @Parameters(commandDescription = "Start sending one command until stopped")
    private final static class CommandSendStart {
        @Parameter(arity = 2, description = "remote command")
        private List<String> args = new ArrayList<>(2);
    }

    @Parameters(commandDescription = "Stop sending the command from send_start")
    private final static class CommandSendStop {
        @Parameter(arity = 2, description = "remote command")
        private List<String> args = new ArrayList<>(2);
    }

    @Parameters(commandDescription = "Inquire either the list of remotes, or the list of commands in a remote")
    private final static class CommandList {
        @Parameter(required = false, description = "[remote]")
        private String remote = null;
    }

    @Parameters(commandDescription = "Set input logging")
    private final static class CommandSetInputLog {
        @Parameter(required = false, description = "Path to log file, empty to stop logging")
        private List<String> logfilePath = new ArrayList<>(1);
    }

    @Parameters(commandDescription = "Set driver option")
    private final static class CommandSetDriverOption {
        @Parameter(arity = 2, description = "key value")
        private List<String> args = new ArrayList<>(2);
    }

//    @Parameters(commandDescription = "Fake reception of IR signals")
//    private final static class CommandSimulate {
//        @Parameter(arity = 2, description = "key value")
//        private List<String> simulateOptions = new ArrayList<>(2);
//    }

    @Parameters(commandDescription = "Set transmitters")
    private final static class CommandSetTransmitters {
        @Parameter(description = "transmitter...")
        private List<Integer> transmitters = new ArrayList<>(8);
    }

    @Parameters(commandDescription = "Inquire version of lircd")
    private final static class CommandVersion {
    }
}
