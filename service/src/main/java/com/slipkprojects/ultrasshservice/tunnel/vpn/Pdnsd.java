package com.slipkprojects.ultrasshservice.tunnel.vpn;

import java.io.File;
import android.util.Log;
import java.io.BufferedReader;
import android.content.Context;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;

import com.slipkprojects.ultrasshservice.R;
import com.slipkprojects.ultrasshservice.util.StreamGobbler;
import com.slipkprojects.ultrasshservice.util.FileUtils;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.ultrasshservice.util.CustomNativeLoader;

public class Pdnsd extends Thread {
	private final static String TAG = "PdnsdThread";
	private final static String PDNSD_SERVER = "server {\n label= \"%1$s\";\n ip = %2$s;\n port = %3$d;\n uptest = none;\n }\n";
	private final static String PDNSD_SERVER_TEST = "server {\n label= \"%1$s\";\n ip = %2$s;\n port = %3$d;\n reject = ::/0;\n reject_policy = negate;\n reject_recursively = on;\n timeout = 5;\n }\n";
	private final static String PDNSD_BIN = "pdnsd";
	
	private OnPdnsdListener mListener;
	public interface OnPdnsdListener {
		public void onStart();
		public void onStop();
	}
	
	private Process mProcess;
	private File filePdnsd;
	
	private Context mContext;
	private String[] mDnsHosts;
	private int mDnsPort;
	private String mPdnsdHost;
	private int mPdnsdPort;
	
	public Pdnsd(Context context, String[] dnsHosts, int dnsPort, String pdnsdHost, int pdnsdPort) {
		mContext = context;
		
		mDnsHosts = dnsHosts;
		mDnsPort = dnsPort;
		mPdnsdHost = pdnsdHost;
		mPdnsdPort = pdnsdPort;
	}

	@Override
	public void run() {
		
		if (mListener != null) {
			mListener.onStart();
		}
		
		try {
			
			//File filePdnsd = CustomNativeLoader.loadExecutableBinary(mContext, "libpdnsd.so");
			filePdnsd = CustomNativeLoader.loadNativeBinary(mContext, PDNSD_BIN, new File(mContext.getFilesDir(), PDNSD_BIN));
			
			if (filePdnsd == null) {
				throw new IOException("Bin Pdnsd não encontrada");
			}
			
			File fileConf = makePdnsdConf(mContext.getFilesDir(), mDnsHosts, mDnsPort, mPdnsdHost, mPdnsdPort);
			
			String cmdString = filePdnsd.getCanonicalPath() + " -v9 -c " + fileConf.toString();
			
			mProcess = Runtime.getRuntime().exec(cmdString);
			
			StreamGobbler.OnLineListener onLineListener = new StreamGobbler.OnLineListener(){
				@Override
				public void onLine(String log){
					SkStatus.logDebug("Pdnsd: " + log);
				}
			};

			StreamGobbler stdoutGobbler = new StreamGobbler(mProcess.getInputStream(), onLineListener);
			StreamGobbler stderrGobbler = new StreamGobbler(mProcess.getErrorStream(), onLineListener);

			stdoutGobbler.start();
			stderrGobbler.start();

			mProcess.waitFor();

		} catch (IOException e) {
			SkStatus.logException("Pdnsd Error", e);
		} catch(Exception e){
			SkStatus.logDebug("Pdnsd Error: " + e);
		}
		
		mProcess = null;
		if (mListener != null) {
			mListener.onStop();
		}

	}

	@Override
	public synchronized void interrupt()
	{
		// TODO: Implement this method
		super.interrupt();
		
		if (mProcess != null)
			mProcess.destroy();
			
		try {
			if (filePdnsd != null)
				VpnUtils.killProcess(filePdnsd);
		} catch(Exception e) {}
		
		mProcess = null;
		filePdnsd = null;
	}

    private File makePdnsdConf(File fileDir, String[] dnsHosts, int dnsPort, String pdnsdHost, int pdnsdPort) throws FileNotFoundException, IOException {
        String content = FileUtils.readFromRaw(mContext, R.raw.pdnsd_local);
		
		// dns servers
		StringBuilder server_dns = new StringBuilder();
		for (int i = 0; i < dnsHosts.length; i++){
			String dnsHost = dnsHosts[i];
			server_dns.append(String.format(PDNSD_SERVER, "server" + Integer.toString(i+1), dnsHost, dnsPort));
			//server_dns.append(String.format(PDNSD_SERVER_TEST, "server" + Integer.toString(i+1), "127.0.0.1", 8865));
		}
		
		String conf = String.format(content, server_dns.toString(), fileDir.getCanonicalPath(), pdnsdHost, pdnsdPort);
		
        Log.d(TAG,"pdnsd conf:" + conf);

        File f = new File(fileDir,"pdnsd.conf");
        if (f.exists()) {
			f.delete();
		}
		FileUtils.saveTextFile(f, conf);
		
        File cache = new File(fileDir,"pdnsd.cache");
        if (!cache.exists()) {
        	try {
            	cache.createNewFile();
            } catch (Exception e) {}
        }

        return f;
	}
	
	public void setOnPdnsdListener(OnPdnsdListener listener){
		this.mListener = listener;
	}
}
