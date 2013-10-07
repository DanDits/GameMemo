package dan.dit.gameMemo.appCore.tichu;

import java.util.Collection;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.appCore.statistics.StatisticsActivity;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothDataExchangeActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
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
 	
	@Override
	protected void startGameSetup(long id) {
	    // make options, in case there is a game to copy values from, change the option values
	    GameSetupOptions.Builder options = new GameSetupOptions.Builder();
	    
        // make intent to start setup activity
		Intent i = new Intent(this, GameSetupActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.TICHU);
		i.putExtra(GameSetupActivity.EXTRA_FLAG_SUGGEST_UNFINISHED_GAME, true);

		// priority to copy info from: parameter id, highlighted id, single checked id
		long copyGameSetupId = Game.isValidId(id) ? id : getHighlightedGame();
		if (!Game.isValidId(copyGameSetupId)) {
			Collection<Long> checked = mOverviewFragment.getCheckedIds();
			if (checked.size() == 1) {
				copyGameSetupId = checked.iterator().next();
			}
		}
		String[] team1Names = new String[2];
		String[] team2Names = new String[2];
		
		if (Game.isValidId(copyGameSetupId)) {
			List<Game> games = null;
			try {
				games = GameKey.loadGames(GameKey.TICHU, getContentResolver(), GameStorageHelper.getUriWithId(GameKey.TICHU, copyGameSetupId));
			} catch (CompactedDataCorruptException e) {
				// fail silently and do not change default information
			}
			if (games != null && games.size() > 0) {
				TichuGame game = (TichuGame) games.get(0);
				team1Names[0] = game.getTeam1().getFirst().getName();
                team1Names[1] = game.getTeam1().getSecond().getName();
                team2Names[0] = game.getTeam2().getFirst().getName();
                team2Names[1] = game.getTeam2().getSecond().getName();
                options.setMercyRuleEnabled(game.usesMercyRule());
                options.setScoreLimit(game.getScoreLimit());
			}
		}
		
	    // make teams
        TeamSetupTeamsController.Builder teamsBuilder = new TeamSetupTeamsController.Builder(false, false);
        teamsBuilder.addTeam(2, 2, false, null, false, TeamSetupViewController.DEFAULT_TEAM_COLOR, false, team1Names);
        teamsBuilder.addTeam(2, 2, false, null, false, TeamSetupViewController.DEFAULT_TEAM_COLOR, false, team2Names);
        i.putExtra(GameSetupActivity.EXTRA_TEAMS_PARAMETERS, teamsBuilder.build());
        
        // set options
        i.putExtra(GameSetupActivity.EXTRA_OPTIONS_PARAMETERS, options.build());
        
        // start the setup
		startActivityForResult(i, GAME_SETUP_ACTIVITY);
	}
	
	@Override
	protected void reactToGameSetupActivity(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(GameStorageHelper.getCursorItemType(GameKey.TICHU))) {
				selectGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU)));
			} else {
			    Bundle options = extras.getBundle(GameSetupActivity.EXTRA_OPTIONS_PARAMETERS);
                extras.putInt(TichuGameDetailFragment.EXTRA_NEW_GAME_SCORE_LIMIT, GameSetupOptions.extractScoreLimit(options));
                extras.putBoolean(TichuGameDetailFragment.EXTRA_NEW_GAME_USE_MERCY_RULE, GameSetupOptions.extractMercyRule(options));
                
			    Bundle teamParameters = extras.getBundle(GameSetupActivity.EXTRA_TEAMS_PARAMETERS);
				String[] playerNames = teamParameters.getStringArray(TeamSetupTeamsController.EXTRA_PLAYER_NAMES);
				if (playerNames != null && playerNames.length >= TichuGame.TOTAL_PLAYERS) {
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM1_PLAYER_1, playerNames[0]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM1_PLAYER_2, playerNames[1]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM2_PLAYER_1, playerNames[2]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM2_PLAYER_2, playerNames[3]);
					loadGameDetails(extras);
				}

			}
		}
	}
	
	@Override
	protected void showStatistics() {
	    Collection<Long> checked = mOverviewFragment.getCheckedGamesStarttimes();
        Intent i = StatisticsActivity.getIntent(this, GameKey.TICHU, checked.isEmpty() ? null : checked);
        startActivity(i);
	}
}
