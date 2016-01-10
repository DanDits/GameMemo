package dan.dit.gameMemo.appCore;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;

/**
 * This fragments holds all information necessary to show and edit a new or
 * already existing game instance. Usually requires a gamekey and id to display
 * an existing game or a gamekey and parameters to create a new game. The game is saved
 * automatically.<b>Requires the hosting activity to implement the {@link DetailViewCallback} interface to listen to
 * requests of the user to close this detail fragment.</b>
 * @author Daniel
 *
 */
public abstract class GameDetailFragment extends ListFragment implements
		ChoosePlayerDialogListener {	
	/**
	 * Indicates if games that are loaded and are already finished are immutable and game rounds cannot be added or changed.
	 */
	public static final boolean LOADED_FINISHED_GAMES_ARE_IMMUTABLE = true;

    protected static final String STORAGE_IS_IMMUTABLE = "STORAGE_IS_IMMUTABLE";
    
	/**
	 * A callback interface that is required for the hosting activity to hide the fragment or display
	 * information about the game.
	 * @author Daniel
	 *
	 */
	public interface DetailViewCallback {
	    /**
	     * Called by the fragment to indicate that it wants to hide.
	     * @param error If <code>true</code>, the closing was not triggered by the user.
	     * @param rematch If <code>true</code>, the user wants a rematch, open GameSetupActivity and
	     * extract as many parameters from the current game as possible to simplify matters.
	     */
		void closeDetailView(boolean error, boolean rematch);
		
		/**
		 * Called by the fragment to display information about the game, that is important specific
		 * to the game. The callback is responsible to display the info, but is also allowed to hand it back 
		 * to the fragment if there is no better way of displaying.
		 * @param main Important information that needs to be stressed and easier visible.
		 * @param extra Additional information like game options.
		 */
		void setInfo(CharSequence main, CharSequence extra);
	}


    protected DetailViewCallback mCallback;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallback = (DetailViewCallback) activity; // throws if given activity does not listen to close requests
        if (!(activity instanceof DetailViewCallback)) {
            throw new ClassCastException("Hosting activity must implement DetailViewCallback interface.");
        }
    }
    
	/**
	 * If the game is loaded from the database and is already finished at this time, this flag is set to true.
	 */
	protected boolean mIsLoadedFinishedGame;
	
	/**
	 * Stores the date the last time game.addRunningTime() was invoked on the game instance.
	 */
	protected Date mLastRunningTimeUpdate;
	
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
	
	/**
	 * Invokes selectRound(position) if position is not negative
	 * or deselectRound() if position is negative.
	 * @param position The round to select or negative if round should
	 * be deselected.
	 */
	protected final void selectRoundSmart(int position) {
		if (position < 0) {
			deselectRound();
		} else {
			selectRound(position);
		}
	}
	   
	/**
     * Returns the game id of the game instance or Game.NO_ID if there is no game
     * displayed or if this game does not yet have a game id. 
     * @return The id of the displayed game or Game.NO_ID.
     */
    public long getDisplayedGameId() {
        Game game = getGame();
        return game == null ? Game.NO_ID : game.getId();
    }
    
	/**
	 * Returns true if the game instance is immutable. This requires the fragment to not allow
	 * changes to the game instance.
	 * @return If the game instance is immutable.
	 */
	protected abstract boolean isImmutable();
	
	/**
	 * Deselects the current round, can be invoked if there is no round selected too.
	 */
	protected abstract void deselectRound();
	
	/**
	 * Selects the given round. Fragment needs to check if there is a round at the given position first.
	 * @param position The position of the round to select.
	 */
	protected abstract void selectRound(int position);
	
	/**
	 * Shows additional game information like the runtime or starttime for guys that just can't have enough
	 * information.
	 */
	protected void showGameInfo() {
        Resources res = getActivity().getResources();
        if (getGame() != null) {
            String formattedInfo = getGame().getFormattedInfo(res);
            new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.menu_info_game))
            .setMessage(formattedInfo)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setNeutralButton(android.R.string.ok, null).show();
        }
	}
	


	/**
	 * Saves the current game, updating its running time. This updates
	 * the database if the game was already saved or creates a new database entry.
	 * Afterwards the game will have a valid id. Though there is no guarantee that the
	 * id will be valid when the method returns.
	 */
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
	
	/**
	 * Returns the displayed game.
	 * @return The displayed game.
	 */
	protected abstract Game getGame();
	
	/**
	 * Returns the game key of the displayed game.
	 * @return The game key of the displayed game.
	 */
	protected abstract int getGameKey();
	
	/**
	 * If the hosting activity cannot or does not want to display the information given at the callback by
	 * the fragment, this method will be invoked, giving the responsibility back to the fragment. So do not
	 * invoked the callback method here but either ignore or handle the text yourself.
	 * @param main The important information.
	 * @param extra The extra information.
	 */
	protected abstract void setInfoText(CharSequence main, CharSequence extra);

}
