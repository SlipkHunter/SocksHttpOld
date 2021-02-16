package com.slipkprojects.sockshttp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.slipkprojects.sockshttp.R;
import com.slipkprojects.sockshttp.activities.BaseActivity;

/**
 * @author anuragdhunna
 */
public class LauncherActivity extends BaseActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
		
		// inicia atividade principal
        Intent intent = new Intent(this, SocksHttpMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(intent);
		
		// encerra o launcher
        finish();
    }
	
}
