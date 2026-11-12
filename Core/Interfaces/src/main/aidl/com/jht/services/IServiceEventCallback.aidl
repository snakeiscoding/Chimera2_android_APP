package com.jht.services;

parcelable ServiceEvent;

interface IServiceEventCallback {
    void onEvent(in ServiceEvent e);
}
