
package com.trilead.ssh2;

import java.io.IOException;
import java.net.Socket;

/**
 * An abstract interface implemented by all proxy data implementations.
 *
 * @see HTTPProxyData
 *
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: ProxyData.java,v 1.1 2007/10/15 12:49:56 cplattne Exp $
 */

public interface ProxyData
{
	/**
	 * Connects the socket to the given destination using the proxy method that this instance
	 * represents.
	*/
	Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException;
	
	/**
	* Fecha conexão do proxy
	*/
	void close();
}
