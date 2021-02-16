package com.slipkprojects.compatstyle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class NotImplemented extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		
		Toast.makeText(this, "Nada aqui", Toast.LENGTH_SHORT)
			.show();
		finish();
	}
}
