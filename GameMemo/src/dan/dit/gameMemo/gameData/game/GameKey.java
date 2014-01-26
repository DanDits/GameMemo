package dan.dit.gameMemo.gameData.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameDetailActivity;
import dan.dit.gameMemo.appCore.GameDetailFragment;
import dan.dit.gameMemo.appCore.doppelkopf.DoppelkopfGameDetailActivity;
import dan.dit.gameMemo.appCore.doppelkopf.DoppelkopfGameDetailFragment;
import dan.dit.gameMemo.appCore.doppelkopf.DoppelkopfGamesActivity;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupNoOptions;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupOptionsController;
import dan.dit.gameMemo.appCore.minigolf.MinigolfGameDetailFragment;
import dan.dit.gameMemo.appCore.minigolf.MinigolfGamesActivity;
import dan.dit.gameMemo.appCore.tichu.TichuGameDetailFragment;
import dan.dit.gameMemo.appCore.tichu.TichuGamesActivity;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGameBuilder;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfGame;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfGameBuilder;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfGameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.game.tichu.TichuGameBuilder;
import dan.dit.gameMemo.gameData.game.tichu.TichuGameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.game.user.UserGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.storage.database.CardGameTable;
import dan.dit.gameMemo.storage.database.GamesDBContentProvider;
import dan.dit.gameMemo.storage.database.SportGameTable;
import dan.dit.gameMemo.util.ActivityUtil;

/**
 * This helper class stores all game keys for classes that need to separate
 * subclasses of {@link Game} to distinguish between different static methods or constants.
 * @author Daniel
 *
 */
/*
 * Note when adding a new game:
 * 1) Create the model classes, especially a Game and a GameRound subclass implementing the game rules and holding all data
 * that is required for essential and bonus information
 * 2) Add a new game key constant, add it to the supported game keys and at all switches where adequate, add a case for the new game key
 * and enter the constant, method or class required for the new game
 * 3) Implement a layout view and controller activites that (in a neat way) enable the user to visualize and enter data for games, the basic
 * framework can be used by subclassing most controllers
 */
public final class GameKey {
    /**
     * The extra key used when an intent stores a single or multiple game keys.
     */
	public static final String EXTRA_GAMEKEY = "dan.dit.gameMemo.EXTRA_GAMEKEY";
	
	/**
	 * The constant for an invalid gamekey used to signal that a game is not supported or
	 * not found.
	 */
	public static int NO_GAME = 0; // game that is not supported to mark an invalid gamekey
	
	/**
	 * The constant for the {@link TichuGame} class.
	 */
	public static final int TICHU = 1;
	
	/**
	 * The constant for the {@link DoppelkopfGame} class.
	 */
	public static final int DOPPELKOPF = 2;
	
	/**
	 * The constant for the {@link MinigolfGame} class.
	 */
	public static final int MINIGOLF = 3;
	
	/**
	 * The constant for the {@link UserGame} class.
	 */
	public static final int USERGAME = 4;
	
	/**
	 * Contains all game keys that will appear in the game chooser activity.
	 */
	public static final int[] ALL_GAMES = new int[] {TICHU, DOPPELKOPF, MINIGOLF, USERGAME};
	
	/**
	 * Private to avoid instantiation of this utility class.
	 */
	private GameKey() {}
	
	/**
	 * Helper method to convert an array of integers to a List<Integer>.
	 * @param array The array to convert.
	 * @return <code>null</code> if array is <code>null</code>, else a list of
	 * size equal to array length with the array data.
	 */
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
	
	/**
     * Helper method to convert an array of longs to a List<Long>.
     * @param array The array to convert.
     * @return <code>null</code> if array is <code>null</code>, else a list of
     * size equal to array length with the array data.
     */
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
	
    /**
       * Helper method to convert a collection of longs to a long array.
       * @param data The collection to convert.
       * @return <code>null</code> if array is <code>null</code>, else an array of
       * length equal to data size with the data of the collection.
       */
	public static long[] toArray(Collection<Long> data) {
		if (data == null) {
			return null;
		}
		long[] array = new long[data.size()];
		int index = 0;
		for (Long m : data) {
			array[index++] = m;
		}
		return array;
	}
	
