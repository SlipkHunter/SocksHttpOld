package com.slipkprojects.ultrasshservice.util;

import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.app.Activity;
import android.Manifest;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import android.os.Environment;
import com.slipkprojects.ultrasshservice.R;

public class FileUtils {
	private static final String TAG = "FileUtils";
	
	// requisita permissão de escrita e leitura
	public static void requestForPermissionExternalStorage(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
			!= PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                promptForPermissionsDialog(activity, activity.getString(R.string.error_request_permission), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ActivityCompat.requestPermissions(activity,
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
					}
				});

            } else {
				ActivityCompat.requestPermissions(activity,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
			
        }
    }
    private static void promptForPermissionsDialog(Context context, String message, DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message)
			.setPositiveButton(context.getString(R.string.ok), onClickListener)
			.setNegativeButton(context.getString(R.string.cancel), null)
			.create()
			.show();
    }
	
	/* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
	
	public static void copiarArquivo(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
	
	public static String readFromRaw(Context context, int resId) {
        InputStream in = context.getResources().openRawResource(resId);
        Scanner scanner = new Scanner(in,"UTF-8").useDelimiter("\\A");
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.next());
        }
        scanner.close();
        return sb.toString();
    }
	
	public static String readTextFile(File f){
		StringBuilder text = new StringBuilder();

		try {
			BufferedReader br = new BufferedReader(new FileReader(f));

			String st;
			while((st = br.readLine()) != null){
				text.append(st + "\n");
			}

			br.close();

			return text.toString();
		} catch(Exception e){
			return null;
		}
	}
	
	public static boolean saveTextFile(File file, String contents)
	{
		try {

			if (!file.exists()) {
				file.createNewFile();
			}
			
			FileWriter writer = new FileWriter(file, false);
			writer.write(contents);

			writer.close();

			return true;

		} catch (IOException e) {
			//	Log.d(TAG, "error writing file: " + path, e);
			e.printStackTrace();
			return false;
		}
	}
}
