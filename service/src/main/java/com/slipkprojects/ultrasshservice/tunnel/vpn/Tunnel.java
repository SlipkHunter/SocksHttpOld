/*
 * Copyright (c) 2015, Psiphon Inc.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
 
package com.slipkprojects.ultrasshservice.tunnel.vpn;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.io.FileDescriptor;
import java.io.File;
import java.util.ArrayList;
import android.system.OsConstants;
import java.util.Iterator;
import java.util.Collection;
import android.content.Intent;
import android.app.PendingIntent;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.ultrasshservice.SocksHttpService;
import android.content.pm.PackageManager;

/**
* Baseado no Cordova Plugin Tun2Socks
* @author dFiR30n
*/

public class Tunnel
{

	public interface HostService
	{
		public String getAppName();

		public Context getContext();

		// Object must be a VpnService; Android < 4 cannot reference this class name
		public Object getVpnService();

		// Object must be a VpnService.Builder;
		// Android < 4 cannot reference this class name
		public Object newVpnServiceBuilder();

		public void onDiagnosticMessage(String message);

		public void onTunnelConnected();

		public void onVpnEstablished();
	}

	private final HostService mHostService;
	private VpnUtils.PrivateAddress mPrivateAddress;
	private AtomicReference<ParcelFileDescriptor> mTunFd;
	private AtomicBoolean mRoutingThroughTunnel;
	private Tun2Socks mTun2Socks;
	private Pdnsd mPdnsd;
	private NetworkSpace mRoutes;

	// Only one VpnService instance may exist at a time, as the underlying
	// tun2socks implementation contains global state.
	private static Tunnel mTunnel;

	public static synchronized Tunnel newTunnel(HostService hostService)
	{
		if (mTunnel != null)
		{
			mTunnel.stop();
		}
		mTunnel = new Tunnel(hostService);
		return mTunnel;
	}

	private Tunnel(HostService hostService)
	{
		mHostService = hostService;
		mTunFd = new AtomicReference<ParcelFileDescriptor>();
		mRoutingThroughTunnel = new AtomicBoolean(false);
		mRoutes = new NetworkSpace();
		
		//org.uproxy.tun2socks.Tun2SocksJni.init();
	}

