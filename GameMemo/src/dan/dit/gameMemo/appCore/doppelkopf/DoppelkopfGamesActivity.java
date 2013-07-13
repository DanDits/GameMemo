package dan.dit.gameMemo.appCore.doppelkopf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameSetupActivity;
import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRuleSystem;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class DoppelkopfGamesActivity extends GamesActivity  {
	private static final int[] DOPPELKOPF_GAME_MIN_PLAYERS = new int[] {4};
	private static final int[] DOPPELKOPF_GAME_MAX_PLAYERS = new int[] {5};
	private static final int[] DOPPELKOPF_OPTIONS_MIN_NUMBERS = new int[] {0, 0};
	private static final int[] DOPPELKOPF_OPTIONS_MAX_NUMBERS = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE};
	private static final String PREFERENCES_DEFAULT_RULE_SYSTEM = "dan.dit.gameMemo.PREF_DEF_RULE_SYSTEM";
 	
	@Override
	protected void startGameSetup(long id) {
		Intent i = new Intent(this, GameSetupActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.DOPPELKOPF);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MIN_PLAYERS, DOPPELKOPF_GAME_MIN_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MAX_PLAYERS, DOPPELKOPF_GAME_MAX_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_NAMES, new String[] { getResources().getString(R.string.doppelkopf_team_name)});
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MAX_VALUES, DOPPELKOPF_OPTIONS_MAX_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MIN_VALUES, DOPPELKOPF_OPTIONS_MIN_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES, makeOptionsNumbers());
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
	
	private int[] makeOptionsNumbers() {
		DoppelkopfRuleSystem ruleSys = DoppelkopfRuleSystem.getInstanceByName(getDefaultRuleSystem());
		if (ruleSys == null) {
			return new int[] {DoppelkopfGame.DEFAULT_DURCHLAEUFE, DoppelkopfGame.DEFAULT_DUTY_SOLI}; //{DURCHLAEUFE,DUTY_SOLI}
		} else {
			return new int[] {ruleSys.getDefaultDurchlaeufe(), ruleSys.getDefaultDutySoli()};
		}
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
					extras.putString(DoppelkopfGameDetailFragment.EXTRA_RULE_SYSTEM, getDefaultRuleSystem());
					extras.putInt(DoppelkopfGameDetailFragment.EXTRA_NEW_GAME_ROUNDS_LIMIT, numberOptions[0]);
					extras.putInt(DoppelkopfGameDetailFragment.EXTRA_NEW_GAME_DUTY_SOLI_PER_PLAYER, numberOptions[1]);
					loadGameDetails(extras);
				}
			}
		}
	}
	
	private String getDefaultRuleSystem() {
		SharedPreferences sharedPref = getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE);
		return sharedPref.getString(PREFERENCES_DEFAULT_RULE_SYSTEM, DoppelkopfRuleSystem.NAME_TOURNAMENT1);
	}
	
	private boolean hasDefaultRuleSystem() {
		SharedPreferences sharedPref = getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE);
		return sharedPref.getString(PREFERENCES_DEFAULT_RULE_SYSTEM, null) != null;
	}
	
	private void setDefaultRuleSystem(String ruleSysName) {
		SharedPreferences.Editor editor = getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();
		editor.putString(PREFERENCES_DEFAULT_RULE_SYSTEM, ruleSysName);
		editor.commit();
	}
	
	private void selectRuleSystem(final int forwardToOnGamesLoaded) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.doppelkopf_rule_system_info, getDefaultRuleSystem()));
		final DoppelkopfRuleSystem[] ALL = DoppelkopfRuleSystem.getAllSystems();
		String[] ALL_NAMES = new String[ALL.length];
		for (int i = 0; i < ALL_NAMES.length; i++) {
			ALL_NAMES[i] = ALL[i].getName() + "\n" + getResources().getString(ALL[i].getDescriptionResource());
		}
		builder.setItems(ALL_NAMES, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				setDefaultRuleSystem(ALL[which].getName());
				if (forwardToOnGamesLoaded >= 0) {
					DoppelkopfGamesActivity.super.onGamesLoaded(forwardToOnGamesLoaded);
				}
			}
			
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.select_rule_system:
			selectRuleSystem(-1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void showStatistics() {
		//TODO start statistics for doppelkopf...
	}
	
	@Override
	public void onGamesLoaded(int count) {
		if (!hasDefaultRuleSystem()) {
			selectRuleSystem(count);
		} else {
			super.onGamesLoaded(count);
		}
	}
}
