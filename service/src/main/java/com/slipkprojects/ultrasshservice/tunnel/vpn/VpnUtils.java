package com.slipkprojects.ultrasshservice.tunnel.vpn;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.Runtime.getRuntime;
import java.util.Map;
import java.util.List;
import java.net.NetworkInterface;
import java.util.Collections;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.HashMap;
import android.annotation.TargetApi;
import android.os.Build;
import java.util.Collection;
import java.util.ArrayList;
import android.net.ConnectivityManager;
import java.lang.reflect.Method;
import android.net.LinkProperties;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.regex.Pattern;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public class VpnUtils {
	
	public static int findProcessId(String command) throws IOException {

        String[] cmds = {"ps -ef","ps -A","toolbox ps"};

        for (int i = 0; i < cmds.length;i++) {
            Process procPs = getRuntime().exec(cmds[i]);

            BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("PID") && line.contains(command)) {
                    String[] lineParts = line.split("\\s+");
                    try {
                        return Integer.parseInt(lineParts[1]); //for most devices it is the second
                    } catch (NumberFormatException e) {
                        return Integer.parseInt(lineParts[0]); //but for samsungs it is the first
                    } finally {
                        try {
                            procPs.destroy();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        return -1;
    }

    public static void killProcess(File fileProcBin) throws Exception {
        killProcess(fileProcBin, "-9"); // this is -KILL
    }
	
    public static int killProcess(File fileProcBin, String signal) throws Exception {

        int procId = -1;
        int killAttempts = 0;

        while ((procId = findProcessId(fileProcBin.getName())) != -1) {
            killAttempts++;
            String pidString = String.valueOf(procId);

            String[] cmds = {"","busybox ","toolbox "};

            for (int i = 0; i < cmds.length;i++) {
                try {
                    getRuntime().exec(cmds[i] + "killall " + signal + " " + fileProcBin.getName
									  ());
                } catch (IOException ioe) {
                }
                try {
                    getRuntime().exec(cmds[i] + "killall " + signal + " " + fileProcBin.getCanonicalPath());
                } catch (IOException ioe) {
                }
            }

            killProcess(pidString, signal);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignored
            }

            if (killAttempts > 4)
                throw new Exception("Cannot kill: " + fileProcBin.getAbsolutePath());
        }

        return procId;
    }

    public static void killProcess(String pidString, String signal) throws Exception {

        String[] cmds = {"","toolbox ","busybox "};

        for (int i = 0; i < cmds.length;i++) {
            try {
                getRuntime().exec(cmds[i] + "kill " + signal + " " + pidString);
            } catch (IOException ioe) {
                SkStatus.logDebug(String.format("error killing process: %s, %s", pidString, ioe.getMessage()));
            }
        }
    }
	
	public static List<String> getActiveNetworkDnsResolver(Context context) throws Exception {
        Collection<InetAddress> dnsResolvers = getActiveNetworkDnsResolvers(context);
        
		if (!dnsResolvers.isEmpty()) {
			ArrayList<String> lista = new ArrayList<String>();
            int max = 2;
			
			Iterator it = dnsResolvers.iterator();
			while (it.hasNext()) {
				String dnsResolver = it.next().toString();
				
				// strip the leading slash e.g., "/192.168.1.1"
				if (dnsResolver.startsWith("/")) {
					dnsResolver = dnsResolver.substring(1);
				}
				
				// remove ipv6 ips
				if (dnsResolver.contains(":")){
					continue;
				}
				
				lista.add(dnsResolver);
				
				max -= 1;
				if (max <= 0) break;
			}
			
            return lista;
        }
		else throw new Exception("no active network DNS resolver");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Collection<InetAddress> getActiveNetworkDnsResolvers(Context context)
	throws Exception {
        final String errorMessage = "getActiveNetworkDnsResolvers failed";
        ArrayList<InetAddress> dnsAddresses = new ArrayList<InetAddress>();
        try {
            // Hidden API
            // - only available in Android 4.0+
            // - no guarantee will be available beyond 4.2, or on all vendor devices
            ConnectivityManager connectivityManager =
				(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Class<?> LinkPropertiesClass = Class.forName("android.net.LinkProperties");
            Method getActiveLinkPropertiesMethod = ConnectivityManager.class.getMethod("getActiveLinkProperties", new Class []{});
            Object linkProperties = getActiveLinkPropertiesMethod.invoke(connectivityManager);
            if (linkProperties != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Method getDnsesMethod = LinkPropertiesClass.getMethod("getDnses", new Class []{});
                    Collection<?> dnses = (Collection<?>)getDnsesMethod.invoke(linkProperties);
                    for (Object dns : dnses) {
                        dnsAddresses.add((InetAddress)dns);
                    }
                } else {
                    // LinkProperties is public in API 21 (and the DNS function signature has changed)
                    for (InetAddress dns : ((LinkProperties)linkProperties).getDnsServers()) {
                        dnsAddresses.add(dns);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new Exception(errorMessage, e);
        } catch (NoSuchMethodException e) {
            throw new Exception(errorMessage, e);
        } catch (IllegalArgumentException e) {
            throw new Exception(errorMessage, e);
        } catch (IllegalAccessException e) {
            throw new Exception(errorMessage, e);
        } catch (InvocationTargetException e) {
            throw new Exception(errorMessage, e);
        } catch (NullPointerException e) {
            throw new Exception(errorMessage, e);
        }

        return dnsAddresses;
    }
	
	private final static String DEFAULT_PRIMARY_DNS_SERVER = "8.8.4.4";
    private final static String DEFAULT_SECONDARY_DNS_SERVER = "8.8.8.8";

	public static List<String> getNetworkDnsServer(Context context) {
        List<String> dnsResolver = new ArrayList<String>();
        try {
            dnsResolver = VpnUtils.getActiveNetworkDnsResolver(context);
        } catch (Exception e) {
            dnsResolver.add(DEFAULT_PRIMARY_DNS_SERVER);
			dnsResolver.add(DEFAULT_SECONDARY_DNS_SERVER);
        }
        return dnsResolver;
    }
	
	
	/**
	* Utils ip
	*/
	
	private static final Pattern IPV4_PATTERN =
	Pattern.compile(
		"^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    private static final Pattern IPV6_STD_PATTERN =
	Pattern.compile(
		"^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN =
	Pattern.compile(
		"^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6StdAddress(final String input) {
        return IPV6_STD_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6HexCompressedAddress(final String input) {
        return IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6Address(final String input) {
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }
	
	
	//----------------------------------------------------------------------------
	// Implementation: Network Utils
	//----------------------------------------------------------------------------

	public static class PrivateAddress
	{
		public final String mIpAddress;
		public final String mSubnet;
		public final int mPrefixLength;
		public final String mRouter;

		public PrivateAddress(String ipAddress, String subnet, int prefixLength, String router)
		{
			mIpAddress = ipAddress;
			mSubnet = subnet;
			mPrefixLength = prefixLength;
			mRouter = router;
		}
	}

	public static PrivateAddress selectPrivateAddress() throws Exception
	{
		// Select one of 10.0.0.1, 172.16.0.1, or 192.168.0.1 depending on
		// which private address range isn't in use.
		Map<String, PrivateAddress> candidates = new HashMap<String, PrivateAddress>();
		candidates.put("10", new PrivateAddress("10.0.0.1", "10.0.0.0", 8, "10.0.0.2"));
		candidates.put("172", new PrivateAddress("172.16.0.1", "172.16.0.0", 12, "172.16.0.2"));
		candidates.put("192", new PrivateAddress("192.168.0.1", "192.168.0.0", 16, "192.168.0.2"));
		candidates.put("169", new PrivateAddress("169.254.1.1", "169.254.1.0", 24, "169.254.1.2"));

		List<NetworkInterface> netInterfaces;
		try
		{
			netInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
		}
		catch (SocketException e)
		{
			e.printStackTrace();
			throw new Exception("selectPrivateAddress failed", e);
		}

		for (NetworkInterface netInterface : netInterfaces)
		{
			for (InetAddress inetAddress : Collections.list(netInterface.getInetAddresses()))
			{

				if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
				{
					String ipAddress = inetAddress.getHostAddress();
					if (ipAddress.startsWith("10."))
					{
						candidates.remove("10");
					}
					else if (ipAddress.length() >= 6
							 && ipAddress.substring(0, 6).compareTo("172.16") >= 0
							 && ipAddress.substring(0, 6).compareTo("172.31") <= 0)
					{
						candidates.remove("172");
					}
					else if (ipAddress.startsWith("192.168"))
					{
						candidates.remove("192");
					}
				}
			}
		}

		if (candidates.size() > 0)
		{
			return candidates.values().iterator().next();
		}

		throw new Exception("no private address available");
	}
	
	private static boolean isPortAvailable(int port)
    {
        Socket socket = new Socket();
        SocketAddress sockaddr = new InetSocketAddress("127.0.0.1", port);

        try 
        {
            socket.connect(sockaddr, 1000);
            // The connect succeeded, so there is already something running on that port
            return false;
        }
        catch (SocketTimeoutException e)
        {
            // The socket is in use, but the server didn't respond quickly enough
            return false;
        }
        catch (IOException e)
        {
            // The connect failed, so the port is available
            return true;
        }
        finally
        {
            if (socket != null)
            {
                try 
                {
                    socket.close();
                } 
                catch (IOException e) 
                {
                    /* should not be thrown */
                }
            }
        }
    }

    public static int findAvailablePort(int start_port, int max_increment)
    {
        for(int port = start_port; port < (start_port + max_increment); port++)
        {
            if (isPortAvailable(port))
            {
                return port;
            }
        }

        return 0;
    }
	
}
