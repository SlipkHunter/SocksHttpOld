package com.slipkprojects.ultrasshservice.tunnel.vpn;

public interface TunnelConstants {
    int FILE_WRITE_BUFFER_SIZE = 2048;

    String SHELL_CMD_PS = "toolbox ps";

	String APP_SOCKSVPN_KEY = "socksvpnkey";
	
	String ACTION_SERVICE_STOP = "actionstopservice";
	String ACTION_SERVICE_RESTART = "actionservicerestart";
	
	int VPN_INTERFACE_MTU = 1500;
	String VPN_INTERFACE_NETMASK = "255.255.255.0";
	String[] DNS_RESOLVERS_IP = {"8.8.8.8", "8.8.4.4"};
	int DNS_RESOLVER_PORT = 53;
	
	String PREFS_DNS_PORT= "PREFS_DNS_PORT";

    String PREFS_KEY_TORIFIED = "PrefTord";

    /**
     * Keep this in sync with the one in TorServiceUtils
     */
    String PREF_TOR_SHARED_PREFS = "org.torproject.android_preferences";
	
}
