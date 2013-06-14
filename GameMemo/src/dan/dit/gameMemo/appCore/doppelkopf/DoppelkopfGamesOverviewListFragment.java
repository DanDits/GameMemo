package dan.dit.gameMemo.appCore.doppelkopf;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.appCore.GamesOverviewListFragment;
import dan.dit.gameMemo.storage.GameStorageHelper;


public class DoppelkopfGamesOverviewListFragment extends
		GamesOverviewListFragment {
	private static final String DOPPELKOPF_GAMES_SORT_ORDER = GameStorageHelper.COLUMN_STARTTIME + " DESC";

	@Override
	protected GameOverviewAdapter makeAdapter() { 		
 		return new DoppelkopfOverviewAdapter(this.getActivity(), R.layout.doppelkopf_game_overview, null);
	}

	@Override
	protected String getSortOrder() {
		return DOPPELKOPF_GAMES_SORT_ORDER;
	}

	@Override
	protected String[] getProjection() {
		return DoppelkopfOverviewAdapter.REQUIRED_COLUMNS;
	}

}
