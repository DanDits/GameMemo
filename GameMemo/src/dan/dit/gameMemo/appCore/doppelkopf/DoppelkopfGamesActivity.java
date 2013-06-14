package dan.dit.gameMemo.appCore.doppelkopf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameSetupActivity;
import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class DoppelkopfGamesActivity extends GamesActivity  {
	private static final int[] DOPPELKOPF_GAME_MIN_PLAYERS = new int[] {4};
	private static final int[] DOPPELKOPF_GAME_MAX_PLAYERS = new int[] {5};
	private static final int[] DOPPELKOPF_OPTIONS_NUMBER = new int[] {DoppelkopfGame.DEFAULT_DURCHLAEUFE, DoppelkopfGame.DEFAULT_DUTY_SOLI}; //{DURCHLAEUFE,DUTY_SOLI}
	private static final int[] DOPPELKOPF_OPTIONS_MIN_NUMBERS = new int[] {0, 0};
	private static final int[] DOPPELKOPF_OPTIONS_MAX_NUMBERS = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE};
 	
	@Override
	protected void startGameSetup(long id) {
		Intent i = new Intent(this, GameSetupActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.DOPPELKOPF);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MIN_PLAYERS, DOPPELKOPF_GAME_MIN_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MAX_PLAYERS, DOPPELKOPF_GAME_MAX_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_NAMES, new String[] { getResources().getString(R.string.doppelkopf_team_name)});
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MAX_VALUES, DOPPELKOPF_OPTIONS_MAX_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MIN_VALUES, DOPPELKOPF_OPTIONS_MIN_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES, DOPPELKOPF_OPTIONS_NUMBER);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_NAMES, new String[] {getResources().getString(R.string.doppelkopf_option_cycles), getString(R.string.doppelkopf_option_duty_soli)});
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
				games = GameKey.loadGames(GameKey.DOPPELKOPF, getContentResolver(), GameStorageHelper.getUriWithId(GameKey.DOPPELKOPF, copyGameSetupId));
			} catch (CompactedDataCorruptException e) {
				// fail silently and do not change default information
			}
			if (games != null && games.size() > 0) {
				DoppelkopfGame game = (DoppelkopfGame) games.get(0);
				List<Player> players = new ArrayList<Player>(game.getPlayers());			
				String[] playerNames = new String[players.size()];
				for (int index = 0; index < playerNames.length; index++) {
					Player curr = players.get(index);
					playerNames[index] = curr == null ? null : curr.getName();
				}
				i.putExtra(GameSetupActivity.EXTRA_PLAYER_NAMES, playerNames);
				i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES, new int[] {game.getLimit(), game.getDutySoliCountPerPlayer()});
			}
		}
		startActivityForResult(i, GAME_SETUP_ACTIVITY);
	}
	
	@Override
	protected void reactToGameSetupActivity(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF))) {
				selectGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF)));
			} else {
				String[] playerNames = extras.getStringArray(GameSetupActivity.EXTRA_PLAYER_NAMES);
				int[] numberOptions = extras.getIntArray(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES);
				if (playerNames != null && playerNames.length >= DoppelkopfGame.MIN_PLAYERS) {
					extras.putStringArray(DoppelkopfGameDetailFragment.EXTRAS_NEW_GAME_PLAYERS, playerNames);
					extras.putInt(DoppelkopfGameDetailFragment.EXTRA_NEW_GAME_ROUNDS_LIMIT, numberOptions[0]);
					extras.putInt(DoppelkopfGameDetailFragment.EXTRA_NEW_GAME_DUTY_SOLI_PER_PLAYER, numberOptions[1]);
					loadGameDetails(extras);
				}
			}
		}
	}
	
	@Override
	protected void showStatistics() {
		//TODO start statistics for doppelkopf...
	}
}
