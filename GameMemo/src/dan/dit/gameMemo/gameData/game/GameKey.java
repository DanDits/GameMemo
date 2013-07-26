package dan.dit.gameMemo.gameData.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameDetailActivity;
import dan.dit.gameMemo.appCore.GameDetailFragment;
import dan.dit.gameMemo.appCore.doppelkopf.DoppelkopfGameDetailFragment;
import dan.dit.gameMemo.appCore.doppelkopf.DoppelkopfGamesActivity;
import dan.dit.gameMemo.appCore.tichu.TichuGameDetailFragment;
import dan.dit.gameMemo.appCore.tichu.TichuGamesActivity;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGameBuilder;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.game.tichu.TichuGameBuilder;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.statistics.GameStatisticBuilder;
import dan.dit.gameMemo.gameData.statistics.tichu.TichuGameStatisticBuilder;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.CardGameTable;
import dan.dit.gameMemo.storage.database.GamesDBContentProvider;
import dan.dit.gameMemo.util.ActivityUtil;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

/**
 * This helper class stores all game keys for classes that need to separate
 * subclasses of {@link Game} to distinguish between different static methods or constants.<br>
 * Just because a constant is listed here does not necessarily mean that the class supports this game.
 * @author Daniel
 *
 */
public final class GameKey {
	public static final String EXTRA_GAMEKEY = "dan.dit.gameMemo.EXTRA_GAMEKEY";
	
	public static int NO_GAME = 0; // game that is not supported to mark an invalid gamekey
	
	/**
	 * The constant for the {@link TichuGame} class.
	 */
	public static final int TICHU = 1;
	
	public static final int DOPPELKOPF = 2;
	
	public static final int[] ALL_GAMES = new int[] {TICHU, DOPPELKOPF};
	
	private GameKey() {}
	
	public static List<Integer> toList(int[] array) {
		if (array == null) {
			return null;
		}
		ArrayList<Integer> list = new ArrayList<Integer>(array.length);
		for (int data : array) {
			list.add(data);
		}
		return list;
	}
	
	public static List<Long> toList(long[] array) {
		if (array == null) {
			return null;
		}
		ArrayList<Long> list = new ArrayList<Long>(array.length);
		for (long data : array) {
			list.add(data);
		}
		return list;
	}
	
	public static long[] toArray(Collection<Long> data) {
		if (data == null) {
			return null;
		}
		long[] array = new long[data.size()];
		int index = 0;
		for (long m : data) {
			array[index++] = m;
		}
		return array;
	}
	
    public static Integer[] toIntegerArray(int[] data) {
        if (data == null) {
            return null;
        }
        Integer[] array = new Integer[data.length];
        int index = 0;
        for (int m : data) {
            array[index++] = m;
        }
        return array;
    }
	
	public static Set<Player> getAllPlayers() {
		Set<Player> allPlayers = new TreeSet<Player>();
		for (int key : ALL_GAMES) {
			allPlayers.addAll(getPool(key).getAll());
		}
		return allPlayers;
	}
	
