package com.slipkprojects.ultrasshservice.tunnel.vpn;

import android.os.Parcelable;
import android.os.Parcel;

public class TunnelVpnSettings
	implements Parcelable {
	
	public String mSocksServer;
	public boolean mDnsForward;
	public String[] mDnsResolver;
	public String mUdpResolver;
	public String[] mExcludeIps;
	public boolean mUdpDnsRelay;
	
	public boolean mEnableFilterApps;
	public boolean mFilterBypassMode;
	public String[] mFilterApps;
	
	public boolean mTetheringSubnet;

	public TunnelVpnSettings(String socksServer, boolean dnsForward, String[] dnsResolver,
		boolean udpDnsRelay, String udpResolver, String[] excludeIps,
			boolean enableFilterApps, boolean filterBypassMode, String[] filterApps,
				boolean enableTethering)
	{
		mSocksServer = socksServer;
		mDnsForward = dnsForward;
		mUdpDnsRelay = udpDnsRelay;
		mDnsResolver = dnsResolver;
		mUdpResolver = udpResolver;
		mExcludeIps = excludeIps;
		
		mEnableFilterApps = enableFilterApps;
		mFilterBypassMode = filterBypassMode;
		mFilterApps = filterApps;
		
		mTetheringSubnet = enableTethering;
	}
	
	@Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSocksServer);
        dest.writeInt(mDnsForward ? 1 : 0);
		dest.writeInt(mUdpDnsRelay ? 1 : 0);
        dest.writeStringArray(mDnsResolver);
		dest.writeString(mUdpResolver);
		dest.writeStringArray(mExcludeIps);
		dest.writeInt(mFilterBypassMode ? 1 : 0);
		dest.writeStringArray(mFilterApps);
		dest.writeInt(mEnableFilterApps ? 1 : 0);
		dest.writeInt(mTetheringSubnet ? 1 : 0);
    }
	
	public TunnelVpnSettings(Parcel in) {
        mSocksServer = in.readString();
		mDnsForward = in.readInt() == 1;
		mUdpDnsRelay = in.readInt() == 1;
		mDnsResolver = in.createStringArray();
		mUdpResolver = in.readString();
		mExcludeIps = in.createStringArray();
		mFilterBypassMode = in.readInt() == 1;
		mFilterApps = in.createStringArray();
		mEnableFilterApps = in.readInt() == 1;
		mTetheringSubnet = in.readInt() == 1;
    }
	
	public static final Creator<TunnelVpnSettings> CREATOR
	= new Creator<TunnelVpnSettings>() {
        public TunnelVpnSettings createFromParcel(Parcel in) {
            return new TunnelVpnSettings(in);
        }

        public TunnelVpnSettings[] newArray(int size) {
            return new TunnelVpnSettings[size];
        }
    };
}
