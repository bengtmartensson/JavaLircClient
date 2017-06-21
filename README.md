# JavaLircClient  [![Build Status](https://travis-ci.org/bengtmartensson/JavaLircClient.svg?branch=master)](https://travis-ci.org/bengtmartensson/JavaLircClient)
Java implementation of a Lirc client for communicating with a Lircd server. It contains
both an API and a command line interface. The command line interface, found in LircClient.java,
roughly resembles the Lirc program [irsend](http://lirc.org/html/irsend.html).

The abstract class LircClient is implemented using TCP sockets in the class TcpLircClient,
and using Unix Domain sockets (`/var/run/lirc/lircd`) in the class UnixDomainSocketLircClient
(which is presently not implemented, but just a skeleton). (However, this is not a very
severe restriction, since the Lircd server can be started with the `--listen` option.)