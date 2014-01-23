package dan.dit.gameMemo.dataExchange;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import dan.dit.gameMemo.gameData.game.GameKey;
/**
 * Manages multiple {@link GameDataExchanger}s, a {@link GamesExchangeView}
 * and interacts with a {@link GamesOverviewDialog} to select a subset of
 * all available games.<br>
 * This class is mainly responsible for managing the visualization of the exchange progress
 * and starting the exchange for the user selected games.
 * @author Daniel
 *
 */
public class GamesExchangeManager {	
	public static final long EXCHANGE_START_DELAY = 1500; //ms, must be smaller than timeout duration
	public static final long TIMEOUT_DURATION = 5000; //ms
	
	private List<Integer> mAllGames;
	private List<Integer> mSelectedGames;
	private FragmentManager mFragManager;
	private GamesExchangeView mExchangeView;
	private List<GameDataExchanger> mDataExchangers;
	private GamesOverviewDialog mGamesOverviewDialog;
	private SparseArray<Collection<Long>> mGameOffers;
	public GamesExchangeManager(FragmentManager fragManager, int[] gamesSuggestions, int[] allGames, Resources res) {
		mDataExchangers = new LinkedList<GameDataExchanger>(); 
		setAllGames(allGames == null ? null : GameKey.toList(allGames), res);
		mGameOffers = new SparseArray<Collection<Long>>(5);
		setSelectedGames(gamesSuggestions);
		initFragManager(fragManager);
	}
	
	public void setGamesExchangeView(GamesExchangeView gamesExchangeView){
		if (gamesExchangeView == null) {
			throw new IllegalArgumentException("GamesExchangeView null.");
		}
		mExchangeView = gamesExchangeView;
		mExchangeView.setOnShowGamesClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mGamesOverviewDialog = new GamesOverviewDialog();
				mGamesOverviewDialog.show(mFragManager, "GamesOverviewDialogFragment");
			}
			
		});
		mExchangeView.setSelectedGames(mSelectedGames);
	}
	
	public void setSelectedGames(int[] gamesSuggestions) {
		mSelectedGames = new LinkedList<Integer>();
		if (gamesSuggestions == null || gamesSuggestions.length == 0) {
			mSelectedGames.addAll(mAllGames);
		} else {
			for (int key : gamesSuggestions) {
				if (mAllGames.contains(Integer.valueOf(key))) {
					mSelectedGames.add(key);
				}
			}
			if (mSelectedGames.size() == 0) { // only got games that were not contained in all games
				mSelectedGames.addAll(mAllGames);
			}
		}
		if (mExchangeView != null) {
			mExchangeView.setSelectedGames(mSelectedGames);
		}
	}
	
	public int[] getSelectedGames() {
	    if (mSelectedGames == null) {
	        return null;
	    }
		int[] keys = new int[mSelectedGames.size()];
		int index = 0;
		for (int key : mSelectedGames) {
			keys[index++] = key;
		}
		return keys;
	}
	
	private void initFragManager(FragmentManager fragManager) {
		if (fragManager == null) {
			throw new IllegalArgumentException("Fragment manager null.");
		}
		mFragManager = fragManager;

	}
	
	public View getView() {
		return mExchangeView;
	}
	
	public void setOffer(int gameKey, Collection<Long> offeredStarttimes) {
		mGameOffers.put(gameKey, offeredStarttimes);
	}

	public void startExchange(ExchangeService mExchangeService, ContentResolver resolver) {
	   	 mDataExchangers.clear();
		 if (mGamesOverviewDialog != null) {
		 	mGamesOverviewDialog.notifyDataSetChanged();
		 }
	     for (int gameKey : mSelectedGames) {
	     	GameDataExchanger exchanger = new GameDataExchanger(resolver, mExchangeService, gameKey);
	     	exchanger.setOffer(mGameOffers.get(gameKey)); // if null then offers all
	     	mDataExchangers.add(exchanger);
	     	exchanger.startExchange(EXCHANGE_START_DELAY);
	     }
	     mExchangeService.startTimeoutTimer(TIMEOUT_DURATION);
	     if (mExchangeView != null) {
			 mExchangeView.setProgress(0);
		     mExchangeView.setMaxProgress(mSelectedGames.size()); 
		     mExchangeView.setProgressIndeterminate(true); //until something actually finishes, we at least show a nice animation to calm down the user
	     }  
	}
	
	public void finishedDataExchanger() {
		if (mExchangeView != null) {
			mExchangeView.setProgressIndeterminate(false);
			mExchangeView.addProgress(1);
		}
		if (mGamesOverviewDialog != null) {
			mGamesOverviewDialog.notifyDataSetChanged();
		}
	}

	public void closeAll() {
		for (GameDataExchanger exchanger : mDataExchangers) {
			exchanger.close();
		}
	}

	public GameDataExchanger getGameDataExchanger(int gameKey) {
		for (GameDataExchanger exchanger : mDataExchangers) {
			if (exchanger.getGameKey() == gameKey) {
				return exchanger;
			}
		}
		return null;
	}

	public int getSuccessfullyExchangedGames() {
		int count = 0;
		for (GameDataExchanger exchanger : mDataExchangers) {
			if (exchanger.exchangeFinishedSuccessfully()) {
				count++;
			}
		}
		return count;
	}
	
	public boolean isSelected(int gameKey) {
		return mSelectedGames.contains(Integer.valueOf(gameKey));
	}

	public List<Integer> getAllGames() {
		return mAllGames;
	}
	
	public void setAllGames(List<Integer> allGames, Resources res) {
        mAllGames = (allGames != null && allGames.size() > 0) ? allGames : GameKey.toList(GameKey.ALL_GAMES);
        GameKey.sortByName(mAllGames, res);
        setSelectedGames(getSelectedGames());
        if (mGamesOverviewDialog != null) {
            mGamesOverviewDialog.notifyDataSetChanged();
        }
	}
	
}
