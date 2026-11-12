package com.jht.chimera.lib.common.rfid;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.rfid.Legic.RFIDChip;

public class PFBarcodeData implements Parcelable {
    private RFIDChip chip;
    private String barcode;

    @Override
    public int describeContents() {
        return 0;
    }

    public PFBarcodeData(RFIDChip chip, String barcode) {
        this.chip = chip;
        this.barcode = barcode;
    }

    public PFBarcodeData(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<PFBarcodeData> CREATOR = new Creator<PFBarcodeData>() {
        @Override
        public PFBarcodeData createFromParcel(Parcel source) {
            return new PFBarcodeData(source);
        }

        @Override
        public PFBarcodeData[] newArray(int size) {
            return new PFBarcodeData[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(chip, 0);
        dest.writeString(barcode);
    }

    public void readFromParcel(Parcel source) {
        chip = source.readParcelable(RFIDChip.class.getClassLoader());
        barcode = source.readString();
    }

    public RFIDChip chip() {
        return chip;
    }

    public String barcode() { return barcode; }
}
