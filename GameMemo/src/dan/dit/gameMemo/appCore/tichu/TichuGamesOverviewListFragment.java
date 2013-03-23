package dan.dit.gameMemo.appCore.tichu;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;

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
public class TichuGamesOverviewListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int DELETE_ID = Menu.FIRST + 1;
	private static final int INFO_ID = Menu.FIRST + 2;
	private static final int GAMEKEY = GameKey.TICHU;
	private static final String STORAGE_HIGHLIGHTED_GAME_ID = "STORAGE_HIGHLIGHTED_GAME";
	// first unfinished games then finished games, most recent games first for both parts
	private static final String TICHU_GAMES_SORT_ORDER = 
			"CASE " + GameStorageHelper.COLUMN_WINNER 
			+ " WHEN " + Game.WINNER_NONE + " THEN " + GameStorageHelper.COLUMN_STARTTIME + " END DESC, "
			+ GameStorageHelper.COLUMN_STARTTIME + " DESC";

	private TichuGameOverviewAdapter adapter;
	private GameSelectionListener mGameSelectionListener;
	
	public interface GameSelectionListener {
		long getSelectedGameId();
		void selectGame(long gameId);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mGameSelectionListener = (GameSelectionListener) activity;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.getListView().setDividerHeight(2);
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
 			adapter.setHighlightedGameId(gameId);
 		}
 	}
 	
 	public long getHighlightedGameId() {
 		if (adapter != null) {
 			return adapter.getHighlightedGameId();
 		} else {
 			return Game.NO_ID;
 		}
 	}

 	private void openGame(long id) {
 		mGameSelectionListener.selectGame(id);
 	}
 	
 	private void fillData() {
 		// Fields from the database (projection)
 		// Must include the _id column for the adapter to work
 		String[] from = new String[] { GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
 				GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_ID};
 	 	getLoaderManager().initLoader(0, null, this);
 		
 		adapter = new TichuGameOverviewAdapter(this.getActivity(), R.layout.tichu_game_overview, null, from,
 				new int[from.length]);
 		setListAdapter(adapter);
 	}
 	
 	@Override
 	public void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		outState.putLong(STORAGE_HIGHLIGHTED_GAME_ID, adapter.getHighlightedGameId());
 	}

 	@Override
 	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (item.getMenuInfo() instanceof AdapterContextMenuInfo) 
				? (AdapterContextMenuInfo) item.getMenuInfo() : null;
		final long gameId = (info != null) ? info.id : -1;
 		switch (item.getItemId()) {
 		case DELETE_ID: 
 			askAndPerformDelete(gameId);
 			return true;
 		case INFO_ID:
 			showGameInfo(gameId);
 			return true;
 		}
 		return super.onContextItemSelected(item);
 	}
 	
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v,
 			ContextMenuInfo menuInfo) {
 		super.onCreateContextMenu(menu, v, menuInfo);
 		menu.add(0, DELETE_ID, 0, R.string.menu_delete_game);
 		menu.add(0, INFO_ID, 0, R.string.menu_info_game);
 	}
 	
 	private void showGameInfo(long gameId) {
 		Resources res = getActivity().getResources();
		List<Game> games = null;
		try {
			games = GameKey.loadGames(GAMEKEY, getActivity().getContentResolver(), GameStorageHelper.getUri(GAMEKEY, gameId));
		} catch (CompressedDataCorruptException e) {
			Toast.makeText(getActivity(), res.getString(R.string.game_failed_loading), Toast.LENGTH_SHORT).show();
		}
		if (games != null && games.size() > 0) {
			String formattedInfo = games.get(0).getFormattedInfo(res);
			new AlertDialog.Builder(getActivity())
			.setTitle(getResources().getString(R.string.menu_info_game))
			.setMessage(formattedInfo)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setNeutralButton(android.R.string.ok, null).show();
		}
 	}

 	private void askAndPerformDelete(final long gameId) {
 		if (!Game.isValidId(gameId)) {
 			return;
 		}
 		// better prompt user, to check if the game really should be deleted
		new AlertDialog.Builder(getActivity())
		.setTitle(getResources().getString(R.string.confirm_delete))
		.setMessage(getResources().getString(R.string.confirm_delete_tichu_game))
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

		    public void onClick(DialogInterface dialog, int whichButton) {
		    	if (mGameSelectionListener.getSelectedGameId() == gameId) {
		    		mGameSelectionListener.selectGame(Game.NO_ID);
		    	}
	 			Uri uri = GameStorageHelper.getUri(GAMEKEY, gameId);
	 			getActivity().getContentResolver().delete(uri, null, null);
		    }})
		 .setNegativeButton(android.R.string.no, null).show();
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
			Cursor oldCursor = adapter.swapCursor(data);
			if (oldCursor != null) {
				oldCursor.close();
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
}
