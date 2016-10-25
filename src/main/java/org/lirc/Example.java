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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An example of using the API of LircClient to send and receive information from a Lirc server.
 */
public class Example {

    public static void main(String[] args) {
        try {
            LircClient lirc = new TcpLircClient("localhost", 8765);
            String version = lirc.getVersion();
            System.out.println(version);
            String[] remotes = lirc.getRemotes();
            if (remotes == null) {
                System.err.println("null receved");
                System.exit(1);
            }
            for (int i = 0; i < remotes.length; i++)
                System.out.println(i + ":\t" + remotes[i]);

            System.out.println("Select a remote by entering its number");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, LircClient.encodingName));
            String line = reader.readLine();
            int remoteNo = Integer.parseInt(line);
            String remote = remotes[remoteNo];
            String[] commands = lirc.getCommands(remote);
            for (int i = 0; i < commands.length; i++)
                System.out.println(i + ":\t" + commands[i]);
            System.out.println("Select a command by entering its number");
            line = reader.readLine();
            int commandNo = Integer.parseInt(line);
            String command = commands[commandNo];
            boolean success = lirc.sendIrCommand(remote, command, 1);
            System.out.println(success ? "success" : "fail");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Example() {
    }
}