	public static GameStatisticBuilder getStatisticBuilder(int gameKey, List<Player> players) {
		switch(gameKey) {
		case GameKey.TICHU:
			return new TichuGameStatisticBuilder(players);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static int getGameIconId(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return dan.dit.gameMemo.R.drawable.tichu_phoenix;
		case GameKey.DOPPELKOPF:
			return dan.dit.gameMemo.R.drawable.doppelkopf_icon;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static CharSequence getGameName(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.GAME_NAME;
		case GameKey.DOPPELKOPF:
			return DoppelkopfGame.GAME_NAME;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static String getGameNameString(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.GAME_NAME;
		case GameKey.DOPPELKOPF:
			return DoppelkopfGame.GAME_NAME;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static PlayerPool getPool(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.PLAYERS;
		case GameKey.DOPPELKOPF:
			return DoppelkopfGame.PLAYERS;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static GameBuilder getBuilder(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return new TichuGameBuilder();
		case DOPPELKOPF:
			return new DoppelkopfGameBuilder();
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);			
		}
	}
	
	public static List<Game> loadGames(int gameKey, ContentResolver resolver, List<Long> timestamps) {
		List<Game> gamesToSend = new ArrayList<Game>(timestamps.size());
		switch (gameKey) {
		case GameKey.TICHU:
			try {
				gamesToSend = TichuGame.loadGames(resolver, GameStorageHelper.getUriAllItems(gameKey), timestamps, false);
			} catch (CompactedDataCorruptException e) {
				assert false; // load games will not throw as requested by the parameter
			}
			break;
		case DOPPELKOPF:
			try {
				gamesToSend = DoppelkopfGame.loadGames(resolver, GameStorageHelper.getUriAllItems(gameKey), timestamps, false);
			} catch (CompactedDataCorruptException e) {
				assert false; // load games will not throw as requested by the parameter
			}
			break;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
		return gamesToSend;
	}
	
	public static List<Game> loadGames(int gameKey, ContentResolver resolver, Uri uri) throws CompactedDataCorruptException {
		switch (gameKey) {
		case GameKey.TICHU:
			return TichuGame.loadGames(resolver, uri, true);
		case DOPPELKOPF:
			return DoppelkopfGame.loadGames(resolver, uri, true);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}
	

	private static final String PREFERENCES_STAY_AWAKE = "dan.dit.gameMemo.STAY_AWAKE_";
	
	public static void applySleepBehavior(final int gameKey, final Activity act) {
	    new Thread(new Runnable() {
	        @Override
	        public void run() {
        		if (getStayAwake(gameKey, act)) {
        			act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        		} else {
        			act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        		}
	        }
	    }).start();
	}
	
	public static void setStayAwake(int gameKey, Activity act, boolean stayAwake) {
		SharedPreferences.Editor editor = act.getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();
		editor.putBoolean(PREFERENCES_STAY_AWAKE + gameKey, stayAwake);
        ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
		applySleepBehavior(gameKey, act);
	}
	
	public static boolean getStayAwake(int gameKey, Activity act) {
		SharedPreferences sharedPref = act.getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE);
		boolean defaultIsAwake = false;
		//add all game keys that have default awake here and set var to true
		
		return sharedPref.getBoolean(PREFERENCES_STAY_AWAKE + gameKey, defaultIsAwake);
	}
	
	public static final boolean isGameSupported(int gameKey) {
		return gameKey == GameKey.TICHU || gameKey == DOPPELKOPF; // add all supported game keys for the static methods that take a gamekey argument and do not throw on usage
	}

	public static int getBackgroundResource(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.drawable.tichu_color;
		case GameKey.DOPPELKOPF:
			return R.drawable.doppelkopf_color;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}
	
	public static int getButtonResource(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.drawable.tichu_button;
		case GameKey.DOPPELKOPF:
			return R.drawable.doppelkopf_button;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}
	
	public static int getSelectorResource(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.drawable.tichu_list_selector;
		case GameKey.DOPPELKOPF:
			return R.drawable.doppelkopf_list_selector;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static void sortByName(List<Integer> mAllGames) {
		Collections.sort(mAllGames, new Comparator<Integer>() {

			@Override
			public int compare(Integer key1, Integer key2) {
				return getGameNameString(key1).compareTo(getGameNameString(key2));
			}
			
		});
	}

	public static int getGamesMainLayout(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.layout.tichu_main;
		case GameKey.DOPPELKOPF:
			return R.layout.doppelkopf_main;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static int getGameDetailLayout(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.layout.tichu_detail_fragment;
		case DOPPELKOPF:
			return R.layout.doppelkopf_detail_fragment;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static Class<?> getGameDetailActivity(int gameKey) {
		switch (gameKey) {
		default:
			return GameDetailActivity.class;			
		}
	}
	
	public static Class<?> getGamesActivity(int gameKey) {
		switch (gameKey) {
		case TICHU:
			return TichuGamesActivity.class;
		case DOPPELKOPF:
			return DoppelkopfGamesActivity.class;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}

	public static int getGameOverviewListLayout(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.layout.tichu_list;
		case GameKey.DOPPELKOPF:
			return R.layout.doppelkopf_list;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static int getGamesMenuLayout(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.menu.tichu_list;
		case GameKey.DOPPELKOPF:
			return R.menu.doppelkopf_list;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static GameDetailFragment getNewGameDetailFragmentInstance(
			int gameKey, long gameId) {
		switch (gameKey) {
		case GameKey.TICHU:
			return TichuGameDetailFragment.newInstance(gameId);
		case DOPPELKOPF:
			return DoppelkopfGameDetailFragment.newInstance(gameId);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}
	
	public static GameDetailFragment getNewGameDetailFragmentInstance(
			int gameKey, Bundle extras) {
		switch (gameKey) {
		case GameKey.TICHU:
			return TichuGameDetailFragment.newInstance(extras);
		case DOPPELKOPF:
			return DoppelkopfGameDetailFragment.newInstance(extras);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static String getStorageTableName(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
		case GameKey.DOPPELKOPF:
			return CardGameTable.TABLE_CARD_GAMES;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}

	public static Collection<String> getStorageAvailableColumns(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
		case GameKey.DOPPELKOPF:
			return CardGameTable.AVAILABLE_COLUMNS_COLL;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}

	public static Uri getStorageUri(int gameKey, int uriType) {
		switch(gameKey) {
		case GameKey.TICHU:
		case GameKey.DOPPELKOPF:
			return GamesDBContentProvider.makeUri(gameKey, uriType);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}

    public static boolean isGameKey(int gameKey) {
        for (int key : ALL_GAMES) {
            if (key == gameKey) {
                return true;
            }
        }
        return false;
    }

}