	public Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException();
	}

	//----------------------------------------------------------------------------------------------
	// Public API
	//----------------------------------------------------------------------------------------------

	// To start, call in sequence: startRouting(), then startTunneling(). After startRouting()
	// succeeds, the caller must call stop() to clean up.

	// Returns true when the VPN routing is established; returns false if the VPN could not
	// be started due to lack of prepare or revoked permissions (called should re-prepare and
	// try again); throws exception for other error conditions.
	public synchronized boolean startRouting(TunnelVpnSettings settings) throws java.lang.Exception
	{
		return startVpn(settings.mDnsForward, settings.mDnsResolver, settings.mExcludeIps,
			settings.mEnableFilterApps, settings.mFilterBypassMode, settings.mFilterApps, settings.mTetheringSubnet);
	}

	// Starts tun2socks. Returns true on success.
	public synchronized boolean startTunneling(String socksServerAddress, String[] dnsResolver, boolean forwardDns, String udpResolver, boolean udpDnsRelay)
	throws Exception
	{
		return routeThroughTunnel(socksServerAddress, dnsResolver, forwardDns, udpResolver, udpDnsRelay);
	}

	// Stops routing traffic through the tunnel by stopping tun2socks.
	// The VPN is unaffected by this method.
	public synchronized void stopTunneling()
	{
		stopRoutingThroughTunnel();
	}

	// Note: to avoid deadlock, do not call directly from a HostService callback;
	// instead post to a Handler if necessary to trigger from a HostService callback.
	public synchronized void stop()
	{
		stopVpn();
	}

	//----------------------------------------------------------------------------
	// VPN Routing
	//----------------------------------------------------------------------------

	private static final String VPN_INTERFACE_NETMASK = "255.255.255.0";
	private static final String DNS_RESOLVER_IP = "8.8.8.8";
	private static final int DNS_RESOLVER_PORT = 53;
	
	private int mMtu = 1500;
	
	// Note: Atomic variables used for getting/setting local proxy port, routing flag, and
	// tun fd, as these functions may be called via callbacks. Do not use
	// synchronized functions as stop() is synchronized and a deadlock is possible as callbacks
	// can be called while stop holds the lock.
	//
	private boolean startVpn(boolean forwardDns, String[] dnsResolver, String[] excludeIps,
			boolean enabledFilter, boolean filterBypassMode, String[] filterApps, boolean enableTethering) throws java.lang.Exception
	{
		StringBuilder routeMessage = new StringBuilder("Routes: ");
		StringBuilder routeExcludeMessage = new StringBuilder("Routes Excluded: ");
		
		mPrivateAddress = VpnUtils.selectPrivateAddress();
		
		// routes list
		for (String ip : excludeIps) {
			mRoutes.addIP(new CIDRIP(ip, 32), false);
		}
		
		Locale previousLocale = Locale.getDefault();

		final String errorMessage = "startVpn failed";
		try
		{
			// Workaround for https://code.google.com/p/android/issues/detail?id=61096
			Locale.setDefault(new Locale("en"));

			ParcelFileDescriptor tunFd = null;
			
			// inicia VpnBuilder
			VpnService.Builder builder =
					((VpnService.Builder) mHostService.newVpnServiceBuilder())
					.addAddress(mPrivateAddress.mIpAddress, mPrivateAddress.mPrefixLength);
				
			mRoutes.addIP(new CIDRIP("0.0.0.0", 0), true);
			mRoutes.addIP(new CIDRIP("10.0.0.0", 8), false);
			mRoutes.addIP(new CIDRIP(mPrivateAddress.mSubnet, mPrivateAddress.mPrefixLength), false);
			
			// ainda pra testar, subnet routing pesquisar "allow lan"
			if (enableTethering) {
                // USB tethering 192.168.42.x
                // Wi-Fi tethering 192.168.43.x
                mRoutes.addIP(new CIDRIP("192.168.42.0", 23), false);
                // Bluetooth tethering 192.168.44.x
                mRoutes.addIP(new CIDRIP("192.168.44.0", 24), false);
                // Wi-Fi direct 192.168.49.x
                mRoutes.addIP(new CIDRIP("192.168.49.0", 24), false);
            }
			
			// Add Dns
			for (String dns : dnsResolver) {
				try {
					builder.addDnsServer(dns);
					mRoutes.addIP(new CIDRIP(dns, 32), forwardDns);
				} catch (IllegalArgumentException iae) {
					mHostService.onDiagnosticMessage(String.format("Erro ao adicinar dns %s, %s", dns, iae.getLocalizedMessage()));
				}
			}
			
			// set MTU
			String release = Build.VERSION.RELEASE;
			if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                && mMtu < 1280) {
				SkStatus.logInfo(String.format(Locale.US, "Forcing MTU to 1280 instead of %d to workaround Android Bug #70916", mMtu));
				mMtu = 1280;
			}
			builder.setMtu(mMtu);
			
			// add routes
			Collection<NetworkSpace.IpAddress> include_routes = mRoutes.getNetworks(true);
			Iterator<NetworkSpace.IpAddress> it = include_routes.iterator();
			while (it.hasNext()) {
				NetworkSpace.IpAddress ip = it.next();
				
				routeMessage.append(String.format("%s/%d", ip.getIPv4Address(), ip.networkMask));
				routeMessage.append(", ");
			}
			routeMessage.deleteCharAt(routeMessage.lastIndexOf(", "));
			
			Collection<NetworkSpace.IpAddress> exclude_routes = mRoutes.getNetworks(false);
			Iterator<NetworkSpace.IpAddress> it2 = exclude_routes.iterator();
			while (it2.hasNext()) {
				NetworkSpace.IpAddress ip = it2.next();

				routeExcludeMessage.append(String.format("%s/%d", ip.getIPv4Address(), ip.networkMask));
				routeExcludeMessage.append(", ");
			}
			routeExcludeMessage.deleteCharAt(routeExcludeMessage.lastIndexOf(", "));
			
			mHostService.onDiagnosticMessage(routeMessage.toString());
			mHostService.onDiagnosticMessage(routeExcludeMessage.toString());
			
			// loop routes
			NetworkSpace.IpAddress multicastRange = new NetworkSpace.IpAddress(new CIDRIP("224.0.0.0", 3), true);
			
			for (NetworkSpace.IpAddress route : mRoutes.getPositiveIPList()) {
				try {
					if (multicastRange.containsNet(route))
						SkStatus.logDebug("VPN: Ignoring multicast route: " + route.toString());
					else
						builder.addRoute(route.getIPv4Address(), route.networkMask);
				} catch (IllegalArgumentException ia) {
					mHostService.onDiagnosticMessage("Route rejeitada: " + route + " " + ia.getLocalizedMessage());
				}
			}
			
			if (Build.VERSION.SDK_INT >= 21) {
				if (enabledFilter) {
					for (String app_pacote : filterApps) {
						try {
							if (filterBypassMode) {
								builder.addDisallowedApplication(app_pacote);
								mHostService.onDiagnosticMessage(String.format("Filtro de Apps: Vpn desativada para \"%s\"", app_pacote));
							}
							else {
								builder.addAllowedApplication(app_pacote);
								mHostService.onDiagnosticMessage(String.format("Filtro de Apps: Vpn ativada para \"%s\"", app_pacote));
							}
						} catch(PackageManager.NameNotFoundException e) {
							mHostService.onDiagnosticMessage("Aplicativo \"" + app_pacote + "\" não encontrado. Filtro de Apps não irá funcionar, verifique as configurações.");
						}
					}
				}
			}
			
			// TunFd
			tunFd = builder
					.setSession(mHostService.getAppName())
					.setConfigureIntent(SocksHttpService.getGraphPendingIntent(mHostService.getContext()))
					.establish();

			if (tunFd == null)
			{
				// As per http://developer.android.com/reference/android/net/VpnService.Builder.html#establish%28%29,
				// this application is no longer prepared or was revoked.
				return false;
			}
			
			mTunFd.set(tunFd);
			mRoutingThroughTunnel.set(false);
			mHostService.onVpnEstablished();
			
			mRoutes.clear();
			
		}
		catch (IllegalArgumentException e)
		{
			throw new Exception(errorMessage, e);
		}
		catch (SecurityException e)
		{
			throw new Exception(errorMessage, e);
		}
		catch (IllegalStateException e)
		{
			throw new Exception(errorMessage, e);
		}
		finally
		{
			// Restore the original locale.
			Locale.setDefault(previousLocale);
		}

		return true;
	}
	
	private boolean routeThroughTunnel(final String socksServerAddress, final String[] dnsResolver, boolean forwardDns, final String udpResolver, final boolean transparentDns)
	{
		if (!mRoutingThroughTunnel.compareAndSet(false, true))
		{
			return false;
		}
		
		final ParcelFileDescriptor tunFd = mTunFd.get();
		if (tunFd == null)
		{
			return false;
		}
		
		// Pdnsd
		String dnsgwRelay = null;
		if (forwardDns) {
			
			int pdnsdPort = VpnUtils.findAvailablePort(8091, 10);
			
			String[] mServidorDNS = dnsResolver;
			dnsgwRelay = String.format("%s:%d", mPrivateAddress.mIpAddress, pdnsdPort);

			mPdnsd = new Pdnsd(mHostService.getContext(), mServidorDNS, DNS_RESOLVER_PORT,
				mPrivateAddress.mIpAddress, pdnsdPort);
			mPdnsd.setOnPdnsdListener(new Pdnsd.OnPdnsdListener(){
				@Override
				public void onStart(){
					mHostService.onDiagnosticMessage("pdnsd started");
				}
				@Override
				public void onStop(){
					mHostService.onDiagnosticMessage("pdnsd stopped");
					stop();
				}
			});

			mPdnsd.start();
		}
		
		// Tun2socks
		mTun2Socks = new Tun2Socks(mHostService.getContext(), tunFd, mMtu,
			mPrivateAddress.mRouter, VPN_INTERFACE_NETMASK, socksServerAddress,
				udpResolver, dnsgwRelay, transparentDns);

		mTun2Socks.setOnTun2SocksListener(new Tun2Socks.OnTun2SocksListener(){
			@Override
			public void onStart()
			{
				mHostService.onDiagnosticMessage("tun2socks started");
			}
			@Override
			public void onStop()
			{
				mHostService.onDiagnosticMessage("tun2socks stopped");
				stop();
			}
		});

		mTun2Socks.start();
		
		mHostService.onTunnelConnected();
		mHostService.onDiagnosticMessage("routing through tunnel");

		// TODO: should double-check tunnel routing; see:
		// https://bitbucket.org/psiphon/psiphon-circumvention-system/src/1dc5e4257dca99790109f3bf374e8ab3a0ead4d7/Android/PsiphonAndroidLibrary/src/com/psiphon3/psiphonlibrary/TunnelCore.java?at=default#cl-779
		return true;
	}

	private void stopRoutingThroughTunnel()
	{
		if (mTun2Socks != null && mTun2Socks.isAlive()) {
			mTun2Socks.interrupt();
		}
		
		mTun2Socks = null;
		
		// teste
		//org.torproject.android.service.vpn.Tun2Socks.Stop();
		//ca.psiphon.PsiphonTunnel.Stop();
		
		if (mPdnsd != null && mPdnsd.isAlive()) {
			mPdnsd.interrupt();
		}
        	
		mPdnsd = null;
	}

	private void stopVpn()
	{
		stopRoutingThroughTunnel();
		
		ParcelFileDescriptor tunFd = mTunFd.getAndSet(null);
		if (tunFd != null)
		{
			try
			{
				mHostService.onDiagnosticMessage("closing VPN interface");
				tunFd.close();
			}
			catch (IOException e) {}
		}
	}

}
