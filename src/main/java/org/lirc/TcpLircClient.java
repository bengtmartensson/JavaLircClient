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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * An implementation of the LircClient using an TCP port, per default localhost at port 8765.
 */
public final class TcpLircClient extends LircClient {

    public final static int lircDefaultPort = 8765;
    public final static String defaultLircIP = "127.0.0.1"; // localhost
    private final String lircServerIp;
    private final int port;
    private InetAddress inetAddress;
    private final Socket socket;

    public TcpLircClient(String address, int port, boolean verbose, int timeout) throws UnknownHostException, IOException {
        super(verbose);
        lircServerIp = (address != null) ? address : defaultLircIP;
        inetAddress = InetAddress.getByName(lircServerIp);
        this.port = port;

        socket = new Socket();
        if (verbose)
            System.err.println("Connecting socket to " + socketName());

        socket.connect(new InetSocketAddress(inetAddress, port), timeout);
        socket.setSoTimeout(timeout);
        socket.setKeepAlive(true);

        outToServer = socket.getOutputStream();

        InputStream inStream = socket.getInputStream();
        Charset charSet = Charset.forName(encodingName);
        inFromServer = new BufferedReader(new InputStreamReader(inStream, charSet));
    }

    public TcpLircClient(String address, int port) throws IOException {
        this(address, port, false, defaultTimeout);
    }

    @Override
    protected String socketName() {
        return inetAddress.getCanonicalHostName() + ":" + Integer.toString(port);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (socket != null)
            socket.close();
    }
}
