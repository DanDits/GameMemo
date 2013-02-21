package dan.dit.gameMemo.gameData.game;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.GameSQLiteHelper;
import dan.dit.gameMemo.util.compression.Compressible;
import dan.dit.gameMemo.util.compression.Compressor;

public abstract class Game implements Iterable<GameRound>, Compressible {
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
		assert !isFinished();
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
	 * Returns all players' names in a compressed that can be decompressed by a {@link Compressor}.
	 * The interpretation of the order and grouping of these players is left to the Game implementation.
	 * @return A compressed form of all players.
	 */
	protected abstract String getPlayerData();
	/**
	 * Returns all rounds in a compressed way that can be decompressed by a {@link Compressor}.
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
		Compressor cmp = new Compressor(2);
		cmp.appendData(bluetoothDeviceName == null ? "" : bluetoothDeviceName);
		cmp.appendData(model == null ? "" : model);
		originData = cmp.compress();
	}
	
	@Override
	public String compress() {
		Compressor cmp = GameBuilder.getCompressor(this);
		return cmp.compress();
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
		values.put(GameSQLiteHelper.COLUMN_PLAYERS, playerData);
		values.put(GameSQLiteHelper.COLUMN_ROUNDS, roundsData);
		values.put(GameSQLiteHelper.COLUMN_STARTTIME, startTime);
		values.put(GameSQLiteHelper.COLUMN_WINNER, winnerData);
		values.put(GameSQLiteHelper.COLUMN_METADATA, metaData);
		values.put(GameSQLiteHelper.COLUMN_RUNTIME, runningTime);
		values.put(GameSQLiteHelper.COLUMN_ORIGIN, originData);

		if (!isValidId(mId)) {
			// New game 
			mId = GameStorageHelper.getIdFromUri(resolver.insert( 
						GameStorageHelper.getUriAllItems(getKey()), values));
			
		} else {
			// Update game
			resolver.update(GameStorageHelper.getUri(getKey(), mId), values, null, null);
		}
		return;
	}
	
	// players in given team must be pairwise unequal
	public static long getUnfinishedGame(int gameKey, ContentResolver resolver, List<Player> matchTeam) {
		String[] projection = { GameSQLiteHelper.COLUMN_ID, GameSQLiteHelper.COLUMN_PLAYERS};
		StringBuilder where = new StringBuilder();
		// search unfinished games
		where.append(GameSQLiteHelper.COLUMN_WINNER);
		where.append(" = ");
		where.append(Game.WINNER_NONE);
		/* which contain all of the given players
		/* this is not 100% accurate since it could say a game with the players (A,B,C,BDDB) is an open game for the given players (D,B,C,A) 
		 * so this only checks for substrings
		 */
		for (Player p : matchTeam) {
			where.append(" and ");
			where.append(GameSQLiteHelper.COLUMN_PLAYERS);
			where.append(" like '%");
			where.append(p.getName());
			where.append("%'");
		}
		Cursor cursor = resolver.query(GameStorageHelper.getUriAllItems(gameKey), projection, where.toString(), null,
				null);
		if (cursor != null) {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				// check if the game really contains the given players (in arbitrary order)
				Compressor playersData = new Compressor(cursor.getString(cursor.getColumnIndexOrThrow(GameSQLiteHelper.COLUMN_PLAYERS)));
				if (playersData.getSize() == matchTeam.size()) {
					boolean allPlayersContained = true;
					for (Player currP : matchTeam) {
						boolean foundEqual = false;
						for (int i = 0; i < playersData.getSize(); i++) {
							if (currP.getName().equals(playersData.getData(i))) {
								foundEqual = true;
							}
						}
						if (!foundEqual) {
							allPlayersContained = false;
						}
					}
					if (allPlayersContained) {
						long id = cursor.getInt(cursor.getColumnIndexOrThrow(GameSQLiteHelper.COLUMN_ID));
						return id;
					}
				}
				cursor.moveToNext();
			}
			cursor.close();
		}
		return Game.NO_ID;
	}
	

	public static long getUnfinishedGame(int gameKey, ContentResolver resolver,
			long startTime) {
		String[] projection = { GameSQLiteHelper.COLUMN_ID};
		StringBuilder where = new StringBuilder();
		where.append(GameSQLiteHelper.COLUMN_WINNER);
		where.append(" = ");
		where.append(Game.WINNER_NONE);
		where.append(" and ");
		where.append(GameSQLiteHelper.COLUMN_STARTTIME);
		where.append(" = ");
		where.append(String.valueOf(startTime));
		Cursor cursor = resolver.query(GameStorageHelper.getUriAllItems(gameKey), projection, where.toString(), null,
				null);
		// since start times are like id's unique this is meant to only return a cursor of length 0 or 1
		if (cursor != null) {
			cursor.moveToFirst();
			if (cursor.isAfterLast()) {
				return Game.NO_ID;
			}
			long id = cursor.getInt(cursor.getColumnIndexOrThrow(GameSQLiteHelper.COLUMN_ID));
			cursor.close();
			return id;
		}
		return Game.NO_ID;
	}
	
	public static List<Player> getPlayers(ContentResolver resolver, int gameKey, long gameId) {
		if (!Game.isValidId(gameId)) {
			return null;
		}
		Cursor cursor = resolver.query(GameStorageHelper.getUri(gameKey, gameId), new String[] {GameSQLiteHelper.COLUMN_PLAYERS}, null, null,
				null);
		if (cursor != null) {
			cursor.moveToFirst();
			if (!cursor.isAfterLast()) {
				String playerData = cursor.getString(cursor.getColumnIndexOrThrow(GameSQLiteHelper.COLUMN_PLAYERS));
				Compressor playerNames = new Compressor(playerData);
				List<Player> players = new ArrayList<Player>(playerNames.getSize());
				for (String player : playerNames) {
					players.add(GameKey.getPool(gameKey).populatePlayer(player));
				}
				return players;
			}
			cursor.close();
		}
		return null;
	}

	
	public static final boolean isValidId(long id) {
		return id >= 0;
	}

}
