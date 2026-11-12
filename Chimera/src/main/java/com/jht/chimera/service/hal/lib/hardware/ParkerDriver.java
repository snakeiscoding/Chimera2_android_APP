package com.jht.chimera.service.hal.lib.hardware;

public class ParkerDriver implements IDriver {
    @Override
    public int safeOpen(String name) {
        return Driver.safeOpen(name);
    }

    @Override
    public void safeClose(int fd) {
        Driver.safeClose(fd);
    }

    @Override
    public int readInt(int fd) {
        return Driver.readInt(fd);
    }

    @Override
    public int read(int fd, byte[] data) {
        return Driver.read(fd, data);
    }

    @Override
    public int write(int fd, byte[] data) {
        return Driver.write(fd, data);
    }

    @Override
    public int waitForRead(int fd, int timeoutMs) {
        return Driver.waitForRead(fd, timeoutMs);
    }
}
