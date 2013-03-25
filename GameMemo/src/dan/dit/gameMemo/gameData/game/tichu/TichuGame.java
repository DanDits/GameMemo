package dan.dit.gameMemo.gameData.game.tichu;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerDuo;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.TichuTable;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;
import dan.dit.gameMemo.util.compression.Compressor;

public class TichuGame extends Game {
	public static final int MIN_SCORE_LIMIT = 5;
	public static final int MAX_SCORE_LIMIT = 10000;
	public static final int DEFAULT_SCORE_LIMIT = 1000;
	public static boolean DEFAULT_USE_MERCY_RULE = false;
	
	//player ids are unique for each player and follow increase from one player to the next by one
	public static final int PLAYER_ONE_ID = 1;
	public static final int PLAYER_TWO_ID = PLAYER_ONE_ID + 1;
	public static final int PLAYER_THREE_ID = PLAYER_TWO_ID + 1;
	public static final int PLAYER_FOUR_ID = PLAYER_THREE_ID + 1;
	public static final int TOTAL_PLAYERS = 4;
	public static final int INVALID_PLAYER_ID = PLAYER_ONE_ID - 2;
	
	public static final String GAME_NAME = "Tichu";
	public static final PlayerPool PLAYERS = new PlayerPool();
	public static final int WINNER_TEAM1 = 1;
	public static final int WINNER_TEAM2 = 2;
	
	/*
	 * TichuGame meta data: [MERCY_RULE,SCORE_LIMIT]
	 */
	public static final String META_DATA_USES_MERCY_RULE = "MJ";
	public static final String META_DATA_DOES_NOT_USE_MERCY_RULE = "MN";
	
	private PlayerDuo firstTeam;
	private PlayerDuo secondTeam;
	private int scoreTeam1;
	private int scoreTeam2;
	protected int mScoreLimit = DEFAULT_SCORE_LIMIT;
	protected boolean mMercyRuleEnabled = DEFAULT_USE_MERCY_RULE;
	
	protected TichuGame() {
		super();
	}
	
	public TichuGame(int scoreLimit, boolean useMercyRule) {
		if (scoreLimit < MIN_SCORE_LIMIT || scoreLimit > MAX_SCORE_LIMIT) {
			throw new IllegalArgumentException("Score limit out of bounds: " + scoreLimit);
		}
		mScoreLimit = scoreLimit;
		mMercyRuleEnabled = useMercyRule;
	}
	
	@Override
	protected String getPlayerData() {
		Compressor cmp = new Compressor(TichuGame.TOTAL_PLAYERS);
		cmp.appendData(firstTeam.getFirst().getName());
		cmp.appendData(firstTeam.getSecond().getName());
		cmp.appendData(secondTeam.getFirst().getName());
		cmp.appendData(secondTeam.getSecond().getName());
		return cmp.compress();
	}
	
	public PlayerDuo getTeam1() {
		return firstTeam;
	}
	
	public PlayerDuo getTeam2() {
		return secondTeam;
	}
	
	public int getScoreTeam1() {
		return scoreTeam1;
	}
	
	public int getScoreTeam2() {
		return scoreTeam2;
	}
	
	public int getScoreUpToRound(int round, boolean forTeam1) {
		int sum = 0;
		for (int i = 0; i <= round && i < rounds.size(); i++) {
			sum += forTeam1 ? ((TichuRound) rounds.get(i)).getScoreTeam1(mMercyRuleEnabled) : ((TichuRound) rounds.get(i)).getScoreTeam2(mMercyRuleEnabled);
		}
		return sum;
	}
	
	@Override
	protected String getRoundsData() {
		Compressor cmp = new Compressor(rounds.size());
		for (GameRound round : rounds) {
			cmp.appendData(round.compress());
		}
		return cmp.compress();
	}
	
	@Override
	public void addRound(GameRound round) {
		if (isFinished()) {
			throw new IllegalStateException("Game is finished, no more rounds can be added.");
		} else if (round == null) {
			throw new NullPointerException("Cannot add null round.");
		} else if (!(round instanceof TichuRound)) {
			throw new IllegalArgumentException("Given round is no TichuRound.");
		} else {
			rounds.add(round);
			// (re)calculateScore here
			scoreTeam1 += ((TichuRound) round).getScoreTeam1(mMercyRuleEnabled);
			scoreTeam2 += ((TichuRound) round).getScoreTeam2(mMercyRuleEnabled);
		}
	}
	
	private void calculateScore() {
		scoreTeam1 = 0;
		scoreTeam2 = 0;
		for (GameRound round : rounds) {
			scoreTeam1 += ((TichuRound) round).getScoreTeam1(mMercyRuleEnabled);
			scoreTeam2 += ((TichuRound) round).getScoreTeam2(mMercyRuleEnabled);
		}
	}
	
