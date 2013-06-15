package dan.dit.gameMemo.appCore;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothDataExchangeActivity;
import dan.dit.gameMemo.dataExchange.file.FileWriteDataExchangeActivity;
import dan.dit.gameMemo.gameData.game.Game.PlayerRenamedListener;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.RenamePlayerDialogFragment;
import dan.dit.gameMemo.storage.GameStorageHelper;

public class GameChooserActivity extends FragmentActivity implements PlayerRenamedListener {
	public static final String EXTRA_LAST_GAME = "dan.dit.gameMemo.extra_last_game_key";
	private Button mTichu;
	private Button mDoppelkopf;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar bar = getActionBar();
			if (bar != null) {
				bar.hide();
			}
		}
		setContentView(R.layout.game_chooser);
		mTichu = (Button) findViewById(R.id.tichu);
		mDoppelkopf = (Button) findViewById(R.id.doppelkopf);
		Game.loadAllPlayers(GameKey.TICHU, getContentResolver());
		initListeners();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mTichu.setText(getGameDescription(GameKey.TICHU));
		mDoppelkopf.setText(getGameDescription(GameKey.DOPPELKOPF));
	}
	
	private String getGameDescription(int gameKey) {
		int count = GameStorageHelper.getStoredGamesCount(getContentResolver(), gameKey);
		return getResources().getString(R.string.game_chooser_game_descr, GameKey.getGameName(gameKey), count);
	}
	private void initListeners() {
		mTichu.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startGameActivity(GameKey.TICHU);
			}
		});
		mDoppelkopf.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startGameActivity(GameKey.DOPPELKOPF);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_chooser, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.open_data_exchanger_bluetooth:
			i = BluetoothDataExchangeActivity.newInstance(this, GameKey.ALL_GAMES);
			startActivity(i);
			return true;
		case R.id.share:
			i = FileWriteDataExchangeActivity.newInstance(this, GameKey.ALL_GAMES, true);
			startActivity(i);
			return true;
		case R.id.rename_player:
	        DialogFragment dialog = RenamePlayerDialogFragment.newInstance(GameKey.ALL_GAMES, null);
	        dialog.show(getSupportFragmentManager(), "RenamePlayerDialogFragment");
			return true;
		case R.id.about:
			showAboutDialog();
			return true;
		
		}
		return super.onOptionsItemSelected(item);
	}
	
 	private void showAboutDialog() {
		String versionName = "";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// will not happen since we ask for own own package which exists
		}
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.about))
		.setMessage(getResources().getString(R.string.about_summary, versionName))
		.setIcon(android.R.drawable.ic_dialog_info)
		.setNeutralButton(android.R.string.ok, null)
		.show();
	}
	
	private void startGameActivity(int gameKey) {
		Intent i = new Intent(GameChooserActivity.this, GameKey.getGamesActivity(gameKey));
		i.putExtra(GameKey.EXTRA_GAMEKEY, gameKey);
		startActivity(i);	
	}

	@Override
	public void playerRenamed(String oldName, String newName, boolean success) {
		// ignore
	}
}
