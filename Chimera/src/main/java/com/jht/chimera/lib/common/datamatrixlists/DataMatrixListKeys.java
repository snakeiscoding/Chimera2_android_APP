package com.jht.chimera.lib.common.datamatrixlists;

import androidx.annotation.NonNull;

class DataMatrixListKeys {
    public Object requestKey;
    public Object responseKey;
    public Object arrayKey;
    public Object elementKey;

    DataMatrixListKeys(@NonNull Object requestKey, @NonNull Object responseKey, @NonNull Object arrayKey, @NonNull Object elementKey) {
        this.requestKey = requestKey;
        this.responseKey = responseKey;
        this.arrayKey = arrayKey;
        this.elementKey = elementKey;
    }
}
