package com.slipkprojects.sockshttp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.slipkprojects.sockshttp.R;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import com.slipkprojects.sockshttp.adapter.ManagerFilesAdapter;
import com.slipkprojects.sockshttp.SocksHttpApp;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.ultrasshservice.util.FileUtils;
import com.slipkprojects.sockshttp.SocksHttpMainActivity;
import com.slipkprojects.ultrasshservice.config.ConfigParser;
import java.io.FileInputStream;
import android.support.v7.app.AlertDialog;
import java.text.DateFormat;
import com.slipkprojects.sockshttp.LauncherActivity;
import com.slipkprojects.sockshttp.preference.SettingsSSHPreference;
import com.slipkprojects.ultrasshservice.config.Settings;
import java.util.Map;
import android.support.v4.util.ArrayMap;

public class ConfigImportFileActivity extends BaseActivity implements ManagerFilesAdapter.OnItemClickListener {
	private static final String TAG = ConfigImportFileActivity.class.getSimpleName();
	
	private static final String RESTORE_CURRENT_PATH = "restoreCurrentPath";
	private static final String HOME_PATH = Environment.getExternalStorageDirectory().toString();
	private static final int PERMISSION_REQUEST_CODE = 1;
	private static final String BACK_DIR = "../";
	
	private RecyclerView rvManagerList;
	private ManagerFilesAdapter adapter;
	
	private List<ManagerFilesAdapter.ManagerItem> folderList;
	private List<ManagerFilesAdapter.ManagerItem> fileList;
	private String currentPath;
	private File backDir;
	private String pathAbertoPeloInicio;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (SkStatus.isTunnelActive()) {
			Toast.makeText(this, R.string.error_tunnel_service_execution, Toast.LENGTH_SHORT)
				.show();
			finish();
			return;
		}
		
		// Get the intent that started this activity
        Intent intent = getIntent();
		String scheme = intent.getScheme();
		
		// Figure out what to do based on the intent type
		if (scheme != null && (scheme.equals("file") || scheme.equals("content"))) {
			// mostra loading bonitinho
			setContentView(R.layout.launchvpn);
			
			Uri data = intent.getData();
			
			File file = new File(data.getPath());

			String file_extensao = getExtension(file);
			if (file_extensao != null && file_extensao.equals(ConfigParser.FILE_EXTENSAO)) {
				
				try {
					importarConfigInputFile(getContentResolver()
						.openInputStream(data));
				} catch(FileNotFoundException e) {
					Toast.makeText(this, R.string.error_file_config_incompatible,
						Toast.LENGTH_SHORT).show();
				}
			
			}
			else {
				Toast.makeText(this, R.string.error_file_config_incompatible,
					Toast.LENGTH_SHORT).show();
			}
			
			finish();
			return;
		}
		
		// set Views
		setContentView(R.layout.activity_config_import);
		this.setFinishOnTouchOutside(false);
		rvManagerList = (RecyclerView) findViewById(R.id.rvMain_ManagerList);
		setToolbar();
		
		// Para Android 6.0 Marshmallow e superior
		if (Build.VERSION.SDK_INT >= 23 && !permissionGranted()) {
			requestPermissionInfo(); // Para Android 5.1 Lollipop e inferior
			return;
		}
		
		if (savedInstanceState != null) {
			currentPath = savedInstanceState.getString(RESTORE_CURRENT_PATH);
			fileManager(currentPath);
		} else {
			//fileManager(HOME_PATH);
			startMainListManager();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(RESTORE_CURRENT_PATH, currentPath);
		super.onSaveInstanceState(outState);
	}

	
	/**
	* ItemListener implementação
	*/
	
