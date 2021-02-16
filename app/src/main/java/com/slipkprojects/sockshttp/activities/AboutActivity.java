package com.slipkprojects.sockshttp.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import com.slipkprojects.sockshttp.R;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;
import com.slipkprojects.sockshttp.util.Utils;
import android.text.Html;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.ads.AdView;
import com.slipkprojects.ultrasshservice.tunnel.TunnelUtils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.slipkprojects.sockshttp.SocksHttpApp;
import com.slipkprojects.sockshttp.BuildConfig;

public class AboutActivity extends BaseActivity
{
	private AdView adsBannerView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		// toolbar
		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		try {
			PackageInfo pm = getPackageManager().getPackageInfo(getPackageName(), 0);
			String version = String.format("%s (Build %d)", pm.versionName, pm.versionCode);
		
			TextView versionName = (TextView) findViewById(R.id.versionName);
			versionName.setText(version);
		} catch (PackageManager.NameNotFoundException e) {}

		Button showLicense = (Button) findViewById(R.id.activity_aboutShowLicenseButton);
		showLicense.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showLicenses();
			}
		});
		
		showAgradecimentos();
		
		adsBannerView = (AdView) findViewById(R.id.adBannerSecondView);
		if (!BuildConfig.DEBUG) {
			//adsBannerView.setAdUnitId(SocksHttpApp.ADS_UNITID_BANNER_SOBRE);
		}
		
		// carrega anúncio
		if (TunnelUtils.isNetworkOnline(this)) {
			
			adsBannerView.setAdListener(new AdListener() {
				@Override
				public void onAdLoaded() {
					if (adsBannerView != null) {
						adsBannerView.setVisibility(View.VISIBLE);
					}
				}
			});

			adsBannerView.loadAd(new AdRequest.Builder()
				.build());
		}
	}

	@Override
	public boolean onSupportNavigateUp()
	{
		onBackPressed();
		return true;
	}
	
	protected void showLicenses() {
		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.fragment_dialog_licenses, null); 
		
		try
		{
			String aboutText = Utils.readFromAssets(this,"LICENSE");
			aboutText = aboutText.replace("\n","<br/>");
			
			((TextView) view.findViewById(R.id.fragment_dialog_licensesAllTextView))
				.setText(Html.fromHtml(aboutText));
		}
		catch (Exception e){}
		
		new AlertDialog.Builder(this)
            .setTitle("Licenses")
            .setView(view)
            .show();
	}
	
	public void showAgradecimentos() {
		try
		{
			String aboutText = Utils.readFromAssets(this,"AGRADECIMENTOS");
			aboutText = aboutText.replace("\n","<br/>");

			((TextView) findViewById(R.id.activity_aboutAgradecimentosTextView))
				.setText(Html.fromHtml(aboutText));
		}
		catch (Exception e){}
	}

	@Override
	protected void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		
		if (adsBannerView != null) {
			adsBannerView.resume();
		}
	}

	@Override
	protected void onPause()
	{
		// TODO: Implement this method
		super.onPause();
		
		if (adsBannerView != null) {
			adsBannerView.pause();
		}
	}

	@Override
	protected void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		
		if (adsBannerView != null) {
			adsBannerView.destroy();
		}
	}
	
}

