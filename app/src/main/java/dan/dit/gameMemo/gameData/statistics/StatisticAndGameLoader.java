package dan.dit.gameMemo.gameData.statistics;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class StatisticAndGameLoader extends
		AsyncTask<Uri, Integer, Void> {
	private Context mApplicationContext;
	private List<LoadingListener> mListeners;
	private final int mGameKey;
	private List<Game> mGames;
	private Set<Long> mAllowedStartTimes;
	
	public static interface LoadingListener {
	    void loadingProgress(int progress, int gameKey);
	    void loadingComplete(List<Game> games, int gameKey);
	}
	
	/**
	 * Creates a statistic and game loader that will load all games sorted by starttime for the given game.
	 * @param applicationContext A context to access the databases.
	 * @param gameKey The gamekey that identifies the game to load games and statistics for.
	 * @param allowedStartTimes If <code>null</code> all games will be used. Else it will
	 * remove all games that have a start time that is not contained in this set of start times.
	 */
	public StatisticAndGameLoader(Context applicationContext, int gameKey, Set<Long> allowedStartTimes) {
		this.mApplicationContext = applicationContext;
		this.mListeners = new LinkedList<LoadingListener>();
		this.mGameKey = gameKey;
		mAllowedStartTimes = allowedStartTimes;
		if (applicationContext == null) {
			throw new IllegalArgumentException("No parameter must be null.");
		} else if (!GameKey.isGameSupported(gameKey)) {
			throw new IllegalArgumentException("Gamekey " + gameKey + " not supported.");
		}
	}

	public void addListener(LoadingListener listener) {
		if (listener != null) {
			mListeners.add(listener);
		}
	}
	
	public boolean removeListener(LoadingListener listener) {
		return mListeners.remove(listener);
	}
	
	@Override
	protected Void doInBackground(Uri... uris) {
        loadStatistics();
        publishProgress(15);
	    loadGames(uris, 15, 80);
        publishProgress(80);
        if (checkAbort()) {
            return null;
        }
        filterGames();
        publishProgress(90);
        if (checkAbort()) {
            return null;
        }
		sortGames();
        publishProgress(100);
		return null;
	}
	
	private boolean checkAbort() {
        if (mGames == null || mGames.size() == 0 || isCancelled()) {
            mGames = null;
            return true;
        }
        return false;
	}
	
	private void filterGames() {
	    if (mAllowedStartTimes == null) {
	        return; // no effect
	    }
	    if (mAllowedStartTimes.size() == 0) {
	        mGames = null; // filtered all out..
	        return;
	    }
	    Iterator<Game> it = mGames.iterator();
	    while (it.hasNext()) {
	        Game g = it.next();
	        if (!mAllowedStartTimes.contains(Long.valueOf(g.getStartTime()))) {
	            it.remove();
	        }
	    }
	}
	
	private void sortGames() {
	    Collections.sort(mGames, new Comparator<Game>() {

            @Override
            public int compare(Game lhs, Game rhs) {
                if (lhs.getStartTime() < rhs.getStartTime()) {
                    return -1;
                } else if (lhs.getStartTime() == rhs.getStartTime()) {
                    return 0;
                } else {
                    return 1;
                }
            }
	        
        });
	}
	
	private void loadGames(Uri[] uris, int startProgress, int endProgress) {
        mGames = new LinkedList<Game>();
        final int LOADING_PROGRESS = endProgress - startProgress;
        final int progressPerUri = LOADING_PROGRESS / uris.length;
        int progress = 0;
        for (Uri u : uris) {
            List<Game> games = null;
            try {
                games = Game.loadGames(mGameKey, mApplicationContext.getContentResolver(), u, false);
            } catch(CompactedDataCorruptException e) {
            }
            if (games != null) {
                mGames.addAll(games);
            }
            progress += progressPerUri;
            publishProgress(startProgress + progress);
            if (isCancelled()) {
                break;
            }
        }
	}
	
	private void loadStatistics() {
	    GameStatisticAttributeManager manager = GameKey.getGameStatisticAttributeManager(mGameKey);
	    if (!manager.isInitialized()) {
	        manager.init(mApplicationContext);
	    }
	}
	
	@Override
	public void onProgressUpdate(Integer...integers ) {
	    if (integers.length > 0) {
            for (LoadingListener listener : mListeners) {
                listener.loadingProgress(integers[0], mGameKey);
            }
	    }
	}
	
	@Override
    protected void onPostExecute(Void nothing) {
		for (LoadingListener listener : mListeners) {
			listener.loadingComplete(mGames, mGameKey);
		}
    }
	
}
