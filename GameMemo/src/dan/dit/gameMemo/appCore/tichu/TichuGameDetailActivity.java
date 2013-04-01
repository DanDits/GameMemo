package dan.dit.gameMemo.appCore.tichu;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.MenuItem;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.tichu.TichuGameDetailFragment.DetailViewCallback;
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
public class TichuGameDetailActivity extends FragmentActivity implements DetailViewCallback, ChoosePlayerDialogListener {
	private ActionBar mBar;
	private TichuGameDetailFragment mDetails;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new ShowStacktraceUncaughtExceptionHandler(this));
		setContentView(R.layout.tichu_detail_fragment);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mBar = getActionBar();
			if (mBar != null) {
				mBar.setHomeButtonEnabled(true);
				mBar.setDisplayHomeAsUpEnabled(true);
				mBar.setIcon(GameKey.getGameIconId(GameKey.TICHU));
				mBar.setDisplayShowTitleEnabled(true);
			}
		}
		if (savedInstanceState == null) {
			if (getIntent().getExtras() == null) {
				closeDetailView(true, false); // we need an id or player names but got nothing
				return;
			}
			// initial setup, cannot make fragment static since we need to pass arguments to it
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            mDetails = TichuGameDetailFragment.newInstance(getIntent().getExtras());
            ft.replace(R.id.game_detail_frame, mDetails);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
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
		if (mDetails != null ) {
			Intent i = new Intent();
			if (mDetails.getDisplayedGameId() == Game.NO_ID) {
				mDetails.saveState();
			}
			i.putExtra(GameStorageHelper.getCursorItemType(GameKey.TICHU), mDetails.getDisplayedGameId());
			i.putExtra(TichuGamesActivity.EXTRA_RESULT_WANT_REMATCH, rematch);
			setResult(RESULT_OK, i);
		} else {
			setResult(RESULT_OK);
		}
	}

	@Override
	public PlayerPool getPool() {
		return mDetails.getPool();
	}

	@Override
	public List<Player> toFilter() {
		return mDetails.toFilter();
	}

	@Override
	public void playerChosen(Player chosen) {
		mDetails.playerChosen(chosen);
	}

	@SuppressLint("NewApi") 
	@Override
	public void setInfo(CharSequence main, CharSequence extra) {
		// if mBar is not null then this is over honeycomb
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && mBar != null) {
			mBar.setTitle(main);
			if (TextUtils.isEmpty(extra)) {
				mBar.setSubtitle(null);
			} else {
				mBar.setSubtitle(extra);
			}
		} else {
			mDetails.setInfoText(main, extra);
		}
	}

}