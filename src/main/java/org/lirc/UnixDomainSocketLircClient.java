package org.lirc;

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

    public UnixDomainSocketLircClient(String socketPath, boolean verbose, int timeout) {
        super(verbose, timeout);
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