	@Override
	protected void setupPlayers(List<Player> players) {
		if (players == null || players.size() != TichuGame.TOTAL_PLAYERS) {
			throw new IllegalArgumentException("A tichu game needs 4 players.");
		}
		firstTeam = new PlayerDuo(players.get(0), players.get(1));
		secondTeam = new PlayerDuo(players.get(2), players.get(3));
	}
	
	public void setupPlayers(PlayerDuo first, PlayerDuo second) {
		if (first == null || second == null) {
			throw new NullPointerException("Null player duo.");
		}
		firstTeam = first;
		secondTeam = second;
	}
	
	/**
	 * A wrapper for setupPlayers(List<Player>). Sets up the teams.
	 * @param team1First The first player of the first team.
	 * @param team1Second The second player of the first team.
	 * @param team2First The first player of the second team.
	 * @param team2Second The second player of the second team.
	 */
	public void setupPlayers(Player team1First, Player team1Second, Player team2First, Player team2Second) {
		LinkedList<Player> players = new LinkedList<Player>();
		players.add(team1First);
		players.add(team1Second);
		players.add(team2First);
		players.add(team2Second);
		setupPlayers(players);
	}

	@Override
	public void reset() {
		scoreTeam1 = 0;
		scoreTeam2 = 0;
		rounds.clear();
	}

	@Override
	public PlayerTeam getWinner() {
		if (isFinished()) {
			return scoreTeam1 > scoreTeam2 ? firstTeam : secondTeam;
		}
		return null;
	}


	@Override
	public boolean isFinished() {
		return (scoreTeam1 >= mScoreLimit || scoreTeam2 >= mScoreLimit) && scoreTeam1 != scoreTeam2;
	}


	protected int getWinnerData() {
		if (isFinished()) {
			return ((scoreTeam1 > scoreTeam2) ?  WINNER_TEAM1 : WINNER_TEAM2);
		} else {
			return WINNER_NONE;
		}
	}


	@Override
	public void synch() {
		calculateScore();
	}

	/**
	 * Returns the player id for the given player.
	 * @param p The player to check.
	 * @return The player id, see TichuGame.PLAYER_X_ID or 
	 * TichuGame.INVALID_PLAYER_ID if player was <code>null</code> or not
	 * part of this game.
	 */
	public int getPlayerId(Player p) {
		if (firstTeam.getFirst().equals(p)) {
			return TichuGame.PLAYER_ONE_ID;
		} else if (firstTeam.getSecond().equals(p)) {
			return TichuGame.PLAYER_TWO_ID;
		} else if (secondTeam.getFirst().equals(p)) {
			return TichuGame.PLAYER_THREE_ID;
		} else if (secondTeam.getSecond().equals(p)) {
			return TichuGame.PLAYER_FOUR_ID;
		} else {
			return TichuGame.INVALID_PLAYER_ID;
		}
	}
	
	/**
	 * Saves the TichuGame using the given ContentResolver. New games will only be saved
	 * if they have at least one round. After this method returns, use getId() to get the new id
	 * if the game was previously not saved.
	 * @param resolver The ContentResolver to resolve the save request, must not be <code>null</code>.
	 */
	@Override
	public void saveGame(ContentResolver resolver) {
		// if TichuTable stores extra columns then add data to ContentValues
		super.saveGame(null, resolver);
	}
	
	public static List<Game> loadGames(ContentResolver resolver, Uri uri, boolean throwAtFailure) throws CompressedDataCorruptException {
		return loadGames(resolver, uri, null, null, throwAtFailure);
	}

	public static List<Game> loadGames(ContentResolver resolver, Uri uri, List<Long> timestamps,
			boolean throwAtFailure) throws CompressedDataCorruptException {
		if (timestamps.size() > 0) {
			StringBuilder selection = new StringBuilder(GameStorageHelper.COLUMN_STARTTIME.length() + 10 + timestamps.size() * 15);
			selection.append(GameStorageHelper.COLUMN_STARTTIME);
			selection.append(" IN (");
			String[] selectionArgs = new String[timestamps.size()];
			int index = 0;
			for (Long l : timestamps) {
				if (index != 0) {
					selection.append(",?");
				} else {
					selection.append("?");
				}
				if (l != null) {
					selectionArgs[index] = l.toString();
				} else {
					selectionArgs[index] = "0";
				}
				index++;
			}
			selection.append(')');
			return loadGames(resolver, uri, selection.toString(), selectionArgs, throwAtFailure);
		} else {
			return loadGames(resolver, uri, throwAtFailure);
		}
	}
	
