package dan.dit.gameMemo.gameData.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool.PlayerNameChangeListener;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.Compacter;

public abstract class Game implements Iterable<GameRound>, Compactable, PlayerNameChangeListener {
	public static final String PREFERENCES_FILE = "dan.dit.gameMemo.GAME_PREFERENCES";
	public static final long NO_ID = -1; // isValidId(NO_ID) must be false
	public static final int WINNER_NONE = 0;
	public static final int GENERAL_GAME_ICON = android.R.drawable.ic_menu_mapmode;
	protected long startTime;
	protected long mRunningTime;
	protected long mId;
	protected List<GameRound> rounds;
	protected String originData = "";
	
	public Game() {
		this.mId = NO_ID;
		startTime = new Date().getTime();
		rounds = new ArrayList<GameRound>();
		GameKey.getPool(getKey()).registerListener(this, true);	
		assert !isFinished();
	}
	
	@Override
	protected void finalize() throws Throwable{
		super.finalize();
		GameKey.getPool(getKey()).unregisterListener(this); // not really necessary since PlayerPool uses weak references
	}
	
	public abstract PlayerTeam getWinner();
	public abstract void addRound(GameRound round);
	public final long getStartTime() {
		return startTime;
	}

	public final long getRunningTime() {
		return mRunningTime;
	}
	
	public final void addRunningTime(long time) {
		mRunningTime += time;
	}
	
	public final long getId() {
		return mId;
	}
	
	public final GameRound getGameRound(int index) {
		return rounds.get(index);
	}
	
	public int getRoundCount() {
		return rounds.size();
	}
	
	public GameRound getRound(int index) {
		return rounds.get(index);
	}
	
	/**
	 * Returns a list of all rounds of this game. The list is backed by the game,
	 * changes made to the list will affect the game as well. It is not recommended
	 * to add, delete or change rounds from the list but use the game's method for this.
	 * Though, the game can support a synch() method to re-read rounds data and update its
	 * internal state.
	 * @return A list of all rounds of this game.
	 */
	public List<GameRound> getRounds() {
		return rounds; 
	}
	
	@Override
	public Iterator<GameRound> iterator() {
		return rounds.iterator();
	}

	public abstract void reset(); // must not be supported, if supported then this clears all except player data
	public abstract boolean isFinished();
	protected abstract void setupPlayers(List<Player> players); // if not made public subclasses must provide another setupGame method with other params
	
	/**
	 * Returns all players' names in a compressed that can be decompressed by a {@link Compacter}.
	 * The interpretation of the order and grouping of these players is left to the Game implementation.
	 * @return A compressed form of all players.
	 */
	protected abstract String getPlayerData();
	/**
	 * Returns all rounds in a compressed way that can be decompressed by a {@link Compacter}.
	 * Must be a concatenation of all rounds of the game. How the data for each round is read is up to the game implementation.
	 * This round specific data can be changed, but regard the general hint given at getMetaData().
	 * @return A compressed form of all game rounds.
	 */
	protected abstract String getRoundsData();
	protected abstract int getWinnerData();
	/**
	 * Returns the metadata of this game in a compressed way. In future releases metadata can be added
	 * for a game, but for backwards compatibility this data must never be expected to be received when building
	 * the game and empty data must always be read and written in the same order with no gaps. This also applies
	 * to player data and origin data.
	 * @return
	 */
	protected abstract String getMetaData();
	public abstract void synch();
	public abstract int getKey();
	
	protected String getOriginData() {
		return originData;
	}

	public abstract String getFormattedInfo(Resources res);
	public String getFormattedOrigin() {
		return getOriginData();
	}
	
	public void setOriginData(String bluetoothDeviceName, String model) {
		Compacter cmp = new Compacter(2);
		cmp.appendData(bluetoothDeviceName == null ? "" : bluetoothDeviceName);
		cmp.appendData(model == null ? "" : model);
		originData = cmp.compact();
	}
	
	@Override
	public String compact() {
		Compacter cmp = GameBuilder.getCompressor(this);
		return cmp.compact();
	}