	/**
     * Helper method to convert an array of ints to a Integer[].
     * @param data The array to convert.
     * @return <code>null</code> if array is <code>null</code>, else an Integer array of
     * size equal to array length with the array data.
     */
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
	
    /**
     * Returns a set of all players of all pools. The pools should therefore
     * be initialized before invoking this method.
     * @return A set of all players that ever played any game that is saved on this app.
     */
	public static Set<Player> getAllPlayers() {
		Set<Player> allPlayers = new TreeSet<Player>();
		for (int key : ALL_GAMES) {
			allPlayers.addAll(getPool(key).getAll());
		}
		return allPlayers;
	}
	
	/**
	 * Returns the id of the game icon for the given game key. 
	 * Used for the action bar and various other locations to represent
	 * the game.
	 * @param gameKey The game key.
	 * @return A resource id of the icon to use for the game. 
	 */
	public static int getGameIconId(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return dan.dit.gameMemo.R.drawable.tichu_phoenix;
		case GameKey.DOPPELKOPF:
			return dan.dit.gameMemo.R.drawable.doppelkopf_icon;
		case GameKey.MINIGOLF:
		    return dan.dit.gameMemo.R.drawable.minigolf_icon;
		case GameKey.USERGAME:
		    return dan.dit.gameMemo.R.drawable.user_game_icon;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	/**
	 * Returns the game name as a CharSequence of the given game key.
	 * Used in the action bar, in share and synch selection and other places
	 * to represent the game.
	 * @param gameKey The game key.
	 * @param res Resource object of the context to load localized data. Should not be <code>null</code>.
	 * @return The name of the game, if possible and required in a localized form.
	 */
	public static CharSequence getGameName(int gameKey, Resources res) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.GAME_NAME;
		case GameKey.DOPPELKOPF:
			return DoppelkopfGame.GAME_NAME;
		case MINIGOLF:
		    return res == null ? MinigolfGame.GAME_NAME : res.getString(R.string.minigolf_game_name);
		case USERGAME:
		    return res == null ? UserGame.NAME : res.getString(R.string.user_game_name);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	/**
     * Returns the game name as a String of the given game key.
     * Used in the action bar, in share and synch selection and other places
     * to represent the game.
     * @param gameKey The game key.
     * @param res Resource object of the context to load localized data. Should not be <code>null</code>.
     * @return The name of the game, if possible and required in a localized form.
     */
	public static String getGameNameString(int gameKey, Resources res) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.GAME_NAME;
		case GameKey.DOPPELKOPF:
			return DoppelkopfGame.GAME_NAME;
        case MINIGOLF:
            return res == null ? MinigolfGame.GAME_NAME : res.getString(R.string.minigolf_game_name);
        case USERGAME:
            return res == null ? UserGame.NAME : res.getString(R.string.user_game_name);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	/**
	 * Returns the player pool for the given game key that stores
	 * all players that ever played the game and have it stored on this app.
	 * Can be used to obtain new players.
	 * @param gameKey The game key.
	 * @return The PlayerPool for the game.
	 */
	public static PlayerPool getPool(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.PLAYERS;
		case GameKey.DOPPELKOPF:
			return DoppelkopfGame.PLAYERS;
		case GameKey.MINIGOLF:
		    return MinigolfGame.PLAYERS;
		case GameKey.USERGAME:
		    return UserGame.PLAYERS;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	/**
	 * Returns a GameBuilder to build a game instance from memory. This is used
	 * when reconstructing games loaded from a database or from a String that was received on any
	 * other way.
	 * @param gameKey The game key.
	 * @return A GameBuilder to rebuild a game.
	 */
	public static GameBuilder getBuilder(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return new TichuGameBuilder();
		case DOPPELKOPF:
			return new DoppelkopfGameBuilder();
		case MINIGOLF:
		    return new MinigolfGameBuilder();
		case USERGAME:
		    //TODO
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);			
		}
	}

