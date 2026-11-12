package com.jht.chimera.service.consoleconfig;

abstract class ConfigSetting {
    private final Object dataMatrixKey;

    ConfigSetting(Object dataMatrixKey) {
        this.dataMatrixKey = dataMatrixKey;
    }

    protected Object key() {
        return dataMatrixKey;
    }

    abstract void update(String text);

    abstract boolean getBool();
    abstract String getString();
    abstract int getInt();

    abstract String getType();
}


