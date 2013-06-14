package dan.dit.gameMemo.appCore.tichu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameSetupActivity;
import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothDataExchangeActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

/**
 * This fragment activity is the home activity for tichu games. It holds
 * a {@link TichuGamesOverviewListFragment} and offers an option menu to start
 * the {@link GameSetupActivity}, the {@link TichuGamesStatisticsActivity}
 * or the {@link BluetoothDataExchangeActivity}.<br>
 * Depending on the layout, it also holds a {@link TichuGameDetailFragment} or starts a {@link TichuGameDetailActivity}
 * when there is a game being selected.
 * @author Daniel
 *
 */
public class TichuGamesActivity extends GamesActivity  {
	private static final int[] TICHU_GAME_MIN_PLAYERS = new int[] {2, 2};
	private static final int[] TICHU_GAME_MAX_PLAYERS = new int[] {2, 2};
	private static final boolean[] TICHU_OPTIONS_BOOLEAN = new boolean[] {false}; // {MERCY_RULE}
	private static final int[] TICHU_OPTIONS_NUMBER = new int[] {TichuGame.DEFAULT_SCORE_LIMIT};//{SCORE_LIMIT}
	private static final int[] TICHU_OPTIONS_MIN_NUMBERS = new int[] {TichuGame.MIN_SCORE_LIMIT};
	private static final int[] TICHU_OPTIONS_MAX_NUMBERS = new int[] {TichuGame.MAX_SCORE_LIMIT};
 	
	@Override
	protected void startGameSetup(long id) {
		Intent i = new Intent(this, GameSetupActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.TICHU);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MIN_PLAYERS, TICHU_GAME_MIN_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MAX_PLAYERS, TICHU_GAME_MAX_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MAX_VALUES, TICHU_OPTIONS_MAX_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MIN_VALUES, TICHU_OPTIONS_MIN_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES, TICHU_OPTIONS_NUMBER);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_VALUES, TICHU_OPTIONS_BOOLEAN);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_NAMES, new String[] {getResources().getString(R.string.tichu_game_score_limit)});
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_NAMES, new String[] {getResources().getString(R.string.tichu_game_mery_rule)});
		i.putExtra(GameSetupActivity.EXTRA_FLAG_SUGGEST_UNFINISHED_GAME, true);
		/*i.putExtra(GameSetupActivity.EXTRA_FLAG_ALLOW_PLAYER_COLOR_EDITING, true);
		i.putExtra(GameSetupActivity.EXTRA_FLAG_ALLOW_TEAM_COLOR_EDITING, true);
		i.putExtra(GameSetupActivity.EXTRA_FLAG_ALLOW_TEAM_NAME_EDITING, true);
		i.putExtra(GameSetupActivity.EXTRA_FLAG_USE_DUMMY_PLAYERS, true);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_COLORS, new int[] {0xFFFF6543, 0xFFAA1245});
		i.putExtra(GameSetupActivity.EXTRA_TEAM_IS_OPTIONAL, new boolean[] {false, false, true, true});*/
		// priority to copy info from: parameter id, highlighted id, single checked id
		long copyGameSetupId = Game.isValidId(id) ? id : getHighlightedGame();
		if (!Game.isValidId(copyGameSetupId)) {
			Collection<Long> checked = mOverviewFragment.getCheckedIds();
			if (checked.size() == 1) {
				copyGameSetupId = checked.iterator().next();
			}
		}
		if (Game.isValidId(copyGameSetupId)) {
			List<Game> games = null;
			try {
				games = GameKey.loadGames(GameKey.TICHU, getContentResolver(), GameStorageHelper.getUriWithId(GameKey.TICHU, copyGameSetupId));
			} catch (CompactedDataCorruptException e) {
				// fail silently and do not change default information
			}
			if (games != null && games.size() > 0) {
				TichuGame game = (TichuGame) games.get(0);
				List<Player> players = new ArrayList<Player>(TichuGame.TOTAL_PLAYERS);
				for (Player p : game.getTeam1()) {players.add(p);}
				for (Player p : game.getTeam2()) {players.add(p);}				
				String[] playerNames = new String[TichuGame.TOTAL_PLAYERS];
				for (int index = 0; index < playerNames.length; index++) {
					Player curr = players.get(index);
					playerNames[index] = curr == null ? null : curr.getName();
				}
				i.putExtra(GameSetupActivity.EXTRA_PLAYER_NAMES, playerNames);
				i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES, new int[] {game.getScoreLimit()});
				i.putExtra(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_VALUES, new boolean[] {game.usesMercyRule()});
			}
		}
		startActivityForResult(i, GAME_SETUP_ACTIVITY);
	}
	
	@Override
	protected void reactToGameSetupActivity(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(GameStorageHelper.getCursorItemType(GameKey.TICHU))) {
				selectGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU)));
			} else {
				String[] playerNames = extras.getStringArray(GameSetupActivity.EXTRA_PLAYER_NAMES);
				boolean[] boolOptions = extras.getBooleanArray(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_VALUES);
				int[] numberOptions = extras.getIntArray(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES);
				if (playerNames != null && playerNames.length >= TichuGame.TOTAL_PLAYERS) {
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM1_PLAYER_1, playerNames[0]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM1_PLAYER_2, playerNames[1]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM2_PLAYER_1, playerNames[2]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM2_PLAYER_2, playerNames[3]);
					if (numberOptions != null) {
						extras.putInt(TichuGameDetailFragment.EXTRA_NEW_GAME_SCORE_LIMIT, numberOptions[0]);
					}
					if (boolOptions != null) {
						extras.putBoolean(TichuGameDetailFragment.EXTRA_NEW_GAME_USE_MERCY_RULE, boolOptions[0]);
					}
					loadGameDetails(extras);
				}
			}
		}
	}
	
	@Override
	protected void showStatistics() {
		Intent i = new Intent(this, TichuGamesStatisticsActivity.class);
		startActivity(i);
	}
}
