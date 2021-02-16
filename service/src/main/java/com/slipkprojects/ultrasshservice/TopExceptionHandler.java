package com.slipkprojects.ultrasshservice;

import android.app.Activity;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Context;
import java.io.File;
import android.os.Environment;
import android.content.Intent;
import android.os.Handler;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.NotificationManagerCompat;
import com.slipkprojects.ultrasshservice.util.FileUtils;

/**
* Reporta erros
* @author dFiR30n
*/
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {
	private static final String FILE_ERROR = "stack.trace";
	
	private static TopExceptionHandler mExceptionHandler;
	
    private Thread.UncaughtExceptionHandler defaultUEH;
    private Context mContext;
	private File mFileTemp;
	
	// inicia
	public static void init(Context context) {
		if (mExceptionHandler == null) {
			mExceptionHandler = new TopExceptionHandler(context);
		}
		Thread.setDefaultUncaughtExceptionHandler(mExceptionHandler);
	}

    private TopExceptionHandler(Context context) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.mContext = context;
		this.mFileTemp = new File(mContext.getFilesDir(), FILE_ERROR);
	}
	
	public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
		
        String report = e.toString()+"\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (int i = 0; i < arr.length; i++) {
            report += "    " + arr[i].toString() + "\n";
        }
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
		report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++) {
                report += "    " + arr[i].toString() + "\n";
            }
        }
        report += "-------------------------------\n\n";

		// salva logs
		writeToFileLog(report, mContext);

        defaultUEH.uncaughtException(t, e);
    }
	
	private void writeToFileLog(String logError, Context context) {
		
		// salva log em armazenamento externo
		/*File dir = new File(context.getExternalFilesDir("erros"),
			"Android/data/" + context.getPackageName() + "/files");
		
		if (!dir.exists()) {
			dir.mkdir();
		}*/
		
		// salva log se possível
		//if (dir.canWrite()) {
			File logFile = new File(context.getExternalFilesDir("erros"), "SocksHttpLogError.txt");
			writeToFile(logError, logFile);
		//}
		
		// salva log temporario
		writeToFile(logError, mFileTemp);
	}
	
	private void writeToFile(String txt, File file) {
		// cria arquivo se não existir
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch(IOException error) {
				// ..
			}
		}
		// sobrescreve o log
        try {
            FileOutputStream trace = new FileOutputStream(file);
            trace.write(txt.getBytes());
            trace.close();
        } catch(IOException ioe) {
			// ..
        }
	}
}
