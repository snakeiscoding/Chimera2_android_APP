package com.jht.chimera.service.hal.lib.hardware;

public interface IDriver {
    int safeOpen(String name);
    void safeClose(int fd);
    int readInt(int fd);
    int read(int fd, byte[] data);
    int write(int fd, byte[] data);
    int waitForRead(int fd, int timeoutMs);
}
