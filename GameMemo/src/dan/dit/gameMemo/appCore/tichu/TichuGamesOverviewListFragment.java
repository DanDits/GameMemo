package dan.dit.gameMemo.appCore.tichu;

import java.util.Collection;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.tichu.TichuGameOverviewAdapter.GameCheckedChangeListener;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.Game.GamesDeletionListener;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.GameStorageHelper;

/**
 * This fragment displays all stored tichu games, sorting unfinished games to the top.
 * Games can be deleted from the item context menu after the user confirms the deletion.
 * If a tichu game is selected from the list, the {@link TichuGameDetailActivity} is started
 * with the intent extra set to the game's uri.
 * <b>Requires the hosting activity to implement the {@link GameSelectionListener} interface to listen for
 * events when the user wants to select a certain game.</b>
 * @author Daniel
 *
 */
public class TichuGamesOverviewListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, GamesDeletionListener {
	private static final int GAMEKEY = GameKey.TICHU;
	private static final String STORAGE_HIGHLIGHTED_GAME_ID = "STORAGE_HIGHLIGHTED_GAME";
	// first unfinished games then finished games, most recent games first for both parts: 
	/* "CASE " + GameStorageHelper.COLUMN_WINNER 
			+ " WHEN " + Game.WINNER_NONE + " THEN " + GameStorageHelper.COLUMN_STARTTIME + " END DESC, "
			+ GameStorageHelper.COLUMN_STARTTIME + " DESC";
			*/
	private static final String TICHU_GAMES_SORT_ORDER = GameStorageHelper.COLUMN_STARTTIME + " DESC";

	private TichuGameOverviewAdapter adapter;
	private GameOverviewCallback mGameOverviewCallback;
	
	public interface GameOverviewCallback extends GameCheckedChangeListener {
		long getSelectedGameId();
		void selectGame(long gameId);
		/**
		 * Convenience method that suggests activity to start game setup, can be ignored to avoid loops or other situations.
		 */
		void setupGame();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mGameOverviewCallback = (GameOverviewCallback) activity;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setSelector(GameKey.getSelectorResource(GAMEKEY));
		fillData();
		registerForContextMenu(getListView());
		restoreFromSave(savedInstanceState);
	}
	
	private void restoreFromSave(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			return;
		}
		setHighlightedGameId(savedInstanceState.getLong(STORAGE_HIGHLIGHTED_GAME_ID, Game.NO_ID));
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tichu_list, container, false);
    }
    
    // Opens the detail activity if an entry is clicked
 	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
 		super.onListItemClick(l, v, position, id);
 		openGame(id);
 	}
 	
 	public void setHighlightedGameId(long gameId) {
 		if (adapter != null) {
 			if (gameId == Game.NO_ID) {
 				getListView().clearChoices();
 				getListView().requestLayout();
 			} else {
 				int pos = getPositionForId(gameId);
 				if (pos >= 0) {
 					getListView().setItemChecked(pos, true); 
 				}
 			}
 		}
 	}
 	
 	private int getPositionForId(long id) {
 		ListView view = getListView();
 		for (int pos = 0; pos < view.getCount(); pos++) {
 			if (view.getItemIdAtPosition(pos) == id) {
 				return pos;
 			}
 		}
 		return -1;
 	}
 	
 	public long getHighlightedGameId() {
 		if (adapter != null) {
 	 		long[] ids = getListView().getCheckedItemIds();
 			if (ids.length > 0) {
 				return ids[0];
 			} else {
 				long id = getListView().getSelectedItemId();
 				if (id != AdapterView.INVALID_ROW_ID) {
 					setHighlightedGameId(id);
 					return id;
 				}
 			}
 		}
 		return Game.NO_ID;
 	}

 	private void openGame(long id) {
 		mGameOverviewCallback.selectGame(id);
 	}
 	
 	private void fillData() {
 		// Fields from the database (projection)
 		// Must include the _id column for the adapter to work
 		String[] from = new String[] { GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
 				GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_ID};
 	 	getLoaderManager().initLoader(0, null, this);
 		
 		adapter = new TichuGameOverviewAdapter(this.getActivity(), R.layout.tichu_game_overview, null, from,
 				new int[from.length]);
 		adapter.setOnGameCheckedChangeListener(new GameCheckedChangeListener() {
			
			@Override
			public void onGameCheckedChange(Collection<Long> checkedIds) {
				mGameOverviewCallback.onGameCheckedChange(checkedIds);
			}
		});
 		setListAdapter(adapter);
 	}
 	
 	@Override
 	public void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		outState.putLong(STORAGE_HIGHLIGHTED_GAME_ID, getHighlightedGameId());
 	}
 	
 	public Collection<Long> getCheckedIds() {
 		return adapter.getChecked();
 	}
	
	public Collection<Long> getCheckedGamesStarttimes() {
		return adapter.getCheckedStarttimes();
	}
	
	@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			String[] projection = { GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
					GameStorageHelper.COLUMN_WINNER};
			CursorLoader cursorLoader = new CursorLoader(this.getActivity(),
					GameStorageHelper.getUriAllItems(GAMEKEY), projection, null, null, TICHU_GAMES_SORT_ORDER);
			return cursorLoader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			Cursor oldCursor = null;
			try {
				adapter.swapCursor(data);
				if (data.getCount() == 0) {
					mGameOverviewCallback.setupGame();
				}
			} finally {
				if (oldCursor != null) {
					oldCursor.close();
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// data is not available anymore, delete reference
			Cursor oldCursor = adapter.swapCursor(null);
			if (oldCursor != null) {
				oldCursor.close();
			}
		}

		@Override
		public void deletedGames(Collection<Long> deletedIds) {
			adapter.deletedGames(deletedIds);
		}
}
