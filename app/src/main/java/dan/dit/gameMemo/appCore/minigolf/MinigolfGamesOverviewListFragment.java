package dan.dit.gameMemo.appCore.minigolf;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.appCore.GamesOverviewListFragment;
import dan.dit.gameMemo.storage.GameStorageHelper;


public class MinigolfGamesOverviewListFragment extends
		GamesOverviewListFragment {
	private static final String GAMES_SORT_ORDER = GameStorageHelper.COLUMN_STARTTIME + " DESC";

	@Override
	protected GameOverviewAdapter makeAdapter() { 		
 		return new MinigolfOverviewAdapter(this.getActivity(), R.layout.minigolf_game_overview, null);
	}

	@Override
	protected String getSortOrder() {
		return GAMES_SORT_ORDER;
	}

	@Override
	protected String[] getProjection() {
		return MinigolfOverviewAdapter.REQUIRED_COLUMNS;
	}

}
