package com.jht.chimera.lib.tv;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by mutombolin on 2019/02/12.
 */

public class SKNETChannelInfo extends SKNETTVChannelInfoBase implements Parcelable {
	private static final String TAG = "SKNETChannelInfo";
	private String frequency = "";
	private String tsId = "";
	private String serviceId = "";
	private String serviceName = "";
	private String channelNo = "";
	private boolean isDefault = false;
	
	protected SKNETChannelInfo(Parcel in) {
		frequency = in.readString();
		tsId = in.readString();
		serviceId = in.readString();
		serviceName = in.readString();
		channelNo = in.readString();
		isDefault = in.readByte() != 0;
	}
	
	public static final Creator<SKNETChannelInfo> CREATOR = new Creator<SKNETChannelInfo>() {
		@Override
		public SKNETChannelInfo createFromParcel(Parcel in) {
			return new SKNETChannelInfo(in);
		}
		
		@Override
		public SKNETChannelInfo[] newArray(int size) {
			return new SKNETChannelInfo[size];
		}
	};
	
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
	
	public boolean isDefault() {
		return isDefault;
	}
	
	
	public SKNETChannelInfo(String frequency, String tsId, String serviceId, String serviceName, String channelNo, boolean isDefault) {
		this.frequency = frequency;
		this.tsId = tsId;
		this.serviceId = serviceId;
		this.serviceName = serviceName;
		this.channelNo = channelNo;
		this.isDefault = isDefault;
	}
	
	public String getFrequency() {
		return frequency;
	}
	
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
	
	public String getTsId() {
		return tsId;
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public String getChannelNo() {
		return channelNo;
	}
	
	public void setTsId(String tsId) {
	
		this.tsId = tsId;
	}
	
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public void setChannelNo(String channelNo) {
		this.channelNo = channelNo;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		dest.writeString(frequency);
		dest.writeString(tsId);
		dest.writeString(serviceId);
		dest.writeString(serviceName);
		dest.writeString(channelNo);
		dest.writeByte((byte) (isDefault ? 1 : 0));
	}
	
	@Override
	public String getName() {
		return serviceName;
	}
	
	@Override
	public String getNumber() {
		return channelNo;
	}
	
	@Override
	public boolean isSkip() {
		return false;
	}

	@Override
	public boolean equals(Object object) {

//		this.frequency = frequency;
//		this.tsId = tsId;
//		this.serviceId = serviceId;
//		this.serviceName = serviceName;
//		this.channelNo = channelNo;
//		this.isDefault = isDefault;

		if(object instanceof SKNETChannelInfo) {
			SKNETChannelInfo sknetChannelInfo = (SKNETChannelInfo)object;
			Log.d(TAG, "equals: frequency = " + frequency + ", sknetChannelInfo.frequency = " + sknetChannelInfo.frequency);
			Log.d(TAG, "equals: tsId = " + tsId + ", sknetChannelInfo.tsId = " + sknetChannelInfo.tsId);
			Log.d(TAG, "equals: serviceId = " + serviceId + ", sknetChannelInfo.serviceId = " + sknetChannelInfo.serviceId);
			Log.d(TAG, "equals: serviceName = " + serviceName + ", sknetChannelInfo.serviceName = " + sknetChannelInfo.serviceName);
			Log.d(TAG, "equals: channelNo = " + channelNo + ", sknetChannelInfo.channelNo = " + sknetChannelInfo.channelNo);
			return 	frequency.equals(sknetChannelInfo.frequency) &&
					tsId.equals(sknetChannelInfo.tsId) &&
					serviceId.equals(sknetChannelInfo.serviceId) &&
					serviceName.equals(sknetChannelInfo.serviceName) &&
					channelNo.equals(sknetChannelInfo.channelNo);
		}
		Log.d(TAG, "equals: return false");
		return false;
	}
}
