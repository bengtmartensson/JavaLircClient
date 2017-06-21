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

package org.harctoolbox.lircclient;

import java.io.Closeable;
import java.io.IOException;

/**
 * An implementation of the LircClient using Unix domain sockets,
 * per default /var/run/lirc/lircd. To be written.
 */
public class UnixDomainSocketLircClient extends LircClient {

    public static final String defaultSocketPath = "/var/run/lirc/lircd";
    private final String socketPath;
    private Closeable socket;

    public UnixDomainSocketLircClient(String socketPath, boolean verbose) {
        super(verbose);
        this.socketPath = socketPath != null ? socketPath : defaultSocketPath;
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (socket != null)
            socket.close();
    }

    @Override
    protected final String socketName() {
        return socketPath;
    }
}
