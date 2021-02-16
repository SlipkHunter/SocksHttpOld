package com.slipkprojects.ultrasshservice.logger;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Vector;
import android.os.Build;
import com.slipkprojects.ultrasshservice.R;
import android.content.Intent;
import android.content.Context;
import android.os.Message;
import java.io.File;
import android.os.HandlerThread;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.util.Iterator;
import java.util.Locale;

public class SkStatus
{
	private static final LinkedList<LogItem> logbuffer;
	
	private static Vector<LogListener> logListener;
    private static Vector<StateListener> stateListener;
	
	private static ConnectionStatus mLastLevel = ConnectionStatus.LEVEL_NOTCONNECTED;
	
	private static String mLaststatemsg = "";
    private static String mLaststate = "NOPROCESS";
    private static int mLastStateresid = R.string.state_noprocess;
    private static Intent mLastIntent = null;
	
	
	static final int MAXLOGENTRIES = 1000;

    public static boolean isTunnelActive() {
        return mLastLevel != ConnectionStatus.LEVEL_AUTH_FAILED && !(mLastLevel == ConnectionStatus.LEVEL_NOTCONNECTED);
    }
	
	public static String getLastState() {
		return mLaststate;
	}
	
	public static String getLastCleanLogMessage(Context c) {
        String message = mLaststatemsg;
        switch (mLastLevel) {
            case LEVEL_CONNECTED:
                String[] parts = mLaststatemsg.split(",");
                /*
				 (a) the integer unix date/time,
				 (b) the state name,
				 0 (c) optional descriptive string (used mostly on RECONNECTING
				 and EXITING to show the reason for the disconnect),

				 1 (d) optional TUN/TAP local IPv4 address
				 2 (e) optional address of remote server,
				 3 (f) optional port of remote server,
				 4 (g) optional local address,
				 5 (h) optional local port, and
				 6 (i) optional TUN/TAP local IPv6 address.
				 */
                // Return only the assigned IP addresses in the UI
                if (parts.length >= 7)
                    message = String.format(Locale.US, "%s %s", parts[1], parts[6]);
                break;
        }

        while (message.endsWith(","))
            message = message.substring(0, message.length() - 1);

        String status = mLaststate;
        if (status.equals("NOPROCESS"))
            return message;

        if (mLastStateresid == R.string.state_waitconnectretry) {
            return c.getString(R.string.state_waitconnectretry, mLaststatemsg);
        }

        String prefix = c.getString(mLastStateresid);
        if (mLastStateresid == R.string.unknown_state)
            message = status + message;
        if (message.length() > 0)
            prefix += ": ";

        return prefix + message;

    }
	
	
	public static enum LogLevel {
		
        INFO(2),
        ERROR(-2),
        WARNING(1),
        VERBOSE(3),
        DEBUG(4);

        protected int mValue;

        LogLevel(int value) {
            mValue = value;
        }

        public int getInt() {
            return mValue;
        }

        public static LogLevel getEnumByValue(int value) {
            switch (value) {
                case 2:
                    return INFO;
                case -2:
                    return ERROR;
                case 1:
                    return WARNING;
                case 3:
                    return VERBOSE;
                case 4:
                    return DEBUG;

                default:
                    return null;
            }
        }
    }
	
	// keytool -printcert -jarfile de.blinkt.openvpn_85.apk
	// tudo ok, certificado da Playstore
	static final byte[] oficialkey = {93, -72, 88, 103, -128, 115, -1, -47, 120, 113, 98, -56, 12, -56, 52, -62, 95, -2, -114, 95};
    // já atualizado, slipk certificado
	static final byte[] oficialdebugkey = {-41, 73, 58, 102, -81, -27, -120, 45, -56, -3, 53, -49, 119, -97, -20, -80, 65, 68, -72, -22};
	
	static {
        logbuffer = new LinkedList<>();
        logListener = new Vector<>();
        stateListener = new Vector<>();
		
		logInformation();
    }
	

    public synchronized static void clearLog() {
        logbuffer.clear();
		logInformation();
		
		for (LogListener li : logListener) {
			li.onClear();
		}
    }
	
	public synchronized static LogItem[] getlogbuffer() {

        // The stoned way of java to return an array from a vector
        // brought to you by eclipse auto complete
        return logbuffer.toArray(new LogItem[logbuffer.size()]);
    }
	
	private static void logInformation() {
		logInfo(R.string.mobile_info, Build.MODEL, Build.BOARD, Build.BRAND, Build.VERSION.SDK_INT,
        	Build.VERSION.RELEASE);
		logInfo(R.string.app_mobile_info, "", "");
	}

	
	/**
	* Listeners
	*/
	
	public interface LogListener {
        void newLog(LogItem logItem);
		void onClear();
    }

