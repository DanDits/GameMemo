package dan.dit.gameMemo.appCore.custom;
import android.text.TextUtils;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.appCore.GamesOverviewListFragment;
import dan.dit.gameMemo.storage.GameStorageHelper;


public class CustomGamesOverviewListFragment extends
		GamesOverviewListFragment {
	public static final String GAMES_SORT_ORDER = GameStorageHelper.COLUMN_STARTTIME + " DESC";
	private String mFilterGameName;
	
	@Override
	protected GameOverviewAdapter makeAdapter() { 		
	    CustomOverviewAdapter adapter = new CustomOverviewAdapter(this.getActivity(), R.layout.custom_game_overview, null);
	    getListView().setTextFilterEnabled(true);
	    return adapter;
	}
	
	public void setFilterGameName(String filterGameName) {
	    ((CustomOverviewAdapter) adapter).setFilterGameName(filterGameName);
	    if (TextUtils.isEmpty(filterGameName)) {
	        mFilterGameName = null;
	    } else {
	        mFilterGameName = filterGameName;
	    }
	}

	@Override
	protected String getSortOrder() {
		return GAMES_SORT_ORDER;
	}

	@Override
	protected String[] getProjection() {
		return CustomOverviewAdapter.REQUIRED_COLUMNS;
	}
	
	@Override
	protected String getSelection() {
	    if (mFilterGameName == null) {
	        return null;
	    }
	    return GameStorageHelper.COLUMN_METADATA + " like ?";
	}
	
	@Override
	protected String[] getSelectionArgs() {
	    if (mFilterGameName == null) {
	        return null;
	    }
	    // this potentially allows games that only have the game name in their description to be visible too, we call it a feature
        return new String[] {'%' + mFilterGameName.toString() + '%'};
	}

}
