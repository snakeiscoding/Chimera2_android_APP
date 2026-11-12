package com.jht.chimera.lib.tv.settopbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mutombolin on 2019/03/19.
 */

public class SKNETDeviceInfo implements Parcelable {
	private String model = "";
	private String version = "";
	private String mac = "";
	private String bcas = "";
	private int maintenanceMode = 0;
	
	public SKNETDeviceInfo() {
	}
	
	protected SKNETDeviceInfo(Parcel in) {
		model = in.readString();
		version = in.readString();
		mac = in.readString();
		bcas = in.readString();
		maintenanceMode = in.readInt();
	}
	
	public static final Creator<SKNETDeviceInfo> CREATOR = new Creator<SKNETDeviceInfo>() {
		@Override
		public SKNETDeviceInfo createFromParcel(Parcel in) {
			return new SKNETDeviceInfo(in);
		}
		
		@Override
		public SKNETDeviceInfo[] newArray(int size) {
			return new SKNETDeviceInfo[size];
		}
	};
	
	public void setModel(String model) {
		this.model = model;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public void setMac(String mac) {
		this.mac = mac;
	}
	
	public void setBcas(String bcas) {
		this.bcas = bcas;
	}
	
	public void setMaintenanceMode(int maintenanceMode) {
		this.maintenanceMode = maintenanceMode;
	}
	
	public String getModel() {
	
		return model;
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getMac() {
		return mac;
	}

	/*
	public String getBcas() {
		return bcas;
	}
	
	public int getMaintenanceMode() {
		return maintenanceMode;
	}
	 */
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		dest.writeString(model);
		dest.writeString(version);
		dest.writeString(mac);
		dest.writeString(bcas);
		dest.writeInt(maintenanceMode);
	}
	
	@Override
	public String toString() {
		return "Model: " + model + "\n" +
		    "Version: " + version + "\n" +
		    "MAC: " + mac + "\n" +
		    "B-CAS: " + bcas + "\n" +
		    "MaintenanceMode: " + maintenanceMode;
	}
}
