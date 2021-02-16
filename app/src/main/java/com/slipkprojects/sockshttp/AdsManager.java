package com.slipkprojects.sockshttp;

import android.app.Activity;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.AdListener;
import android.os.SystemClock;
import android.util.Log;
import com.google.android.gms.ads.AdRequest;
import android.content.Context;
import android.widget.Toast;
import android.os.CountDownTimer;
import android.content.SharedPreferences;

/**
* Ads Manager
* @author dFiR30n
*/

public class AdsManager
{
	private final String TAG = "AdsManager";
	
	private Context mContext;
	private SharedPreferences mPrefs;
	
	private InterstitialAd mInterstitialAd;
	
	public static AdsManager newInstance(Context context) {
		return new AdsManager(context);
	}
	
	private AdsManager(Context context){
		mContext = context;
		mPrefs = context.getSharedPreferences(SocksHttpApp.PREFS_GERAL, Context.MODE_PRIVATE);
		
		// Ads interstitial
		setupAdsInterstitial();
	}
	
	private void setupAdsInterstitial() {
		mInterstitialAd = new InterstitialAd(mContext);
		
		if (!BuildConfig.DEBUG)
			mInterstitialAd.setAdUnitId(SocksHttpApp.ADS_UNITID_INTERSTITIAL_MAIN);
		else
			mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
		
		mInterstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				if (mInterstitialAd != null) {
					mInterstitialAd.show();
				}
			}

			@Override
			public void onAdFailedToLoad(int errorCode) {
				// Code to be executed when an ad request fails.
			}

			@Override
			public void onAdOpened() {
				// Code to be executed when the ad is displayed.
				if (mPrefs != null) {
					SharedPreferences.Editor pEdit = mPrefs.edit();
					pEdit.putLong("last_ads_time", SystemClock.elapsedRealtime());
					pEdit.apply();
				}
			}

			@Override
			public void onAdClicked() {
				// Code to be executed when the user clicks on an ad.
			}

			@Override
			public void onAdLeftApplication() {
				// Code to be executed when the user has left the app.
			}

			@Override
			public void onAdClosed() {
				// Code to be executed when the interstitial ad is closed.
				Toast.makeText(mContext, "Obrigado pôr apoiar o app!! 💙", Toast.LENGTH_SHORT)
					.show();
			}
		});
	}
	
	public void loadAdsInterstitial() {
		// carrega anúncio a cada 1 hora
		long time = 60*60*1;
		if (mInterstitialAd != null && ((SystemClock.elapsedRealtime() - mPrefs.getLong("last_ads_time", 0)) / 1000) >= time){
			mInterstitialAd.loadAd(new AdRequest.Builder().build());
			Log.d(TAG, "Carregando anúncio interstitial..");
		}
	}
	
	
	/**
	* Ads Timer
	*/
	private CountDownTimer countDownTimer;
	private long timerMilliseconds;
	
	private void createTimer(final long milliseconds) {
        // Create the game timer, which counts down to the end of the level
        // and shows the "retry" button.
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(milliseconds, 50) {
            @Override
            public void onTick(long millisUnitFinished) {
                timerMilliseconds = millisUnitFinished;
            }

            @Override
            public void onFinish() {
				loadAdsInterstitial(); // carrega novo anúncio
            }
        };
    }
	
}
