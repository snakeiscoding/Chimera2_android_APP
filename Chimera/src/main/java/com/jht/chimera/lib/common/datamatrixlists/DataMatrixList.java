package com.jht.chimera.lib.common.datamatrixlists;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

abstract class DataMatrixList<T extends Parcelable> implements Parcelable{
    List<T> list = new ArrayList<>();

    public DataMatrixList() {}

    abstract protected ClassLoader getClassLoader();
    protected DataMatrixList(Parcel in) {
        int size = in.readInt();
        for(int i = 0; i < size; i++) {
            list.add(in.readParcelable(getClassLoader()));
        }
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(list.size());
        for( T value : list) {
            dest.writeParcelable(value, 0);
        }
    }
}