	public boolean updateRound(int index, GameRound updatedRound) { // must not be supported (since it also uses reset)
		if (index < 0 || index >= getRoundCount()) {
			throw new IndexOutOfBoundsException("Bounds 0 (incl.) to " + getRoundCount() + ", got " + index);
		}
		if (updatedRound == null) {
			throw new NullPointerException("GameRound is null");
		} else if (updatedRound.equals(rounds.get(index))) {
			return true;
		}
		List<GameRound> roundsCopy = new ArrayList<GameRound>(rounds);
		reset();
		for (int i = 0; i < index; i++) {
			assert !isFinished();
			addRound(roundsCopy.get(i));
		}
		addRound(updatedRound);
		for (int i = index + 1; i < roundsCopy.size(); i++) {
			if (!isFinished()) {
				addRound(roundsCopy.get(i));
			}
		}
		return true;
	}

	public abstract void saveGame(ContentResolver resolver);
	
	public boolean saveGame(ContentResolver resolver, long id) {
		if (!isValidId(id) || isValidId(mId)) {
			// this is a strict method, it only allows saving for a new id if game does not yet have an ID and given id is valid
			// to prevent accidentally saving a game multiple times under different ids
			return false; 
		}
		mId = id;
		saveGame(resolver);
		return true;
	}
	
	protected void saveGame(ContentValues pValues, ContentResolver resolver) {
		String playerData = getPlayerData();
		String roundsData = getRoundsData();
		String metaData = getMetaData();
		String originData = getOriginData();
		long startTime = getStartTime();
		long runningTime = getRunningTime();
		int winnerData = getWinnerData();

		// Only save new game if there are rounds
		if (roundsData.length() == 0 && !isValidId(mId)) {
			return;
		}

		ContentValues values = (pValues == null) ? new ContentValues() : pValues;
		values.put(GameStorageHelper.COLUMN_PLAYERS, playerData);
		values.put(GameStorageHelper.COLUMN_ROUNDS, roundsData);
		values.put(GameStorageHelper.COLUMN_STARTTIME, startTime);
		values.put(GameStorageHelper.COLUMN_WINNER, winnerData);
		values.put(GameStorageHelper.COLUMN_METADATA, metaData);
		values.put(GameStorageHelper.COLUMN_RUNTIME, runningTime);
		values.put(GameStorageHelper.COLUMN_ORIGIN, originData);
		if (!isValidId(mId)) {
			// New game 
			mId = GameStorageHelper.getIdOrStarttimeFromUri(resolver.insert( 
						GameStorageHelper.getUriAllItems(getKey()), values));
			
		} else {
			// Update game
			resolver.update(GameStorageHelper.getUriWithId(getKey(), mId), values, null, null);
		}
		return;
	}
	
