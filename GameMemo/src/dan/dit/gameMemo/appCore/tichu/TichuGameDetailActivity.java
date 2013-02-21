package dan.dit.gameMemo.appCore.tichu;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar bar = getActionBar();
			if (bar != null) {
				bar.hide();
			}
		}
		if (savedInstanceState == null) {
			if (getIntent().getExtras() == null) {
				closeDetailView(true); // we need an id or player names but got nothing
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
	public void onBackPressed() {
		prepareSuccessfulResult();
		super.onBackPressed();
	}

	@Override
	public void closeDetailView(final boolean error) {
		if (!error) {
			prepareSuccessfulResult();
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}
	
	private void prepareSuccessfulResult() {
		Fragment frag = getSupportFragmentManager().findFragmentById(R.id.game_detail_frame);
		if (frag != null && frag instanceof TichuGameDetailFragment) {
			TichuGameDetailFragment details = (TichuGameDetailFragment) frag;
			Intent i = new Intent();
			if (details.getDisplayedGameId() == Game.NO_ID) {
				details.saveState();
			}
			i.putExtra(GameStorageHelper.getCursorItemType(GameKey.TICHU), details.getDisplayedGameId());
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