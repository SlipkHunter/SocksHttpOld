package com.slipkprojects.ultrasshservice.tunnel.vpn;

// Singleton class to maintain state related to VPN Tunnel service.
public class TunnelState {

  private static TunnelState m_tunnelState;

  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public static synchronized TunnelState getTunnelState() {
    if (m_tunnelState == null) {
      m_tunnelState = new TunnelState();
    }
    return m_tunnelState;
  }

  private TunnelVpnManager m_tunnelManager = null;
  private boolean m_startingTunnelManager = false;

  private TunnelState() {}

  public synchronized void setTunnelManager(TunnelVpnManager tunnelManager) {
    m_tunnelManager = tunnelManager;
    m_startingTunnelManager = false;
  }

  public synchronized TunnelVpnManager getTunnelManager() {
    return m_tunnelManager;
  }

  public synchronized void setStartingTunnelManager() {
    m_startingTunnelManager = true;
  }

  public synchronized boolean getStartingTunnelManager() {
    return m_startingTunnelManager;
  }
};