    public interface StateListener {
        void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent);
    }
	
    public synchronized static void addLogListener(LogListener ll) {
        if (!logListener.contains(ll)) {
			logListener.add(ll);
		}
    }

    public synchronized static void removeLogListener(LogListener ll) {
        if (logListener.contains(ll)) {
			logListener.remove(ll);
		}
    }

    public synchronized static void addStateListener(StateListener sl) {
        if (!stateListener.contains(sl)) {
            stateListener.add(sl);
            if (mLaststate != null)
                sl.updateState(mLaststate, mLaststatemsg, mLastStateresid, mLastLevel, mLastIntent);
        }
    }
	
	public synchronized static void removeStateListener(StateListener sl) {
        if (stateListener.contains(sl)) {
			stateListener.remove(sl);
		}
    }

	
	/**
	* State
	*/
	
	public static final String
		SSH_CONECTANDO = "CONECTANDO",
		SSH_AGUARDANDO_REDE = "AGUARDANDO",
		SSH_AUTENTICANDO = "AUTENTICANDO",
		SSH_CONECTADO = "CONECTADO",
		SSH_DESCONECTADO = "DESCONECTADO",
		SSH_RECONECTANDO = "RECONECTANDO",
		SSH_INICIANDO = "INICIANDO",
		SSH_PARANDO = "PARANDO";
	
	public static int getLocalizedState(String state) {
        switch (state) {
			case SSH_CONECTANDO:
                return R.string.state_connecting;
            case SSH_AGUARDANDO_REDE:
                return R.string.state_nonetwork;
            case SSH_AUTENTICANDO:
                return R.string.state_auth;
            case "GET_CONFIG":
                return R.string.state_get_config;
            case "ASSIGN_IP":
                return R.string.state_assign_ip;
            case "ADD_ROUTES":
                return R.string.state_add_routes;
            case SSH_CONECTADO:
                return R.string.state_connected;
            case SSH_DESCONECTADO:
                return R.string.state_disconnected;
            case SSH_RECONECTANDO:
                return R.string.state_reconnecting;
			case SSH_INICIANDO:
				return R.string.state_starting;
            case SSH_PARANDO:
                return R.string.state_stopping;
            case "RESOLVE":
                return R.string.state_resolve;
            case "TCP_CONNECT":
                return R.string.state_tcp_connect;
            case "AUTH_PENDING":
                return R.string.state_auth_pending;
            default:
                return R.string.unknown_state;
        }

    }

	private static ConnectionStatus getLevel(String state) {
        String[] noreplyet = {SSH_INICIANDO, SSH_CONECTANDO, SSH_AGUARDANDO_REDE, SSH_RECONECTANDO, "RESOLVE", "TCP_CONNECT"};
        String[] reply = {SSH_AUTENTICANDO, "GET_CONFIG", "ASSIGN_IP", "ADD_ROUTES", "AUTH_PENDING"};
        String[] connected = {SSH_CONECTADO};
        String[] notconnected = {SSH_DESCONECTADO};

        for (String x : noreplyet)
            if (state.equals(x))
                return ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;

        for (String x : reply)
            if (state.equals(x))
                return ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;

        for (String x : connected)
            if (state.equals(x))
                return ConnectionStatus.LEVEL_CONNECTED;

        for (String x : notconnected)
            if (state.equals(x))
                return ConnectionStatus.LEVEL_NOTCONNECTED;

        return ConnectionStatus.UNKNOWN_LEVEL;
    }
	
    public static void updateStateString(String state, String msg) {
        int rid = getLocalizedState(state);
        ConnectionStatus level = getLevel(state);
        updateStateString(state, msg, rid, level);
    }

    public synchronized static void updateStateString(String state, String msg, int resid, ConnectionStatus level)
    {
        updateStateString(state, msg, resid, level, null);
    }

    public synchronized static void updateStateString(String state, String msg, int resid, ConnectionStatus level, Intent intent) {
        // Workound for OpenVPN doing AUTH and wait and being connected
        // Simply ignore these state
        if (mLastLevel == ConnectionStatus.LEVEL_CONNECTED &&
				(state.equals(SSH_AUTENTICANDO))) {
            newLogItem(new LogItem((LogLevel.DEBUG), String.format("Ignoring SocksHttp Status in CONNECTED state (%s->%s): %s", state, level.toString(), msg)));
            return;
        }

        mLaststate = state;
        mLaststatemsg = msg;
        mLastStateresid = resid;
        mLastLevel = level;
        mLastIntent = intent;


        for (StateListener sl : stateListener) {
            sl.updateState(state, msg, resid, level, intent);
        }
		
        //newLogItem(new LogItem((LogLevel.DEBUG), String.format("SocksHttp Novo Status (%s->%s): %s",state,level.toString(),msg)));
    }

    
	/**
	* NewLog
	*/
	
    static void newLogItem(LogItem logItem) {
        newLogItem(logItem, false);
    }

    synchronized static void newLogItem(LogItem logItem, boolean cachedLine) {
        if (cachedLine) {
            logbuffer.addFirst(logItem);
        } else {
            logbuffer.addLast(logItem);
        }

        if (logbuffer.size() > MAXLOGENTRIES + MAXLOGENTRIES / 2) {
            while (logbuffer.size() > MAXLOGENTRIES)
                logbuffer.removeFirst();
        }

        for (LogListener ll : logListener) {
            ll.newLog(logItem);
        }
    }

	
	/**
	* Logger static methods
	*/
	
	public static void logException(String context, Exception e) {
        logException(LogLevel.ERROR, context, e);
    }
	
	public static void logException(LogLevel ll, String context, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

		LogItem li;
		
		if (context != null)
			li = new LogItem(ll, String.format("%s: %s, %s", context, e.getMessage(), sw.toString()));
		else
			li = new LogItem(ll, String.format("Erro: %s, %s", e.getMessage(), sw.toString()));

        newLogItem(li);
    }

	public static void logException(Exception e) {
        logException(LogLevel.ERROR, null, e);
    }
	
	public static void logInfo(String message) {
        newLogItem(new LogItem(LogLevel.INFO, message));
    }

    public static void logDebug(String message) {
        newLogItem(new LogItem(LogLevel.DEBUG, message));
    }

    public static void logInfo(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.INFO, resourceId, args));
    }

    public static void logDebug(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.DEBUG, resourceId, args));
    }

    public static void logError(String msg) {
        newLogItem(new LogItem(LogLevel.ERROR, msg));
    }

    public static void logWarning(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.WARNING, resourceId, args));
    }

    public static void logWarning(String msg) {
        newLogItem(new LogItem(LogLevel.WARNING, msg));
    }

    public static void logError(int resourceId) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId));
    }

    public static void logError(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId, args));
    }

}