	@Override
	public void onItemClick(View view, int position) {
		File file = new File(folderList.get(position).getDirPath());
		if (file.isDirectory()) {
			fileManager(folderList.get(position).getDirPath());
		} else {
			// Tratamento para arquivos
			String file_extensao = getExtension(file);
			if (file_extensao != null && file_extensao.equals(ConfigParser.FILE_EXTENSAO)) {
				try {
					importarConfigInputFile(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					Toast.makeText(this, R.string.error_file_not_found,
						Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	public void onItemLongClick(View view, int position)
	{
		File file = new File(folderList.get(position).getDirPath());
		if (!file.isDirectory()) {
			showApagarPrompt(file);
		}
	}
	
	private void startMainListManager() {
		folderList = new ArrayList<>();
		fileList = new ArrayList<>();
		currentPath = null;
		pathAbertoPeloInicio = null;
		
		if (mToolbar != null) {
			mToolbar.setSubtitle(R.string.select_file_setting);
		}
		
		backDir = null;
		
		String[] listDirs = {
			HOME_PATH,
			HOME_PATH + "/Download",
			HOME_PATH + "/SocksHttp"
		};
		
		for (String dir : listDirs) {
			File file = new File(dir);
			
			if (file.exists() && !file.isHidden() && file.canRead()) {
				if (file.isDirectory()) {
					String dir_name = file.getName();
					
					if (dir.equals(HOME_PATH))
						dir_name = getString(R.string.dir_home_name);
					
					folderList.add(new ManagerFilesAdapter.ManagerItem(dir_name, file.getPath(), getString(R.string.dir_name)));
				}
			}
			
		}
		
		folderList.addAll(fileList);

		adapter = new ManagerFilesAdapter(this, folderList);
		rvManagerList.setLayoutManager(new LinearLayoutManager(this));
		adapter.setOnItemClickListener(this);
		rvManagerList.setAdapter(adapter);
	}

	private void fileManager(String folderPath) {
		if (folderPath == null || folderPath.equals(BACK_DIR)) {
			if (backDir != null && canGoBackFolder())
				folderPath = backDir.getPath();
			else {
				startMainListManager();
				return;
			}
		}
		
		folderList = new ArrayList<>();
		fileList = new ArrayList<>();
		
		// está vindo do inicio
		if (currentPath == null) {
			pathAbertoPeloInicio = folderPath;
		}
		
		currentPath = folderPath;
		
		if (mToolbar != null) {
			mToolbar.setSubtitle(folderPath);
		}
		
		File path = new File(folderPath);
		if (path.getParentFile() != null && !currentPath.equals(pathAbertoPeloInicio)) {
			backDir = path.getParentFile();
		}
		
		for (File file : path.listFiles()) {
			if (!file.isHidden() && file.canRead()) {
				if (file.isDirectory()) {
					folderList.add(new ManagerFilesAdapter.ManagerItem(file.getName(), file.getPath(), getString(R.string.dir_name)));
				} else {
					String file_extensao = getExtension(file);
					if (file_extensao != null && file_extensao.equals(ConfigParser.FILE_EXTENSAO)) {
						String dateLastModified = String.format("%s %s",
							android.text.format.DateFormat.getDateFormat(this).format(file.lastModified()),
								android.text.format.DateFormat.getTimeFormat(this).format(file.lastModified()));
						
						fileList.add(new ManagerFilesAdapter.ManagerItem(file.getName(), file.getPath(), dateLastModified));
					}
				}
			}
		}

		Collections.sort(folderList);
		Collections.sort(fileList);
		folderList.addAll(fileList);

		// Adiciona a opção de voltar
		if (canGoBackFolder() || currentPath.equals(pathAbertoPeloInicio)) {
			folderList.add(0, new ManagerFilesAdapter.ManagerItem("...", BACK_DIR, "Pasta"));
		}

		adapter = new ManagerFilesAdapter(this, folderList);
		rvManagerList.setLayoutManager(new LinearLayoutManager(this));
		adapter.setOnItemClickListener(this);
		rvManagerList.setAdapter(adapter);
	}

	@Override
	public void onBackPressed() {
		if (canGoBackFolder() || currentPath != null && currentPath.equals(pathAbertoPeloInicio)) {
			fileManager(BACK_DIR);
		}
		else {
			super.onBackPressed();
		}
	}

	private boolean canGoBackFolder() {
		if (backDir != null) {
			return backDir.canRead() && !backDir.getPath().equals(currentPath);
		}
		return false;
	}

	private void requestPermissionInfo() {
		if (ActivityCompat.shouldShowRequestPermissionRationale(ConfigImportFileActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			// Dialog de informação para caso o usuário já tenha negado as permissões pelo menos uma vez
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.title_permission_request);
			dialog.setMessage(R.string.message_permission_request);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int position) {
					ActivityCompat.requestPermissions(ConfigImportFileActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
					dialogInterface.dismiss();
				}
			});
			dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int position) {
					dialogInterface.dismiss();
					finish();
				}
			});
			dialog.show();
		} else {
			ActivityCompat.requestPermissions(ConfigImportFileActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
		}
	}

	// Método chamado assim que o usuário concede ou nega uma permissão
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_CODE:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//fileManager(HOME_PATH);
					startMainListManager();
				} else {
					requestPermissionInfo();
				}
			break;
		}
	}

	private boolean permissionGranted() {
		int result = ContextCompat.checkSelfPermission(ConfigImportFileActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (result == PackageManager.PERMISSION_GRANTED) {
			return true;
		}
		return false;
	}
	
	public String getExtension(File file) {
		String filename = file.getAbsolutePath();
		
		if (filename.contains(".")) {
			return filename.substring(filename.lastIndexOf(".") + 1);
		}
		
		return "";
	}
	
	public void importarConfigInputFile(InputStream inputFile) {
		try {
			
			if (!ConfigParser.convertInputAndSave(inputFile, this)) {
				throw new IOException(getString(R.string.error_save_settings));
			}
			
			long mValidade = new Settings(this)
				.getPrefsPrivate().getLong(Settings.CONFIG_VALIDADE_KEY, 0);
			
			if (mValidade > 0) {
				SkStatus.logInfo(R.string.log_settings_valid,
					android.text.format.DateFormat.getDateFormat(this).format(mValidade));
			}
			
			Toast.makeText(this, R.string.success_import_settings, Toast.LENGTH_SHORT)
				.show();
				
			// atualiza views
			SocksHttpMainActivity.updateMainViews(this);
			
		} catch(IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT)
				.show();
		}
		
		Intent intent = new Intent(this, LauncherActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		
		finish();
	}
	
	
	/**
	* Toolbar
	*/
	
	private Toolbar mToolbar;
	
	private void setToolbar() {
		mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	private void showApagarPrompt(final File file) {
		AlertDialog dialog = new AlertDialog.Builder(this)
			.create();
		
			dialog.setTitle(R.string.title_delete_file);
			dialog.setMessage(getString(R.string.alert_delete_file));

			dialog.setButton(dialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					if (file.delete())
						fileManager(currentPath);
					else
						Toast.makeText(getApplicationContext(), R.string.error_delete_file,
							Toast.LENGTH_SHORT).show();
				}
			});

			dialog.setButton(dialog.BUTTON_NEGATIVE, getString(R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface p1, int p2) {}
			});

		dialog.show();
	}

}
