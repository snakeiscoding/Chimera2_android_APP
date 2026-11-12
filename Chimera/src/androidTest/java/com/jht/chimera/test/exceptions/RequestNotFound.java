package com.jht.chimera.test.exceptions;

public class RequestNotFound extends Exception {
    private final int command;
    private final int version;
    public RequestNotFound(int command, int version) {
        this.command = command;
        this.version = version;
    }

    public int command() { return command; }
    public int version() { return version; }
}
