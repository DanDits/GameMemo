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
import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.appCore.doppelkopf.GameSetupOptions;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRuleSystem;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ActivityUtil;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class DoppelkopfGamesActivity extends GamesActivity  {
	private static final String PREFERENCES_DEFAULT_RULE_SYSTEM = "dan.dit.gameMemo.PREF_DEF_RULE_SYSTEM";
 	
	@Override
	protected void startGameSetup(long id) {
        // make options, in case there is a game to copy values from, change the option values
        GameSetupOptions.Builder options = makeOptionsBuilder();
        
		Intent i = new Intent(this, GameSetupActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.DOPPELKOPF);
		// priority to copy info from: parameter id, highlighted id, single checked id
		long copyGameSetupId = Game.isValidId(id) ? id : getHighlightedGame();
		if (!Game.isValidId(copyGameSetupId)) {
			Collection<Long> checked = mOverviewFragment.getCheckedIds();
			if (checked.size() == 1) {
				copyGameSetupId = checked.iterator().next();
			}
		}
		String[] playerNames = null;
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
				playerNames = new String[players.size()];
				for (int index = 0; index < playerNames.length; index++) {
					Player curr = players.get(index);
					playerNames[index] = curr == null ? null : curr.getName();
				}
				options.setDurchlauefe(game.getLimit());
				options.setDutySoli(game.getDutySoliCountPerPlayer());
			}
		}
		
        // make teams
        TeamSetupTeamsController.Builder teamsBuilder = new TeamSetupTeamsController.Builder(false, false);
        teamsBuilder.addTeam(4, 5, false, getResources().getString(R.string.doppelkopf_team_name), false, 
                TeamSetupViewController.DEFAULT_TEAM_COLOR, false, playerNames); 
        
        i.putExtra(GameSetupActivity.EXTRA_TEAMS_PARAMETERS, teamsBuilder.build());

        // set options
        i.putExtra(GameSetupActivity.EXTRA_OPTIONS_PARAMETERS, options.build());
        
		startActivityForResult(i, GAME_SETUP_ACTIVITY);
	}
	
	private GameSetupOptions.Builder makeOptionsBuilder() {
		DoppelkopfRuleSystem ruleSys = DoppelkopfRuleSystem.getInstanceByName(getDefaultRuleSystem());
		return new GameSetupOptions.Builder(ruleSys);
	}
	
	@Override
	protected void reactToGameSetupActivity(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF))) {
				selectGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF)));
			} else {
                Bundle options = extras.getBundle(GameSetupActivity.EXTRA_OPTIONS_PARAMETERS);
                extras.putInt(DoppelkopfGameDetailFragment.EXTRA_NEW_GAME_ROUNDS_LIMIT, GameSetupOptions.extractDurchlaeufe(options));
                extras.putInt(DoppelkopfGameDetailFragment.EXTRA_NEW_GAME_DUTY_SOLI_PER_PLAYER, GameSetupOptions.extractDutySoli(options));
                
			    Bundle teamParameters = extras.getBundle(GameSetupActivity.EXTRA_TEAMS_PARAMETERS);
				String[] playerNames = teamParameters.getStringArray(TeamSetupTeamsController.EXTRA_PLAYER_NAMES);
				if (playerNames != null && playerNames.length >= DoppelkopfGame.MIN_PLAYERS) {
					extras.putStringArray(DoppelkopfGameDetailFragment.EXTRAS_NEW_GAME_PLAYERS, playerNames);
					extras.putString(DoppelkopfGameDetailFragment.EXTRA_RULE_SYSTEM, getDefaultRuleSystem());
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
        ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
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
