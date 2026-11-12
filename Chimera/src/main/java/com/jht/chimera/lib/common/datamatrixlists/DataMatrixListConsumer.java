package com.jht.chimera.lib.common.datamatrixlists;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.datamatrixlib.DataMatrix;
import com.jht.datamatrixlib.adapter.IAdapter;

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
public class DataMatrixListConsumer<T extends Parcelable, L extends DataMatrixList<T>> {
    public interface IOnGetList<T extends Parcelable> {
        void onGetList(List<T> list);
    }
    public interface IOnGetElement<T extends Parcelable> {
        void onGetElement(T element);
    }

    private List<T> list = new ArrayList<>();
    private int initialSize = 0;
    private boolean listInitialized = false;
    private IAdapter sizeAdapter;
    private IAdapter parcelableAdapter;
    private DataMatrixListKeys keys;

    public DataMatrixListConsumer(@NonNull DataMatrixListKeys keys) {
        this.keys = keys;
    }

    public void getList(@NonNull DataMatrix dm, @Nullable IOnGetList<T> onGetList, @Nullable IOnGetElement<T> onGetElement, @NonNull Class<L> listClass, @NonNull Class<T> elementClass) {
        listInitialized = false;
        sizeAdapter = dm.createIntAdapter(keys.responseKey, size -> {
            initialSize = size;
        });
        parcelableAdapter = dm.createParcelableAdapter(keys.arrayKey, listClass, list -> {
            this.list.addAll(list.list);
            if(this.list.size() >= initialSize) {
                listInitialized = true;
                dm.deregister(sizeAdapter);
                dm.deregister(parcelableAdapter);
                if(onGetList != null) {
                    onGetList.onGetList(this.list);
                }
            }
        });
        dm.createParcelableAdapter(keys.elementKey, elementClass, value -> {
            // There is a race condition where we could get an element from the server before getting
            // the initial list.  It is ok to ignore it because we will get this element when we get
            // the full initial list because these are synchronized in the server.
            if(listInitialized) {
                list.add(value);
                if (onGetElement != null) {
                    onGetElement.onGetElement(value);
                }
            }
        });
        DataMatrix.sendEvent(keys.requestKey);
    }

    public List<T> getList() {
        return list;
    }

    public void clearList() {
        list.clear();
    }

}
