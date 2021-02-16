package com.slipkprojects.ultrasshservice.config;

import android.content.Context;
import java.io.File;
import java.util.Properties;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import android.util.Log;
import android.content.pm.PackageInfo;
import java.util.Calendar;
import android.widget.Toast;
import java.util.Date;
import java.text.ParseException;
import java.text.DateFormat;
import android.content.pm.PackageManager;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.ultrasshservice.util.FileUtils;
import java.io.InputStream;
import com.slipkprojects.ultrasshservice.R;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.os.Build;
import com.kimchangyoun.rootbeerFresh.RootBeer;
import com.slipkprojects.ultrasshservice.util.securepreferences.crypto.Cryptor;
import com.slipkprojects.ultrasshservice.util.securepreferences.model.SecurityConfig;
import java.util.ArrayList;
import com.slipkprojects.ultrasshservice.util.Cripto;

/**
* @author SlipkHunter
*/
public class ConfigParser
{
	private static final String TAG = ConfigParser.class.getSimpleName();
	public static final String CONVERTED_PROFILE = "converted Profile";
	
	public static final String FILE_EXTENSAO = "sks";
	private static final String KEY_PASSWORD = "cinbdf665$4";
	
	private static final String
		SETTING_VERSION = "file.appVersionCode",
		SETTING_VALIDADE = "file.validade",
		SETTING_PROTEGER = "file.proteger",
		SETTING_AUTOR_MSG = "file.msg";
	
	
	public static boolean convertInputAndSave(InputStream input, Context mContext)
			throws IOException {
		Properties mConfigFile = new Properties();
		
		Settings settings = new Settings(mContext);
		SharedPreferences.Editor prefsEdit = settings.getPrefsPrivate()
			.edit();
		
		try {
			
			InputStream decodedInput = decodeInput(input);
			
			try {
				mConfigFile.loadFromXML(decodedInput);
			} catch(FileNotFoundException e) {
				throw new IOException("File Not Found");
			} catch(IOException e) {
				throw new Exception("Error Unknown", e);
			}

			// versão check
			int versionCode = Integer.parseInt(mConfigFile.getProperty(SETTING_VERSION));

			if (versionCode > getBuildId(mContext)) {
				throw new IOException(mContext.getString(R.string.alert_update_app));
			}

			// validade check
			String msg = mConfigFile.getProperty(SETTING_AUTOR_MSG);
			boolean mIsProteger = mConfigFile.getProperty(SETTING_PROTEGER).equals("1") ? true : false;
			long mValidade = 0;
			
			try {
				mValidade = Long.parseLong(mConfigFile.getProperty(SETTING_VALIDADE));
			} catch(Exception e) {
				throw new IOException(mContext.getString(R.string.alert_update_app));
			}

			if (!mIsProteger || mValidade < 0) {
				mValidade = 0;
			}
			else if (mValidade > 0 && isValidadeExpirou(mValidade)){
				throw new IOException(mContext.getString(R.string.error_settings_expired));
			}
			
			// bloqueia root
			boolean isBloquearRoot = false;
			String _blockRoot = mConfigFile.getProperty("bloquearRoot");
			if (_blockRoot != null) {
				isBloquearRoot = _blockRoot.equals("1") ? true : false;
				if (isBloquearRoot) {
					if (isDeviceRooted(mContext)) {
						throw new IOException(mContext.getString(R.string.error_root_detected));
					}
				} 
			}
			

			try {
				String mServidor = mConfigFile.getProperty(Settings.SERVIDOR_KEY);
				String mServidorPorta = mConfigFile.getProperty(Settings.SERVIDOR_PORTA_KEY);
				String mUsuario = mConfigFile.getProperty(Settings.USUARIO_KEY);
				String mSenha = mConfigFile.getProperty(Settings.SENHA_KEY);
				int mPortaLocal = Integer.parseInt(mConfigFile.getProperty(Settings.PORTA_LOCAL_KEY));
				int mTunnelType = Settings.bTUNNEL_TYPE_SSH_DIRECT;
				
				String _tunnelType = mConfigFile.getProperty(Settings.TUNNELTYPE_KEY);
				if (!_tunnelType.isEmpty()) {
					/**
					* Mantêm compatibilidade
					*/
					if (_tunnelType.equals(Settings.TUNNEL_TYPE_SSH_PROXY)) {
						mTunnelType = Settings.bTUNNEL_TYPE_SSH_PROXY;
					}
					else if (!_tunnelType.equals(Settings.TUNNEL_TYPE_SSH_DIRECT)) {
						mTunnelType = Integer.parseInt(_tunnelType);
					}
				}
				
				if (mServidor == null) {
					throw new Exception();
				}

				String _proxyIp = mConfigFile.getProperty(Settings.PROXY_IP_KEY);
				String _proxyPort = mConfigFile.getProperty(Settings.PROXY_PORTA_KEY);
				prefsEdit.putString(Settings.PROXY_IP_KEY, _proxyIp != null ? _proxyIp : "");
				prefsEdit.putString(Settings.PROXY_PORTA_KEY, _proxyPort != null ? _proxyPort : "");

				prefsEdit.putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, !mConfigFile.getProperty(Settings.PROXY_USAR_DEFAULT_PAYLOAD).equals("1") ? false : true);
				
				String _customPayload = mConfigFile.getProperty(Settings.CUSTOM_PAYLOAD_KEY);
				prefsEdit.putString(Settings.CUSTOM_PAYLOAD_KEY, _customPayload != null ? _customPayload : "");
				
				if (mIsProteger) {
					prefsEdit.putString(Settings.CONFIG_MENSAGEM_KEY, msg != null ? msg : "");
					
					new Settings(mContext)
						.setModoDebug(false);

					String pedirLogin = mConfigFile.getProperty("file.pedirLogin");
					if (pedirLogin != null)
						prefsEdit.putBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, pedirLogin.equals("1") ? true : false);
					else
						prefsEdit.putBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false);
				}
				else {
					prefsEdit.putString(Settings.CONFIG_MENSAGEM_KEY, "");
					prefsEdit.putBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false);
				}
				
				prefsEdit.putString(Settings.SERVIDOR_KEY, mServidor);
				prefsEdit.putString(Settings.SERVIDOR_PORTA_KEY, mServidorPorta);
				prefsEdit.putString(Settings.USUARIO_KEY, mUsuario);
				prefsEdit.putString(Settings.SENHA_KEY, mSenha);
				prefsEdit.putString(Settings.PORTA_LOCAL_KEY, Integer.toString(mPortaLocal));

				prefsEdit.putInt(Settings.TUNNELTYPE_KEY, mTunnelType);
				prefsEdit.putBoolean(Settings.CONFIG_PROTEGER_KEY, mIsProteger);
				prefsEdit.putLong(Settings.CONFIG_VALIDADE_KEY, mValidade);
				prefsEdit.putBoolean(Settings.BLOQUEAR_ROOT_KEY, isBloquearRoot);
				
				String _isDnsForward = mConfigFile.getProperty(Settings.DNSFORWARD_KEY);
				boolean isDnsForward = _isDnsForward != null && _isDnsForward.equals("0") ? false : true;
				String dnsResolver = mConfigFile.getProperty(Settings.DNSRESOLVER_KEY);
				settings.setVpnDnsForward(isDnsForward);
				settings.setVpnDnsResolver(dnsResolver);
				
				String _isUdpForward = mConfigFile.getProperty(Settings.UDPFORWARD_KEY);
				boolean isUdpForward = _isUdpForward != null && _isUdpForward.equals("1") ? true : false;
				String udpResolver = mConfigFile.getProperty(Settings.UDPRESOLVER_KEY);
				settings.setVpnUdpForward(isUdpForward);
				settings.setVpnUdpResolver(udpResolver);
				
			} catch(Exception e) {
				if (settings.getModoDebug()) {
					SkStatus.logException("Error Settings", e);
				}
				throw new IOException(mContext.getString(R.string.error_file_settings_invalid));
			}
			
			return prefsEdit.commit();
		
		} catch(IOException e) {
			throw e;
		} catch(Exception e) {
			throw new IOException(mContext.getString(R.string.error_file_invalid), e);
		} catch (Throwable e) {
			throw new IOException(mContext.getString(R.string.error_file_invalid));
		}
	}
	
	public static void convertDataToFile(OutputStream fileOut, Context mContext,
			boolean mIsProteger, boolean mPedirSenha, boolean isBloquearRoot, String mMensagem, long mValidade)
				throws IOException {
		
		Properties mConfigFile = new Properties();
		ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
		
		Settings settings = new Settings(mContext);
		SharedPreferences prefs = settings.getPrefsPrivate();
		
		try {
			int targerId = getBuildId(mContext);
			// para versões betas
			targerId = 24;
			
			mConfigFile.setProperty(SETTING_VERSION, Integer.toString(targerId));

			mConfigFile.setProperty(SETTING_AUTOR_MSG, mMensagem);
			mConfigFile.setProperty(SETTING_PROTEGER, mIsProteger ? "1" : "0");
			mConfigFile.setProperty("bloquearRoot", isBloquearRoot ? "1" : "0");
						
			mConfigFile.setProperty(SETTING_VALIDADE, Long.toString(mValidade));
			mConfigFile.setProperty("file.pedirLogin", mPedirSenha ? "1" : "0");

			String server = prefs.getString(Settings.SERVIDOR_KEY, "");
			String server_port = prefs.getString(Settings.SERVIDOR_PORTA_KEY, "");
			
			if (mIsProteger && (server.isEmpty() || server_port.isEmpty())) {
				throw new Exception();
			}
						
			mConfigFile.setProperty(Settings.SERVIDOR_KEY, server);
			mConfigFile.setProperty(Settings.SERVIDOR_PORTA_KEY, server_port);
			mConfigFile.setProperty(Settings.USUARIO_KEY, prefs.getString(Settings.USUARIO_KEY, ""));
			mConfigFile.setProperty(Settings.SENHA_KEY, prefs.getString(Settings.SENHA_KEY, ""));
			mConfigFile.setProperty(Settings.PORTA_LOCAL_KEY, prefs.getString(Settings.PORTA_LOCAL_KEY, "1080"));

			mConfigFile.setProperty(Settings.TUNNELTYPE_KEY, Integer.toString(prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT)));
			
			mConfigFile.setProperty(Settings.DNSFORWARD_KEY, settings.getVpnDnsForward() ? "1" : "0");
			mConfigFile.setProperty(Settings.DNSRESOLVER_KEY, settings.getVpnDnsResolver());
			
			mConfigFile.setProperty(Settings.UDPFORWARD_KEY, settings.getVpnUdpForward() ? "1" : "0");
			mConfigFile.setProperty(Settings.UDPRESOLVER_KEY, settings.getVpnUdpResolver());
			
			mConfigFile.setProperty(Settings.PROXY_IP_KEY, prefs.getString(Settings.PROXY_IP_KEY, ""));
			mConfigFile.setProperty(Settings.PROXY_PORTA_KEY, prefs.getString(Settings.PROXY_PORTA_KEY, ""));

			String isDefaultPayload = prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true) ? "1" : "0";
			String customPayload = prefs.getString(Settings.CUSTOM_PAYLOAD_KEY, "");
						
			if (mIsProteger && isDefaultPayload.equals("0") && customPayload.isEmpty()) {
				throw new IOException();
			}
			
			mConfigFile.setProperty(Settings.PROXY_USAR_DEFAULT_PAYLOAD, isDefaultPayload);
			mConfigFile.setProperty(Settings.CUSTOM_PAYLOAD_KEY, customPayload);

		} catch(Exception e) {
			throw new IOException(mContext.getString(R.string.error_file_settings_invalid));
		}

		try {
			mConfigFile.storeToXML(tempOut,
				"Arquivo de Configuração");
		} catch(FileNotFoundException e) {
			throw new IOException("File Not Found");
		} catch(IOException e) {
			throw new IOException("Error Unknown", e);
		}
		
		try {
			InputStream input_encoded = encodeInput(
				new ByteArrayInputStream(tempOut.toByteArray()));
			
			FileUtils.copiarArquivo(input_encoded, fileOut);
		} catch(Throwable e) {
			throw new IOException(mContext.getString(R.string.error_save_settings));
		}
	}
	
	
	/**
	* Criptografia
	*/
	
	private static Cryptor mCrypto;
	
	static {
		mCrypto = Cryptor.initWithSecurityConfig(
			new SecurityConfig.Builder("fubvx788b46v").build());
	}
	
	private static InputStream encodeInput(InputStream in) throws Throwable {
		//ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		String strBase64 = mCrypto.encryptToBase64(getBytesArrayInputStream(in)
			.toByteArray());
		
		//Cripto.encrypt(KEY_PASSWORD, in, out);
		
		return new ByteArrayInputStream(strBase64.getBytes());
	}
	
	private static InputStream decodeInput(InputStream in) throws Throwable {
		byte[] byteDecript;
		
		ByteArrayOutputStream byteArrayOut = getBytesArrayInputStream(in);
		
		try {
			byteDecript = mCrypto.decryptFromBase64(byteArrayOut.toString());
		} catch (IllegalArgumentException e) {
			// decodifica confg antigas
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Cripto.decrypt(KEY_PASSWORD, new ByteArrayInputStream(byteArrayOut.toByteArray()), out);
			byteDecript = out.toByteArray();
		}
		
		return new ByteArrayInputStream(byteDecript);
	}
	
	public static ByteArrayOutputStream getBytesArrayInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();
		
		return buffer;
	}

	
	/**
	* Utils
	*/
	
	public static boolean isValidadeExpirou(long validadeDateMillis) {
		if (validadeDateMillis == 0) {
			return false;
		}
		
		// Get Current Date
		long date_atual = Calendar.getInstance()
			.getTime().getTime();
		
		if (date_atual >= validadeDateMillis) {
			return true;
		}
		
		return false;
	}
	
	public static int getBuildId(Context context) throws IOException {
		try {
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			throw new IOException("Build ID not found");
		}
	}
	
	public static boolean isDeviceRooted(Context context) {
        /*for (String pathDir : System.getenv("PATH").split(":")){
			if (new File(pathDir, "su").exists()) {
				return true;
			}
		}
		
		Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }*/
		
		RootBeer rootBeer = new RootBeer(context);
		
		boolean simpleTests = rootBeer.detectRootManagementApps() || rootBeer.detectPotentiallyDangerousApps() || rootBeer.checkForBinary("su")
			|| rootBeer.checkForDangerousProps() || rootBeer.checkForRWPaths()
			|| rootBeer.detectTestKeys() || rootBeer.checkSuExists() || rootBeer.checkForRootNative() || rootBeer.checkForMagiskBinary();
		//boolean experiementalTests = rootBeer.checkForMagiskNative();
			
		return simpleTests;
	}

}
