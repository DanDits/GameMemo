package dan.dit.gameMemo.appCore.binokel;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.appCore.GamesOverviewListFragment;
import dan.dit.gameMemo.appCore.tichu.TichuGameOverviewAdapter;
import dan.dit.gameMemo.storage.GameStorageHelper;

public class BinokelGamesOverviewListFragment extends GamesOverviewListFragment {

	private static final String BINOKEL_GAMES_SORT_ORDER = GameStorageHelper.COLUMN_STARTTIME + "" +
			" DESC";
	
	@Override
	protected String getSortOrder() {
		return BINOKEL_GAMES_SORT_ORDER;
	}

	@Override
	protected GameOverviewAdapter makeAdapter() {
 		return new BinokelGameOverviewAdapter(this.getActivity(), R.layout.binokel_game_overview, null);
	}

	@Override
	protected String[] getProjection() {
		return BinokelGameOverviewAdapter.REQUIRED_COLUMNS;
	}

	
}
