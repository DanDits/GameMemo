package dan.dit.gameMemo.appCore;

import java.util.Date;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;

public abstract class GameDetailFragment extends ListFragment implements
		ChoosePlayerDialogListener {	
	/**
	 * Indicates if games that are loaded and are already finished are immutable and game rounds cannot be added or changed.
	 */
	public static final boolean LOADED_FINISHED_GAMES_ARE_IMMUTABLE = true;
	
	/**
	 * A callback interface that is required for the hosting activity.
	 * @author Daniel
	 *
	 */
	public interface DetailViewCallback {
		void closeDetailView(boolean error, boolean rematch);
		void setInfo(CharSequence main, CharSequence extra);
	}

	protected boolean mIsLoadedFinishedGame;
	protected Date mLastRunningTimeUpdate;
	
	protected String getBluetoothDeviceName() {
		if (BluetoothAdapter.getDefaultAdapter() != null) {
			return BluetoothAdapter.getDefaultAdapter().getName();
		} else {
			return "";
		}
	}	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.info:
			showGameInfo();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		selectRoundSmart(position);
	}
	
	protected final void selectRoundSmart(int position) {
		if (position < 0) {
			deselectRound();
		} else {
			selectRound(position);
		}
	}

	protected abstract boolean isImmutable();
	protected abstract void deselectRound();
	protected abstract void selectRound(int position);
	protected abstract void showGameInfo();
	public abstract long getDisplayedGameId();

	protected void saveState() {
		Game game = getGame();
		if (game != null) {
			if (mLastRunningTimeUpdate != null) {
				Date currTime = new Date();
				game.addRunningTime(currTime.getTime() - mLastRunningTimeUpdate.getTime());
				mLastRunningTimeUpdate = currTime;
			}
			if (game.isFinished()) {
				mLastRunningTimeUpdate = null;
			}
			game.saveGame(getActivity().getContentResolver());
		}
	}
	
	protected abstract Game getGame();
	protected abstract void setInfoText(CharSequence main, CharSequence extra);

}
