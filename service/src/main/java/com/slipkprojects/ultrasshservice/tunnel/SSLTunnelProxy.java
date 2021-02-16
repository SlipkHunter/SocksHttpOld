package com.slipkprojects.ultrasshservice.tunnel;

import com.trilead.ssh2.ProxyData;
import java.net.Socket;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLSocket;
import java.security.SecureRandom;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HandshakeCompletedEvent;
import java.util.Arrays;

public class SSLTunnelProxy implements ProxyData
{
	class HandshakeTunnelCompletedListener implements HandshakeCompletedListener {
        private final String val$host;
        private final int val$port;
        private final SSLSocket val$sslSocket;

        HandshakeTunnelCompletedListener( String str, int i, SSLSocket sSLSocket) {
            this.val$host = str;
            this.val$port = i;
            this.val$sslSocket = sSLSocket;
        }

        public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
			// addLog(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection with ").append(val$host).append(":").append(this.val$port).append(" using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
			//addLog(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection ").append("using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
		    //addLog(new StringBuffer().append("Supported cipher suites: ").append(Arrays.toString(this.val$sslSocket.getSupportedCipherSuites())).toString());
            //addLog(new StringBuffer().append("Enabled cipher suites: ").append(Arrays.toString(this.val$sslSocket.getEnabledCipherSuites())).toString());
            SkStatus.logInfo(new StringBuffer().append("SSL: Supported protocols: <br>").append(Arrays.toString(val$sslSocket.getSupportedProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			SkStatus.logInfo(new StringBuffer().append("SSL: Enabled protocols: <br>").append(Arrays.toString(val$sslSocket.getEnabledProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			SkStatus.logInfo("SSL: Using cipher " + handshakeCompletedEvent.getSession().getCipherSuite());
			SkStatus.logInfo("SSL: Using protocol " + handshakeCompletedEvent.getSession().getProtocol());
			SkStatus.logInfo("SSL: Handshake finished");
        }
    }
	
	private String stunnelServer;
	private int stunnelPort = 443;
	private String stunnelHostSNI;
	
	private Socket mSocket;
	
	public SSLTunnelProxy(String server, int port, String hostSni) {
		this.stunnelServer = server;
		this.stunnelPort = port;
		this.stunnelHostSNI = hostSni;
	}

	@Override
	public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException
	{
		mSocket = SocketChannel.open().socket();
		mSocket.connect(new InetSocketAddress(stunnelServer, stunnelPort));
		
		if (mSocket.isConnected()) {
			mSocket = doSSLHandshake(hostname, stunnelHostSNI, port);
		}
		
		return mSocket;
	}
	
	private Socket doSSLHandshake(Socket socket, String host, String sni, int port) throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
				}
			}
		};
        try {
            SSLContext sSLContext = SSLContext.getInstance("SSL");
            KeyManager[] keyManagerArr = null;
            sSLContext.init(keyManagerArr, trustAllCerts, new SecureRandom());
            SSLSocket socket3 = (SSLSocket) sSLContext.getSocketFactory().createSocket(socket, host, port, true);
            if (sSLContext.getSocketFactory() instanceof android.net.SSLCertificateSocketFactory && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
				((android.net.SSLCertificateSocketFactory)sSLContext.getSocketFactory()).setHostname(socket, sni);
			} else {
				try {
					socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);
					SkStatus.logInfo("Setting up SNI: "+sni);
				} catch (Throwable e) {
					// ignore any error, we just can't set the hostname...
				}
			}
			socket3.setEnabledProtocols(socket3.getSupportedProtocols());
            socket3.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, socket3));
            SkStatus.logInfo("Starting SSL Handshake...");
			socket3.startHandshake();
            return socket3;
        } catch (Exception e) {
            IOException iOException = new IOException(new StringBuffer().append("Could not do SSL handshake: ").append(e).toString());
            throw iOException;
        }
    }

	private SSLSocket doSSLHandshake(String host, String sni, int port) throws IOException {
        try {
			TLSSocketFactory tsf = new TLSSocketFactory();
            SSLSocket socket = (SSLSocket) tsf.createSocket(host, port);
			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);
				SkStatus.logInfo("Setting up SNI: "+sni);
			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}

			//socket.setEnabledProtocols(socket.getSupportedProtocols());
            socket.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, socket));
            SkStatus.logInfo("Starting SSL Handshake...");
			socket.startHandshake();
			return socket;
        } catch (Exception e) {
            IOException iOException = new IOException(new StringBuffer().append("Could not do SSL handshake: ").append(e).toString());
            throw iOException;
        }
    }

	@Override
	public void close()
	{
		try {
			if (mSocket != null) {
				mSocket.close();
			}
		} catch(IOException e) {}
	}

}