	// players in given team must be pairwise unequal
	public static long getUnfinishedGame(int gameKey, ContentResolver resolver, List<Player> matchTeam, boolean subsetOk) {
		String[] projection = { GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_PLAYERS};
		StringBuilder where = new StringBuilder();
		// search unfinished games
		where.append(GameStorageHelper.COLUMN_WINNER);
		where.append(" = ");
		where.append(Game.WINNER_NONE);
		/* which contain all of the given players
		/* this is not 100% accurate since it could say a game with the players (A,B,C,BDDB) is an open game for the given players (D,B,C,A) 
		 * so this only checks for substrings
		 */
		int index = 0;
		String[] selectionArgs = new String[matchTeam.size()];
		for (Player p : matchTeam) {
			where.append(" and ");
			where.append(GameStorageHelper.COLUMN_PLAYERS);
			where.append(" like ?");
			selectionArgs[index] = '%' + p.getName() + '%';
			index++;
		}
		Cursor cursor = null;
		try {
			cursor = resolver.query(GameStorageHelper.getUriAllItems(gameKey), projection, where.toString(), selectionArgs,
				null);
			if (cursor != null) {
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					// check if the game really contains the given players (in arbitrary order)
					Compacter playersData = new Compacter(cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_PLAYERS)));
					if (subsetOk || playersData.getSize() == matchTeam.size()) {
						boolean allPlayersContained = true;
						for (Player currP : matchTeam) {
							boolean foundEqual = false;
							for (int i = 0; i < playersData.getSize(); i++) {
								if (currP.getName().equalsIgnoreCase(playersData.getData(i))) {
									foundEqual = true;
								}
							}
							if (!foundEqual) {
								allPlayersContained = false;
							}
						}
						if (allPlayersContained) {
							long id = cursor.getInt(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ID));
							return id;
						}
					}
					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return Game.NO_ID;
	}
	

	public static long getUnfinishedGame(int gameKey, ContentResolver resolver,
			long startTime) {
		String[] projection = { GameStorageHelper.COLUMN_ID};
		StringBuilder where = new StringBuilder();
		where.append(GameStorageHelper.COLUMN_WINNER);
		where.append(" = ");
		where.append(Game.WINNER_NONE);
		Cursor cursor = null;
		long id = Game.NO_ID;
		try {
			cursor = resolver.query(GameStorageHelper.getUriWithStarttime(gameKey, startTime), projection, where.toString(), null,
				null);
			// since start times are like id's unique this is meant to only return a cursor of length 0 or 1
			if (cursor != null) {
				cursor.moveToFirst();
				if (cursor.isAfterLast()) {
					return Game.NO_ID;
				}
				id = cursor.getInt(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ID));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return id;
	}
	
	public interface GamesDeletionListener {
		void deletedGames(Collection<Long> deletedIds);
	}
	
	public static void deleteConfirmed(final Context context, final Collection<Long> ids, final int gameKey, final GamesDeletionListener listener) {
		if (ids == null || ids.size() == 0) {
			return;
		}
 		// better prompt user, to check if the games really should be deleted
		new AlertDialog.Builder(context)
		.setTitle(context.getResources().getString(R.string.confirm_delete))
		.setMessage(context.getResources().getQuantityString(R.plurals.confirm_delete_games, ids.size(), ids.size()))
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

		    public void onClick(DialogInterface dialog, int whichButton) {
		    	deleteGames(context.getContentResolver(), ids, gameKey, listener);
		    }})
		 .setNegativeButton(android.R.string.no, null).show();
	}
	
	private static void deleteGames(ContentResolver resolver, Collection<Long> ids, int gameKey, GamesDeletionListener listener) {
		GamesDeleteTask deleteTask = new GamesDeleteTask(gameKey, resolver, listener);
		Long[] idsArray = new Long[ids.size()];
		int index = 0;
		for (long id : ids) {
			idsArray[index++] = Long.valueOf(id);
		}
		deleteTask.execute(idsArray);
	}
	
	private static class GamesDeleteTask extends AsyncTask<Long, Integer, Collection<Long>> {
		private final int mGameKey;
		private final ContentResolver mResolver;
		private final GamesDeletionListener mListener;
		private GamesDeleteTask(int gameKey, ContentResolver resolver, GamesDeletionListener listener) {
			mGameKey = gameKey;
			mResolver = resolver;
			mListener = listener;
		}
		@Override
		protected Collection<Long> doInBackground(Long... ids) {
			Collection<Long> deleted = new HashSet<Long>();
			for (long id : ids) {
				Uri uri = GameStorageHelper.getUriWithId(mGameKey, id);
				if (mResolver.delete(uri, null, null) > 0) {
					deleted.add(id);
				}
			}
			return deleted;
		}
		
		@Override
	    protected void onPostExecute(Collection<Long> result) {
			if (mListener != null && result != null && result.size() > 0) {
				mListener.deletedGames(result);
			}
	    }
	}
	
	//maybe this will be needed in some other situation, but best is too avoid reading only single columns
	/*public static List<Player> getPlayers(ContentResolver resolver, int gameKey, long gameId) {
		if (!Game.isValidId(gameId)) {
			return null;
		}
		Cursor cursor = resolver.query(GameStorageHelper.getUri(gameKey, gameId), new String[] {GameStorageHelper.COLUMN_PLAYERS}, null, null,
				null);
		if (cursor != null) {
			cursor.moveToFirst();
			if (!cursor.isAfterLast()) {
				String playerData = cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_PLAYERS));
				Compacter playerNames = new Compacter(playerData);
				List<Player> players = new ArrayList<Player>(playerNames.getSize());
				for (String player : playerNames) {
					players.add(GameKey.getPool(gameKey).populatePlayer(player));
				}
				return players;
			}
			cursor.close();
		}
		return null;
	}*/

	
	public static final boolean isValidId(long id) {
		return id >= 0;
	}

}
