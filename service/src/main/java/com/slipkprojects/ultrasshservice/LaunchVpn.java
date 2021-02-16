package com.slipkprojects.ultrasshservice;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.ultrasshservice.logger.ConnectionStatus;
import com.slipkprojects.ultrasshservice.tunnel.TunnelManagerHelper;
import android.os.Build;
import com.slipkprojects.ultrasshservice.config.Settings;
import android.net.VpnService;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.widget.Toast;
import com.slipkprojects.ultrasshservice.tunnel.TunnelUtils;
import android.widget.EditText;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.annotation.SuppressLint;
import android.widget.CheckBox;
import android.support.v4.widget.CompoundButtonCompat;
import android.widget.CompoundButton;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.os.RemoteException;
import com.slipkprojects.ultrasshservice.config.PasswordCache;
import android.content.SharedPreferences;
import android.widget.ImageButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

public class LaunchVpn extends AppCompatActivity
	implements DialogInterface.OnCancelListener
{
	public static final String EXTRA_HIDELOG = "com.slipkprojects.sockshttp.showNoLogWindow";
	public static final String CLEARLOG = "clearlogconnect";
	
	private static final int START_VPN_PROFILE = 70;
	
	private Settings mConfig;
	private String mTransientAuthPW;
	private boolean mhideLog = false;
	private boolean isMostrarSenha = false;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.launchvpn);
		
		mConfig = new Settings(this);

        startVpnFromIntent();
		//throw new RuntimeException();
    }
	
	protected void startVpnFromIntent() {
        // Resolve the intent

        final Intent intent = getIntent();
        final String action = intent.getAction();
		
		// If the intent is a request to create a shortcut, we'll do that and exit


        if (Intent.ACTION_MAIN.equals(action)) {
            // Check if we need to clear the log
            if (mConfig.getAutoClearLog())
				SkStatus.clearLog();

            mhideLog = intent.getBooleanExtra(EXTRA_HIDELOG, false);
			
            launchVPN();
        }
    }
	
	private void askForPW(final int type) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.title_auth);
        dialog.setMessage(R.string.auth_message);
		dialog.setOnCancelListener(this);

        @SuppressLint("InflateParams")
		final View userpwlayout = getLayoutInflater()
			.inflate(R.layout.userpass, null, false);

        ((EditText) userpwlayout.findViewById(R.id.username)).setText(mConfig.getPrivString(Settings.USUARIO_KEY));
        ((EditText) userpwlayout.findViewById(R.id.password)).setText(mConfig.getPrivString(Settings.SENHA_KEY));
        ((CheckBox) userpwlayout.findViewById(R.id.save_password)).setChecked(true);
        ((ImageButton) userpwlayout.findViewById(R.id.show_password)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isMostrarSenha = !isMostrarSenha;
				if (isMostrarSenha) {
					((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					((ImageButton) userpwlayout.findViewById(R.id.show_password)).setImageDrawable(ContextCompat.getDrawable(LaunchVpn.this, R.drawable.ic_visibility_off_black_24dp));
				}
				else {
					((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					((ImageButton) userpwlayout.findViewById(R.id.show_password)).setImageDrawable(ContextCompat.getDrawable(LaunchVpn.this, R.drawable.ic_visibility_black_24dp));
				}
			}
		});
			
        dialog.setView(userpwlayout);

        dialog.setPositiveButton(android.R.string.ok,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					if (type == R.string.password) {
						SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();
						
						String mUsername = ((EditText) userpwlayout.findViewById(R.id.username)).getText().toString();
						
						edit.putString(Settings.USUARIO_KEY, mUsername);
						
						String pw = ((EditText) userpwlayout.findViewById(R.id.password)).getText().toString();
						if (((CheckBox) userpwlayout.findViewById(R.id.save_password)).isChecked()) {
							edit.putString(Settings.SENHA_KEY, pw);
						} else {
							edit.remove(Settings.SENHA_KEY);
							mTransientAuthPW = pw;
						}
						
						edit.apply();
					}
					
					if (mTransientAuthPW != null)
						PasswordCache.setCachedPassword(null, PasswordCache.AUTHPASSWORD, mTransientAuthPW);
					onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
				}

			});
        dialog.setNegativeButton(android.R.string.cancel,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SkStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
						ConnectionStatus.LEVEL_NOTCONNECTED);
					finish();
				}
			});

        dialog.create().show();
	}

	@Override
	public void onCancel(DialogInterface p1)
	{
		SkStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
			ConnectionStatus.LEVEL_NOTCONNECTED);
		finish();
	}
	
	private void showLogWindow() {
        Intent updateView = new Intent("com.slipkprojects.sockshttp:openLogs");
		LocalBroadcastManager.getInstance(this)
			.sendBroadcast(updateView);
    }
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {
				SharedPreferences prefs = mConfig.getPrefsPrivate();
				
				if (!TunnelUtils.isNetworkOnline(this)) {
					SkStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
						ConnectionStatus.LEVEL_NOTCONNECTED);
					
					Toast.makeText(this, R.string.error_internet_off,
						Toast.LENGTH_SHORT).show();

					finish();
				}
				else if (prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT) == Settings.bTUNNEL_TYPE_SSH_PROXY &&
						(mConfig.getPrivString(Settings.PROXY_IP_KEY).isEmpty() || mConfig.getPrivString(Settings.PROXY_PORTA_KEY).isEmpty())) {
					SkStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
						ConnectionStatus.LEVEL_NOTCONNECTED);

					Toast.makeText(this, R.string.error_proxy_invalid,
						Toast.LENGTH_SHORT).show();

					finish();
				}
				else if (!prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true) && mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY).isEmpty()) {
					SkStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
						ConnectionStatus.LEVEL_NOTCONNECTED);
					
					Toast.makeText(this, R.string.error_empty_payload,
						Toast.LENGTH_SHORT).show();

					finish();
				}
				else if (mConfig.getPrivString(Settings.SERVIDOR_KEY).isEmpty() || mConfig.getPrivString(Settings.SERVIDOR_PORTA_KEY).isEmpty()) {
					SkStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
						ConnectionStatus.LEVEL_NOTCONNECTED);
					
					Toast.makeText(this, R.string.error_empty_settings,
						Toast.LENGTH_SHORT).show();

					Intent startLW = new Intent();
        			startLW.setComponent(new ComponentName(this, getPackageName() + ".activities.ConfigGeralActivity"));
        			startLW.setAction("openSSHScreen");
					startLW.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(startLW);

					finish();
				}
            	else if (mConfig.getPrivString(Settings.USUARIO_KEY).isEmpty() || (mConfig.getPrivString(Settings.SENHA_KEY).isEmpty() &&
						(mTransientAuthPW == null || mTransientAuthPW.isEmpty()))) {
                    SkStatus.updateStateString("USER_VPN_PASSWORD", "", R.string.state_user_vpn_password,
						ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
                    
					askForPW(R.string.password);
                }
				else {
                    if (!mhideLog) {
						showLogWindow();
					}
					
                    TunnelManagerHelper.startSocksHttp(this);
                    
					finish();
                }
				
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User does not want us to start, so we just vanish
                SkStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled,
					ConnectionStatus.LEVEL_NOTCONNECTED);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    SkStatus.logError(R.string.nought_alwayson_warning);

                finish();
            }
        }
    }
	
	private void launchVPN() {
		Intent intent = VpnService.prepare(this);
        	
        if (intent != null) {
            SkStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
				ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                SkStatus.logError(R.string.no_vpn_support_image);
                showLogWindow();
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }
	
}
