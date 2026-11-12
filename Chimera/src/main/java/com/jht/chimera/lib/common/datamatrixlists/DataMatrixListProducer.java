package com.jht.chimera.lib.common.datamatrixlists;

import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a helper class for managing big lists that can go through the data matrix.  For now
 * this only handles lists that grow during a workout.  It does not handle lists that can have
 * elements modified or deleted.  It does not handle inserts in the middle of the list.  Those are
 * future needs.  The initial case this is solving is sending graph and workout snapshot data across.
 * The basic concept is that the list is held in a service.  When a client wants it asks for the list
 * and then gets insert notifications when the list grows.  The key point here is that lists can get
 * larger than the 1 MB limit for Parcelables thus we may need to send the initial list in pages.
 * This class manages all of that.
 */
public class DataMatrixListProducer<T extends Parcelable> {

    private List<T> list = new ArrayList<>();
    private DataMatrixListKeys keys;
    private final Object lock = new Object();

    public DataMatrixListProducer(int elementSize, @NonNull DataMatrixList<T> dmList, @NonNull DataMatrix dm, @NonNull DataMatrixListKeys keys) {
        this.keys = keys;

        // Need to keep pages under 1MB
        int elementsPerPage = 1000000 / elementSize;
        dm.createEventAdapter(keys.requestKey, event -> {
            synchronized (lock) {
                DataMatrix.putInt(keys.responseKey, list.size());
                int numPages = list.size() / elementsPerPage;
                if (list.size() % elementsPerPage != 0) {
                    numPages++;
                }
                for (int i = 0; i < numPages; i++) {
                    dmList.list.clear();
                    for (int j = 0; j < elementsPerPage; j++) {
                        int index = i * elementsPerPage + j;
                        if (index >= list.size()) {
                            break;
                        }
                        dmList.list.add(list.get(index));
                    }
                    DataMatrix.putParcelable(keys.arrayKey, dmList);
                }
            }
        });
    }


    public void addElement(T element) {
        synchronized (lock) {
            list.add(element);
            DataMatrix.putParcelable(keys.elementKey, element);
        }
    }

    public void clearList() {
        synchronized (lock) {
            list.clear();
        }
    }

    public List<T> getList() {
        synchronized (lock) {
            return new ArrayList<>(list);
        }
    }

}
