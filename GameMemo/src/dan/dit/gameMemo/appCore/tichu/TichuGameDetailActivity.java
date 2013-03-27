package dan.dit.gameMemo.appCore.tichu;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.tichu.TichuGameDetailFragment.CloseDetailViewRequestListener;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ShowStacktraceUncaughtExceptionHandler;

/**
 * This activity hosts a {@link TichuGameDetailFragment} and passes its intent extras
 * to its fragment.
 * @author Daniel
 *
 */
public class TichuGameDetailActivity extends FragmentActivity implements CloseDetailViewRequestListener, ChoosePlayerDialogListener {
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new ShowStacktraceUncaughtExceptionHandler(this));
		setContentView(R.layout.tichu_detail_fragment);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar bar = getActionBar();
			if (bar != null) {
				bar.setHomeButtonEnabled(true);
				bar.setDisplayHomeAsUpEnabled(true);
				bar.setIcon(GameKey.getGameIconId(GameKey.TICHU));
			}
		}
		if (savedInstanceState == null) {
			if (getIntent().getExtras() == null) {
				closeDetailView(true, false); // we need an id or player names but got nothing
				return;
			}
			// initial setup, cannot make fragment static since we need to pass arguments to it
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            TichuGameDetailFragment details = TichuGameDetailFragment.newInstance(getIntent().getExtras());
            ft.replace(R.id.game_detail_frame, details);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; simply finish since we always came from TichuGamesActivity
	        	closeDetailView(false, false);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onBackPressed() {
		prepareSuccessfulResult(false);
		super.onBackPressed();
	}

	@Override
	public void closeDetailView(final boolean error, boolean rematch) {
		if (!error) {
			prepareSuccessfulResult(rematch);
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}
	
	private void prepareSuccessfulResult(boolean rematch) {
		Fragment frag = getSupportFragmentManager().findFragmentById(R.id.game_detail_frame);
		if (frag != null && frag instanceof TichuGameDetailFragment) {
			TichuGameDetailFragment details = (TichuGameDetailFragment) frag;
			Intent i = new Intent();
			if (details.getDisplayedGameId() == Game.NO_ID) {
				details.saveState();
			}
			i.putExtra(GameStorageHelper.getCursorItemType(GameKey.TICHU), details.getDisplayedGameId());
			i.putExtra(TichuGamesActivity.EXTRA_RESULT_WANT_REMATCH, rematch);
			setResult(RESULT_OK, i);
		} else {
			setResult(RESULT_OK);
		}
	}

	@Override
	public PlayerPool getPool() {
		TichuGameDetailFragment frag = (TichuGameDetailFragment) getSupportFragmentManager().findFragmentById(R.id.game_detail_frame);
		return frag.getPool();
	}

	@Override
	public List<Player> toFilter() {
		TichuGameDetailFragment frag = (TichuGameDetailFragment) getSupportFragmentManager().findFragmentById(R.id.game_detail_frame);
		return frag.toFilter();
	}

	@Override
	public void playerChosen(Player chosen) {
		TichuGameDetailFragment frag = (TichuGameDetailFragment) getSupportFragmentManager().findFragmentById(R.id.game_detail_frame);
		frag.playerChosen(chosen);
	}

}