package dan.dit.gameMemo.appCore;

import java.util.Collection;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import dan.dit.gameMemo.appCore.GameOverviewAdapter.GameCheckedChangeListener;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.Game.GamesDeletionListener;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.GameStorageHelper;

public abstract class GamesOverviewListFragment extends ListFragment implements
		LoaderCallbacks<Cursor>, GamesDeletionListener {
	private static final String STORAGE_HIGHLIGHTED_GAME_ID = "STORAGE_HIGHLIGHTED_GAME";
	private int mGameKey;
	private GameOverviewAdapter adapter;
	private GameOverviewCallback mGameOverviewCallback;
	
	public interface GameOverviewCallback extends GameCheckedChangeListener {
		long getSelectedGameId();
		void selectGame(long gameId);
		/**
		 * Convenience method that suggests activity to start game setup, can be ignored to avoid loops or other situations.
		 */
		void onGamesLoaded(int count);
		int getGameKey();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mGameOverviewCallback = (GameOverviewCallback) activity;
		mGameKey = mGameOverviewCallback.getGameKey();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setSelector(GameKey.getSelectorResource(mGameKey));
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
        return inflater.inflate(GameKey.getGameOverviewListLayout(mGameKey), container, false);
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
 	
 	protected abstract GameOverviewAdapter makeAdapter();
 	
 	private void fillData() {
 		adapter = makeAdapter();
 	 	getLoaderManager().initLoader(0, null, this);
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
	
	protected abstract String getSortOrder();
	protected abstract String[] getProjection();
	
	@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			String[] projection = getProjection();
			CursorLoader cursorLoader = new CursorLoader(this.getActivity(),
					GameStorageHelper.getUriAllItems(mGameKey), projection, null, null, getSortOrder());
			return cursorLoader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			Cursor oldCursor = null;
			try {
				adapter.swapCursor(data);
				mGameOverviewCallback.onGamesLoaded(data.getCount());
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
