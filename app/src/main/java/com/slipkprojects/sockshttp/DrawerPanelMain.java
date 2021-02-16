package com.slipkprojects.sockshttp;

import android.app.Activity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.NavigationView;
import android.content.pm.PackageInfo;
import com.slipkprojects.sockshttp.util.Utils;
import android.support.v7.app.AppCompatActivity;
import com.slipkprojects.sockshttp.R;
import android.view.View;
import android.widget.TextView;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.Build;
import android.content.Intent;
import android.net.Uri;
import com.slipkprojects.sockshttp.util.GoogleFeedbackUtils;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import android.support.v4.view.GravityCompat;
import com.slipkprojects.sockshttp.activities.ConfigGeralActivity;
import com.slipkprojects.sockshttp.activities.AboutActivity;

public class DrawerPanelMain
	implements NavigationView.OnNavigationItemSelectedListener
{
	private AppCompatActivity mActivity;
	
	public DrawerPanelMain(AppCompatActivity activity) {
		mActivity = activity;
	}
	

	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle toggle;

	public void setDrawer(Toolbar toolbar) {
		NavigationView drawerNavigationView = (NavigationView) mActivity.findViewById(R.id.drawerNavigationView);
		drawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawerLayoutMain);

		// set drawer
		toggle = new ActionBarDrawerToggle(mActivity,
			drawerLayout, toolbar, R.string.open, R.string.cancel);

        drawerLayout.setDrawerListener(toggle);

		toggle.syncState();

		// set app info
		PackageInfo pinfo = Utils.getAppInfo(mActivity);
		if (pinfo != null) {
			String version_nome = pinfo.versionName;
			int version_code = pinfo.versionCode;
			String header_text = String.format("v. %s (%d)", version_nome, version_code);

			View view = drawerNavigationView.getHeaderView(0);

			TextView app_info_text = view.findViewById(R.id.nav_headerAppVersion);
			app_info_text.setText(header_text);
		}

		// set navigation view
		drawerNavigationView.setNavigationItemSelectedListener(this);
	}
	
	public ActionBarDrawerToggle getToogle() {
		return toggle;
	}
	
	public DrawerLayout getDrawerLayout() {
		return drawerLayout;
	}
	
	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();

		switch(id)
		{
			case R.id.miPhoneConfg:
				PackageInfo app_info = Utils.getAppInfo(mActivity);
				if (app_info != null) {
					String aparelho_marca = Build.BRAND.toUpperCase();

					if (aparelho_marca.equals("SAMSUNG") || aparelho_marca.equals("HUAWEY")) {
						Toast.makeText(mActivity, R.string.error_no_supported, Toast.LENGTH_SHORT)
							.show();
					}
					else {
						try {
							Intent in = new Intent(Intent.ACTION_MAIN);
							in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							in.setClassName("com.android.settings", "com.android.settings.RadioInfo");
							mActivity.startActivity(in);
						} catch(Exception e) {
							Toast.makeText(mActivity, R.string.error_no_supported, Toast.LENGTH_SHORT)
								.show();
						}
					}
				}
				break;

			case R.id.miSettings:
				Intent intent = new Intent(mActivity, ConfigGeralActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mActivity.startActivity(intent);
				break;
				
			case R.id.miSettingsSSH:
				Intent intent2 = new Intent(mActivity, ConfigGeralActivity.class);
				intent2.setAction(ConfigGeralActivity.OPEN_SETTINGS_SSH);
				intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mActivity.startActivity(intent2);
				break;

			case R.id.miAvaliarPlaystore:
				String url = "https://play.google.com/store/apps/details?id=com.slipkprojects.sockshttp";
				Intent intent3 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				intent3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mActivity.startActivity(Intent.createChooser(intent3, mActivity.getText(R.string.open_with)));
				break;

			case R.id.miSendFeedback:
				if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					try {
						GoogleFeedbackUtils.bindFeedback(mActivity);
					} catch (Exception e) {
						Toast.makeText(mActivity, "Não disponível em seu aparelho", Toast.LENGTH_SHORT)
							.show();
						SkStatus.logDebug("Error: " + e.getMessage());
					}
				}
				else {
					Intent email = new Intent(Intent.ACTION_SEND);  
					email.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					email.putExtra(Intent.EXTRA_EMAIL, new String[]{"slipkprojects@gmail.com"});  
					email.putExtra(Intent.EXTRA_SUBJECT, "SocksHttp - " + mActivity.getString(R.string.feedback));  
					//email.putExtra(Intent.EXTRA_TEXT, "");  

					//need this to prompts email client only  
					email.setType("message/rfc822");  

					mActivity.startActivity(Intent.createChooser(email, "Choose an Email client:"));
				}
				break;
				
			case R.id.miAbout:
				if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            		drawerLayout.closeDrawers();
        		}
				Intent aboutIntent = new Intent(mActivity, AboutActivity.class);
				mActivity.startActivity(aboutIntent);
				break;
		}

		return true;
	}
	
}