	private static final String PREFERENCES_STAY_AWAKE = "dan.dit.gameMemo.STAY_AWAKE_";
	
	/**
	 * Applies screen lock to the activites window to keep the screen on if stayAwake() is <code>true</code>.
	 * By default this is not the case to save battery life.
	 * @param gameKey The game key.
	 * @param act The activity to apply the sleep behavior to.
	 */
	public static void applySleepBehavior(final int gameKey, final Activity act) {
		if (getStayAwake(gameKey, act)) {
			act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	/**
	 * Sets the desired sleep behavior for the given game. Warning! Setting this to true will
	 * drain the battery very fast when a game view is open!
	 * @param gameKey The game key.
	 * @param act The activity to apply the sleep behavior to.
	 * @param stayAwake If <code>true</code> the screen will not go off for a game view of the given game key.
	 */
	public static void setStayAwake(int gameKey, Activity act, boolean stayAwake) {
		SharedPreferences.Editor editor = act.getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();
		editor.putBoolean(PREFERENCES_STAY_AWAKE + gameKey, stayAwake);
        ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
		applySleepBehavior(gameKey, act);
	}
	
	/**
	 * Returns the desired sleep behavior for the given game.
	 * @param gameKey The game key.
	 * @param context A context for the preferences. 
	 * @return If the screen should stay on for the given game.
	 */
	public static boolean getStayAwake(int gameKey, Context context) {
		SharedPreferences sharedPref = context.getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE);
		boolean defaultIsAwake = false;
		//Note: add all game keys that have default awake here and set var to true
		
		return sharedPref.getBoolean(PREFERENCES_STAY_AWAKE + gameKey, defaultIsAwake);
	}
	
	/**
	 * Returns <code>true</code> if the given game key is supported by this GameKey class.
	 * This means that ALL_GAMES contains the given key and that all methods will not throw an exception when
	 * being invoked for the given game key.
	 * @param gameKey The game key.
	 * @return If the game is supported.
	 */
	public static final boolean isGameSupported(int gameKey) {
	    // currently supports all games listed in ALL_GAMES, can change
		for (int key : ALL_GAMES) { // add all supported game keys for the static methods that take a gamekey argument and do not throw on usage
		    if (gameKey == key) {
		        return true;
		    }
		}
		return false;
	}

	public static int getBackgroundResource(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.drawable.tichu_color;
		case GameKey.DOPPELKOPF:
			return R.drawable.doppelkopf_color;
		case MINIGOLF:
		    return R.drawable.minigolf_color;
		case USERGAME:
		    return R.drawable.usergame_color;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}
	
	public static void applyTheme(int gameKey, Resources res, View view) {
	    view.setBackgroundResource(getButtonResource(gameKey));
	    if (view instanceof TextView) {
	        ((TextView) view).setTextColor(res.getColorStateList(getButtonTextColorResource(gameKey)));
	    }
	}
	
	private static int getButtonTextColorResource(int gameKey) {
        switch (gameKey) {
        case GameKey.TICHU:
            return R.color.tichu_text_color;
        case GameKey.DOPPELKOPF:
            return R.color.doppelkopf_text_color;
        case MINIGOLF:
            return R.color.minigolf_text_color;
        case USERGAME:
            return R.color.usergame_text_color;
        default:
            throw new IllegalArgumentException("Game not supported: " + gameKey);               
        }
	}
	
	private static int getButtonResource(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.drawable.tichu_button;
		case GameKey.DOPPELKOPF:
			return R.drawable.doppelkopf_button;
		case MINIGOLF:
		    return R.drawable.minigolf_button;
		case USERGAME:
		    return R.drawable.usergame_button;
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
		case MINIGOLF:
		    return R.drawable.minigolf_list_selector;
		case USERGAME:
		    return R.drawable.usergame_list_selector;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static void sortByName(List<Integer> mAllGames, final Resources res) {
		Collections.sort(mAllGames, new Comparator<Integer>() {

			@Override
			public int compare(Integer key1, Integer key2) {
				return getGameNameString(key1, res).compareTo(getGameNameString(key2, res));
			}
			
		});
	}

	public static int getGamesMainLayout(int gameKey) {
		switch (gameKey) {
		case GameKey.TICHU:
			return R.layout.tichu_main;
		case GameKey.DOPPELKOPF:
			return R.layout.doppelkopf_main;
		case MINIGOLF:
		    return R.layout.minigolf_main;
		case USERGAME:
		    //TODO
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

	public static int getGameDetailLayout(int gameKey) {
		switch (gameKey) {
		default:
			return R.layout.generic_detail_fragment;			
		}
	}

	public static Class<?> getGameDetailActivity(int gameKey) {
		switch (gameKey) {
		case DOPPELKOPF:
		    return DoppelkopfGameDetailActivity.class;
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
		case MINIGOLF:
		    return MinigolfGamesActivity.class;
		case USERGAME:
		    //TODO
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
		case MINIGOLF:
		    return R.layout.minigolf_list;
		case USERGAME:
		    //TODO
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
		case MINIGOLF:
		    return R.menu.minigolf_list;
		case USERGAME:
		    //TODO
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
		case MINIGOLF:
		    return MinigolfGameDetailFragment.newInstance(gameId);
		case USERGAME:
		    //TODO
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
	     case MINIGOLF:
	        return MinigolfGameDetailFragment.newInstance(extras);
	        case USERGAME:
	            //TODO
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}

    public static GameSetupOptionsController makeGameSetupOptionsController(
            int gameKey, Context context, ViewGroup container, Bundle parameters) {
        switch (gameKey) {
        case GameKey.TICHU:
            return new dan.dit.gameMemo.appCore.tichu.GameSetupOptions(context, container, parameters);
        case DOPPELKOPF: 
            return new dan.dit.gameMemo.appCore.doppelkopf.GameSetupOptions(context, container, parameters);
        case MINIGOLF:
            return new dan.dit.gameMemo.appCore.minigolf.GameSetupOptions(context, container, parameters);
        case USERGAME:
            //TODO
        default:
            return GameSetupNoOptions.INSTANCE;               
        }
    }

	public static String getStorageTableName(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
		case GameKey.DOPPELKOPF:
        case USERGAME:
			return CardGameTable.TABLE_CARD_GAMES;
		case MINIGOLF:
		    return SportGameTable.TABLE_SPORT_GAMES;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}

	public static Collection<String> getStorageAvailableColumns(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
		case GameKey.DOPPELKOPF:
		case USERGAME:
			return CardGameTable.AVAILABLE_COLUMNS_COLL;
		case MINIGOLF:
		    return SportGameTable.AVAILABLE_COLUMNS_COLL;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}
	
    public static String[] getStorageAvailableColumnsProj(int gameKey) {
        switch(gameKey) {
        case GameKey.TICHU:
        case GameKey.DOPPELKOPF:
        case USERGAME:
            return CardGameTable.AVAILABLE_COLUMNS;
        case MINIGOLF:
            return SportGameTable.AVAILABLE_COLUMNS;
        default:
            throw new IllegalArgumentException("Game not supported: " + gameKey);
        }
    }
    
	public static Uri getStorageUri(int gameKey, int uriType) {
		switch(gameKey) {
		case GameKey.TICHU:
		case GameKey.DOPPELKOPF:
		case MINIGOLF:
		case USERGAME:
			return GamesDBContentProvider.makeUri(gameKey, uriType);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}

    public static GameStatisticAttributeManager getGameStatisticAttributeManager(
            int gameKey) {
        switch (gameKey) {
        case TICHU:
            return TichuGameStatisticAttributeManager.INSTANCE;
        case DOPPELKOPF:
            return DoppelkopfGameStatisticAttributeManager.INSTANCE;
        case MINIGOLF:
            return MinigolfGameStatisticAttributeManager.INSTANCE;
        case USERGAME:
            //TODO
        default:
            throw new IllegalArgumentException("Game not supported: " + gameKey);
        }
    }
}
