package dan.dit.gameMemo.appCore.tichu;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.appCore.GamesOverviewListFragment;
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
public class TichuGamesOverviewListFragment extends GamesOverviewListFragment {
	// alternative sort order string: first unfinished games then finished games, most recent games first for both parts: 
	/* "CASE " + GameStorageHelper.COLUMN_WINNER 
			+ " WHEN " + Game.WINNER_NONE + " THEN " + GameStorageHelper.COLUMN_STARTTIME + " END DESC, "
			+ GameStorageHelper.COLUMN_STARTTIME + " DESC";
			*/
	private static final String TICHU_GAMES_SORT_ORDER = GameStorageHelper.COLUMN_STARTTIME + " DESC";
	
	@Override
	protected String getSortOrder() {
		return TICHU_GAMES_SORT_ORDER;
	}

	@Override
	protected GameOverviewAdapter makeAdapter() {
 		return new TichuGameOverviewAdapter(this.getActivity(), R.layout.tichu_game_overview, null);
	}

	@Override
	protected String[] getProjection() {
		return TichuGameOverviewAdapter.REQUIRED_COLUMNS;
	}

	
}
