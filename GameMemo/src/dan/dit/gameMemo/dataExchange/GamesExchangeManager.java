package dan.dit.gameMemo.dataExchange;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import dan.dit.gameMemo.gameData.game.GameKey;

public class GamesExchangeManager {	
	public static final long EXCHANGE_START_DELAY = 1500; //ms, must be smaller than timeout duration
	public static final long TIMEOUT_DURATION = 5000; //ms
	
	private List<Integer> mSelectedGames;
	private FragmentManager mFragManager;
	private GamesExchangeView mExchangeView;
	private List<GameDataExchanger> mDataExchangers;
	private GamesOverviewDialog mGamesOverviewDialog;
	
	public GamesExchangeManager(FragmentManager fragManager, int[] gamesSuggestions) {
		mDataExchangers = new LinkedList<GameDataExchanger>();
		setSelectedGames(gamesSuggestions);
		initFragManager(fragManager);
	}
	
	public void setGamesExchangeView(GamesExchangeView gamesExchangeView){
		if (gamesExchangeView == null) {
			throw new NullPointerException("GamesExchangeView null.");
		}
		mExchangeView = gamesExchangeView;
		mExchangeView.setOnShowGamesClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mGamesOverviewDialog = new GamesOverviewDialog();
				mGamesOverviewDialog.show(mFragManager, "GamesOverviewDialogFragment");
			}
			
		});
		mExchangeView.setSelectedGamesCount(mSelectedGames.size(), GameKey.ALL_GAMES.length);
	}
	
	public void setSelectedGames(int[] gamesSuggestions) {
		mSelectedGames = new LinkedList<Integer>();
		if (gamesSuggestions == null || gamesSuggestions.length == 0) {
			for (int key : GameKey.ALL_GAMES) {
				mSelectedGames.add(key);
			}
		} else {
			for (int key : gamesSuggestions) {
				mSelectedGames.add(key);
			}
		}
		if (mExchangeView != null) {
			mExchangeView.setSelectedGamesCount(mSelectedGames.size(), GameKey.ALL_GAMES.length);
		}
	}
	
	public int[] getSelectedGames() {
		int[] keys = new int[mSelectedGames.size()];
		int index = 0;
		for (int key : mSelectedGames) {
			keys[index++] = key;
		}
		return keys;
	}
	
	private void initFragManager(FragmentManager fragManager) {
		if (fragManager == null) {
			throw new NullPointerException("Fragment manager null.");
		}
		mFragManager = fragManager;

	}
	
	public View getView() {
		return mExchangeView;
	}

	public void startExchange(ExchangeService mExchangeService, ContentResolver resolver) {
	   	 mDataExchangers.clear();
		 if (mGamesOverviewDialog != null) {
		 	mGamesOverviewDialog.notifyDataSetChanged();
		 }
	     for (int gameKey : mSelectedGames) {
	     	GameDataExchanger exchanger = new GameDataExchanger(resolver, mExchangeService, gameKey);
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

	public boolean isSelected(int gameKey) {
		return mSelectedGames.contains(Integer.valueOf(gameKey));
	}
	
}
