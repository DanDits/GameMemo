package dan.dit.gameMemo.gameData.game.doppelkopf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.CardGameTable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class DoppelkopfGame extends Game {
	public static final boolean IS_FINISHED_WITHOUT_LIMIT = false;
	public static final int NO_LIMIT = 0;
	public static final int MAX_PLAYERS = 5;
	public static final int MIN_PLAYERS = 4;
	public static final String GAME_NAME = "Doppelkopf";
	public static final PlayerPool PLAYERS = new PlayerPool();
	public static final int DEFAULT_DURCHLAEUFE = 2;
	public static final int DEFAULT_DUTY_SOLI = 0;
	
	private List<Player> mPlayers = new ArrayList<Player>(MAX_PLAYERS);
	private int[] mScores;
	protected int mRoundLimit;
	protected DoppelkopfRuleSystem mRuleSystem;
	protected int mDutySoliCountPerPlayer;
	
	public DoppelkopfGame(String ruleSysName, int roundLimit, int dutySoliCountPerPlayer) {
		mRuleSystem = DoppelkopfRuleSystem.getInstanceByName(ruleSysName);
		if (mRuleSystem == null) {
			mRuleSystem = DoppelkopfRuleSystem.getInstance();
		}
		mRoundLimit = roundLimit;
		if (mRoundLimit < 0) {
			mRoundLimit = NO_LIMIT;
		}
		mDutySoliCountPerPlayer = dutySoliCountPerPlayer;
		if (mDutySoliCountPerPlayer < 0) {
			mDutySoliCountPerPlayer = 0;
		}
	}

	public DoppelkopfGame() {
		this(null, NO_LIMIT, 0);
	}
	
	private List<Integer> getMaxScoreIndices() {
		List<Integer> maxIndices = new ArrayList<Integer>(mScores.length);
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < mScores.length; i++) {
			if (mScores[i] > max) {
				max = mScores[i];
				maxIndices.clear();
				maxIndices.add(Integer.valueOf(i));
			} else if (mScores[i] == max) {
				maxIndices.add(Integer.valueOf(i));
			}
		}
		return maxIndices;
	}
	
	@Override
	public PlayerTeam getWinner() {
		if (isFinished()) {
			List<Integer> maxIndices = getMaxScoreIndices();
			PlayerTeam winners = new PlayerTeam();
			for (int index : maxIndices) {
				winners.addPlayer(mPlayers.get(index));
			}
			return winners;
		}
		return null;
	}

	@Override
	public void addRound(GameRound round) {
		if (isFinished()) {
			throw new IllegalStateException("Game is finished, no more rounds can be added.");
		} else if (round == null) {
			throw new NullPointerException("Cannot add null round.");
		} else if (!(round instanceof DoppelkopfRound)) {
			throw new IllegalArgumentException("Given round is no DoppelkopfRound.");
		} else {
			rounds.add(round);
			DoppelkopfRound r = (DoppelkopfRound) round;
			addRoundToScores(r, rounds.size() - 1);
		}
	}

	@Override
	public void reset() {
		rounds.clear();
		mScores = new int[mPlayers.size()];
	}
	
	public int getLimit() {
		return mRoundLimit;
	}
	
	public boolean hasLimit() {
		return mRoundLimit != NO_LIMIT;
	}

	@Override
	public boolean isFinished() {
		return mRuleSystem.isFinished(this, rounds.size());
	}

	@Override
	public void setupPlayers(List<Player> players) {
		mPlayers.clear();
		for (Player p : players) {
			mPlayers.add(p);
		}
		mScores = new int[mPlayers.size()];
		synch();
	}

	@Override
	protected String getPlayerData() {
		Compacter cmp = new Compacter(TichuGame.TOTAL_PLAYERS);
		for (Player p : mPlayers) {
			cmp.appendData(p.getName());
		}
		return cmp.compact();
	}

	@Override
	protected int getWinnerData() {
		int result = Game.WINNER_NONE;
		if (isFinished()) {
			result = 0;
			for (int index : getMaxScoreIndices()) {
				result |= (1 << index);
			}
		}
		return result;
	}
	
	public static final boolean isPlayerWinner(int winner, int index) {
		return (winner & (1 << index)) != 0;
	}

	@Override
	protected String getMetaData() {
		Compacter cmp = new Compacter(4);
		cmp.appendData(mRuleSystem.getName());
		cmp.appendData(mRoundLimit);
		cmp.appendData(mDutySoliCountPerPlayer);
		return cmp.compact();
	}
	
	public static int extractRoundLimitOfMetadata(Compacter metaData) throws CompactedDataCorruptException {
		return metaData.getInt(1);
	}
	
	public static int extractDutySoliOfMetadata(Compacter metaData) throws CompactedDataCorruptException {
		return metaData.getInt(2);
	}

	@Override
	public void synch() {
		int roundIndex = 0;
		for (GameRound r : rounds) {
			addRoundToScores((DoppelkopfRound) r, roundIndex++);
		}
	}
	
	private void addRoundToScores(DoppelkopfRound round, int roundIndex) {
		int reScore = mRuleSystem.getTotalScore(true, round);
		int contraScore = mRuleSystem.getTotalScore(false, round);
		for (int i = 0; i < mPlayers.size(); i++) {
			mScores[i] += isPlayerActive(i, rounds.size() - 1) ? (isPlayerRe(i, rounds.size() - 1) ? reScore : contraScore) : 0;
		}
	}

	public DoppelkopfRuleSystem getRuleSystem() {
		return mRuleSystem;
	}
	
	@Override
	public int getKey() {
		return GameKey.DOPPELKOPF;
	}

	@Override
	public String getFormattedInfo(Resources res) {
	     java.text.DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        java.text.DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		StringBuilder builder = new StringBuilder();
		builder.append(res.getString(R.string.game_starttime)).append(": ").append(dateFormat.format(new Date(getStartTime()))).append('\n')
		.append(res.getString(R.string.game_runtime)).append(": ").append(timeFormat.format(new Date(getRunningTime()))).append("\n\n");
		for (int i = 0; i < mPlayers.size(); i++) {
			builder.append(mPlayers.get(i).getName()).append(" (").append(mScores[i]).append(" )\n");
		}
		builder.append("\n");
		builder.append(res.getString(R.string.doppelkopf_rule_system_info, mRuleSystem.getName()));
		builder.append("\n");
		if (mDutySoliCountPerPlayer > 0) {
			builder.append(res.getString(R.string.doppelkopf_duty_soli_per_player, mDutySoliCountPerPlayer)).append('\n');
		}
		if (mRoundLimit != NO_LIMIT) {
			builder.append(res.getString(R.string.doppelkopf_round_limit, mRoundLimit)).append('\n');
		}
		
		String originFormatted = getFormattedOrigin();
		if (originFormatted.length() > 0) {
			builder.append(res.getString(R.string.game_origin)).append(": ").append(originFormatted);
		}
		return builder.toString();
	}

	@Override
	public void saveGame(ContentResolver resolver) {
		super.saveGame(null, resolver);
		
	}

	public List<Player> getPlayers() {
		return new ArrayList<Player>(mPlayers);
	}

	public int getPlayerCount() {
		return mPlayers.size();
	}
	
	public static List<Game> loadGames(ContentResolver resolver, Uri uri, boolean throwAtFailure) throws CompactedDataCorruptException {
		return loadGames(resolver, uri, null, null, throwAtFailure);
	}

	public static List<Game> loadGames(ContentResolver resolver, Uri uri, List<Long> timestamps,
			boolean throwAtFailure) throws CompactedDataCorruptException {
		if (timestamps.size() > 0) {
			String timestampSelection = Game.timestampsToSelection(timestamps);
			String[] selectionArgs = Game.timestampsToSelectionArgs(timestamps);
			return loadGames(resolver, uri, timestampSelection, selectionArgs, throwAtFailure);
		} else {
			return loadGames(resolver, uri, throwAtFailure);
		}
	}
	
	private static List<Game> loadGames(ContentResolver resolver, Uri uri,
			String selection, String[] selectionArgs, boolean throwAtFailure) throws CompactedDataCorruptException {
		String[] projection = CardGameTable.AVAILABLE_COLUMNS;
		Cursor cursor = null;
		try {
			cursor = resolver.query(uri, projection, selection, selectionArgs,
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
					
					GameBuilder builder = new DoppelkopfGameBuilder();
					try {
						builder.loadMetadata(new Compacter(metaData))
						.setStarttime(startTime)
						.setRunningTime(runningTime)
						.setId(id)
						.loadPlayer(new Compacter(playerData))
						.loadOrigin(new Compacter(originData))
						.loadRounds(new Compacter(roundsData));
						
					} catch (CompactedDataCorruptException e) {
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
			}
			return games;
		} finally {
			// Always close the cursor
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	public double getDurchlauf() {
		return getDurchlauf(rounds.size());
	}

	protected double getDurchlauf(int round) {
		int giver = 0; // first giver is first player, so start at 0
		int totalPlayers = mPlayers.size();
		int durchlauf = 0;
		for (int currRoundIndex = 0; currRoundIndex < round && currRoundIndex < rounds.size(); currRoundIndex++) {
			if (!mRuleSystem.keepsGiver(this, currRoundIndex)) {
				giver++;
			}
			if (giver == totalPlayers) {
				durchlauf++;
				giver = 0;
			}
		}
		return durchlauf + (giver / (double) totalPlayers);
	}

	public int getDutySoliCountPerPlayer() {
		return mDutySoliCountPerPlayer;
	}

	public int getRemainingSoli(int upToRound) {
		if (mDutySoliCountPerPlayer == 0) {
			return 0;
		}
		int[] remainingSoli = new int[mPlayers.size()];
		Arrays.fill(remainingSoli, mDutySoliCountPerPlayer);
		int roundCount = 0;
		for (GameRound round : rounds) {
			if (roundCount >= upToRound) {
				break;
			}
			roundCount++;
			DoppelkopfRound r = (DoppelkopfRound) round;
			if (r.isSolo()) {
				DoppelkopfSolo solo = (DoppelkopfSolo) r.getRoundStyle();
				if (solo.isValidDutySolo() && remainingSoli[solo.getFirstIndex()] > 0 ) {
					remainingSoli[solo.getFirstIndex()]--;
				}
			}
		}
		int sum = 0;
		for (int soloRem : remainingSoli) {
			sum += soloRem;
		}
		return sum;
	}

	public int getPlayerScoreUpToRound(int index, int round) {
		int sum = 0;
		for (int i = 0; i <= round && i < rounds.size(); i++) {
			DoppelkopfRound r = (DoppelkopfRound) rounds.get(i);
			if (isPlayerActive(index, i)) {
				sum += mRuleSystem.getTotalScore(isPlayerRe(index, i), r);
			}
		}
		return sum;
	}
	
	public boolean isPlayerRe(int index, int round) {
		DoppelkopfRound r = (DoppelkopfRound) rounds.get(round);
		return r.isPlayerRe(index);		
	}
	
	public boolean isPlayerActive(int playerIndex, int round) {
		return mPlayers.size() > MIN_PLAYERS ? getGiver(round) != playerIndex: true;
 	}
	
	public int getGiver(int round) {
		int giver = 0; // first giver is first player, so start at 0
		int totalPlayers = mPlayers.size();
		for (int currRoundIndex = 0; currRoundIndex < round && currRoundIndex < rounds.size(); currRoundIndex++) {
			if (!mRuleSystem.keepsGiver(this, currRoundIndex)) {
				giver++;
			}
			giver %= totalPlayers;
		}
		return giver;
	}

	public boolean enforcesDutySolo(int round) {
		return mRuleSystem.enforcesDutySolo(this, round);
	}

	public int getSoloCount(int playerIndex, int upToRound) {
		int count = 0;
		for (int i = 0; i < upToRound && i < rounds.size(); i++) {
			DoppelkopfRound r = (DoppelkopfRound) rounds.get(i);
			if (r.isSolo() && ((DoppelkopfSolo) r.getRoundStyle()).isValidDutySolo()) {
				count++;
			}
		}
		return count;
	}
	
}
