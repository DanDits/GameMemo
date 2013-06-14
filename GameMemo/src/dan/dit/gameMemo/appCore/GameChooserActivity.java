package dan.dit.gameMemo.appCore;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;

public class GameChooserActivity extends FragmentActivity {
	public static final String EXTRA_LAST_GAME = "dan.dit.gameMemo.extra_last_game_key";
	private ImageButton mTichu;
	private ImageButton mDoppelkopf;
	
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
		mTichu = (ImageButton) findViewById(R.id.tichu);
		mDoppelkopf = (ImageButton) findViewById(R.id.doppelkopf);
		initListeners();
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
	
	private void startGameActivity(int gameKey) {
		Intent i = new Intent(GameChooserActivity.this, GameKey.getGamesActivity(gameKey));
		i.putExtra(GameKey.EXTRA_GAMEKEY, gameKey);
		startActivity(i);	
	}
}