	private static List<Game> loadGames(ContentResolver resolver, Uri uri,
			String selection, String[] selectionArgs, boolean throwAtFailure) throws CompressedDataCorruptException {
		String[] projection = TichuTable.AVAILABLE_COLUMNS;
		Cursor cursor = resolver.query(uri, projection, selection, selectionArgs,
				null);
		List<Game> games = null;
		if (cursor != null) {
			games = new LinkedList<Game>();
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String playerData = cursor.getString(cursor
						.getColumnIndexOrThrow(GameStorageHelper.COLUMN_PLAYERS));
				String roundsData = cursor.getString(cursor
						.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ROUNDS));
				String metaData = cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_METADATA));
				long startTime = cursor.getLong(cursor
						.getColumnIndexOrThrow(GameStorageHelper.COLUMN_STARTTIME));
				long id = cursor.getLong(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ID));
				long runningTime = cursor.getLong(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_RUNTIME));
				String originData = cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ORIGIN));
				
				GameBuilder builder = new TichuGameBuilder();
				try {
					builder.loadMetadata(new Compressor(metaData))
					.setStarttime(startTime)
					.setRunningTime(runningTime)
					.setId(id)
					.loadPlayer(new Compressor(playerData))
					.loadOrigin(new Compressor(originData))
					.loadRounds(new Compressor(roundsData));
					
				} catch (CompressedDataCorruptException e) {
					if (throwAtFailure) {
						throw e;
					}
					builder = null;
				}
				if (builder != null) {
					games.add(builder.build());
				}
				cursor.moveToNext();
			}
			// Always close the cursor
			cursor.close();
		}
		return games;
	}

	@Override
	public int getKey() {
		return GameKey.TICHU;
	}
	
	@Override
	public String toString() {
		return "TichuGame: " + firstTeam + " vs. " + secondTeam + "(" + getScoreTeam1() + ":" + getScoreTeam2() + ")";
	}

	public static boolean areValidPlayers(String player1, String player2,
			String player3, String player4) {
		return Player.isValidPlayerName(player1) && Player.isValidPlayerName(player2)
		&& Player.isValidPlayerName(player3) && Player.isValidPlayerName(player4)
		&& !player1.equalsIgnoreCase(player2) && !player1.equalsIgnoreCase(player3) 
		&& !player1.equalsIgnoreCase(player4) 
		&& !player2.equalsIgnoreCase(player3) && !player2.equalsIgnoreCase(player4)
		&& !player3.equalsIgnoreCase(player4);
	}

	public boolean usesMercyRule() {
		return mMercyRuleEnabled;
	}
	
	public int getScoreLimit() {
		return mScoreLimit;
	}

	@Override
	protected String getMetaData() {
		Compressor cmp = new Compressor();
		cmp.appendData(mMercyRuleEnabled ? META_DATA_USES_MERCY_RULE : META_DATA_DOES_NOT_USE_MERCY_RULE);
		cmp.appendData(mScoreLimit);
		return cmp.compress();
	}

	@Override
	public String getFormattedInfo(Resources res) {
        java.text.DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        java.text.DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		StringBuilder builder = new StringBuilder();
		builder.append(res.getString(R.string.game_starttime)).append(": ").append(dateFormat.format(new Date(getStartTime()))).append('\n')
		.append(res.getString(R.string.game_runtime)).append(": ").append(timeFormat.format(new Date(getRunningTime()))).append('\n')
		.append(getTeam1().getFirst().getName()).append(" + " ).append(getTeam1().getSecond().getName())
		.append(" vs. ")
		.append(getTeam2().getFirst().getName()).append(" + " ).append(getTeam2().getSecond().getName()).append('\n')
		.append(getScoreTeam1()).append(':').append(getScoreTeam2()).append('\n');
		if (usesMercyRule()) {
			builder.append(res.getString(R.string.tichu_game_mery_rule)).append(' ');
		}
		if (getScoreLimit() != TichuGame.DEFAULT_SCORE_LIMIT) {
			builder.append(res.getString(R.string.tichu_game_score_limit)).append(": ").append(getScoreLimit()).append('\n');
		}
		String originFormatted = getFormattedOrigin();
		if (originFormatted.length() > 0) {
			builder.append(res.getString(R.string.game_origin)).append(": ").append(originFormatted);
		}
		return builder.toString();
	}

	public void onRenameSuccess(Player newPlayer, String oldName) {
		// possible scenario: a player is being renamed for a game and now we have two equal players in a team or in a game
		// reaction: use vogel-strauss algorithm and ignore this.. the user is prompted that this can corrupt games
		if (firstTeam.contains(oldName)) {
			boolean firstGotRenamed = firstTeam.getFirst().getName().equalsIgnoreCase(oldName);
			PlayerDuo newTeam = new PlayerDuo(firstGotRenamed ? newPlayer : firstTeam.getFirst(), firstGotRenamed ? firstTeam.getSecond() : newPlayer);
			setupPlayers(newTeam, secondTeam);
		} else if (getTeam2().contains(oldName)) {
			boolean firstGotRenamed = secondTeam.getFirst().getName().equalsIgnoreCase(oldName);
			PlayerDuo newTeam = new PlayerDuo(firstGotRenamed ? newPlayer : secondTeam.getFirst(), firstGotRenamed ? secondTeam.getSecond() : newPlayer);			
			setupPlayers(firstTeam, newTeam);
		}
	}

}
