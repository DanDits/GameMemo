package dan.dit.gameMemo.appCore.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.custom.CustomGame;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ActivityUtil;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class CustomGamesActivity extends GamesActivity {
    private Spinner mGameSelectSpinner;
    public static final String PREFERENCE_LAST_SELECTED_GAME_NAME = "dan.dit.gameMemo.PREF_LAST_SELECTED_GAME_NAME"; // String

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGameSelectSpinner = (Spinner) findViewById(R.id.game_name_chooser);
        mGameSelectSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapter, View view,
                    int position, long id) {
                applySelection(position > 0 ? adapter.getItemAtPosition(position).toString() : "");
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                applySelection("");
            }
        });
        CustomGame.initGameNames(getContentResolver());
    }
    
    public static String getSavedSelection(Context context) {
        return context.getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE).getString(PREFERENCE_LAST_SELECTED_GAME_NAME, "");
    }
    
    private void applySelection(String selection) {
        SharedPreferences.Editor edit = getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();
        edit.putString(PREFERENCE_LAST_SELECTED_GAME_NAME, selection);
        ActivityUtil.commitOrApplySharedPreferencesEditor(edit);
        if (mOverviewFragment != null) {
            ((CustomGamesOverviewListFragment) mOverviewFragment).setFilterGameName(selection);
            ((CustomGamesOverviewListFragment) mOverviewFragment).getLoaderManager().restartLoader(0, null, mOverviewFragment);
            
        }
    }
    
    private String getAllGamesName() {
        return getResources().getString(R.string.customgame_game_select_all);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        List<String> names = new ArrayList<String>();
        names.add(getAllGamesName());
        names.addAll(CustomGame.ALL_NAMES);
        String lastSelected = getSavedSelection(this);
        mGameSelectSpinner.setAdapter(new ArrayAdapter<String>(this,R.layout.game_names_item, names));
        if (TextUtils.isEmpty(lastSelected)) {
            mGameSelectSpinner.setSelection(0);
        } else {
            int index = names.indexOf(lastSelected);
            if (index >= 0 && index < names.size()) {
                mGameSelectSpinner.setSelection(index);
            }
        }
    }
    
    @Override
    protected void startGameSetup(long id) {
        // make options, in case there is a game to copy values from, change the option values
        GameSetupOptions.Builder options = new GameSetupOptions.Builder();

        // priority to copy info from: parameter id, highlighted id, single checked id
        long copyGameSetupId = Game.isValidId(id) ? id : getHighlightedGame();
        if (!Game.isValidId(copyGameSetupId)) {
            Collection<Long> checked = mOverviewFragment.getCheckedIds();
            if (checked.size() == 1) {
                copyGameSetupId = checked.iterator().next();
            }
        }
        List<String[]> teams = new ArrayList<String[]>(CustomGame.MAX_TEAMS);
        List<Integer> teamColors = new ArrayList<Integer>(CustomGame.MAX_TEAMS);
        List<String> teamNames = new ArrayList<String>(CustomGame.MAX_TEAMS);
        
        if (Game.isValidId(copyGameSetupId)) {
            List<Game> games = null;
            try {
                games = Game.loadGames(GameKey.CUSTOMGAME, getContentResolver(), GameStorageHelper.getUriWithId(GameKey.CUSTOMGAME, copyGameSetupId), true);
            } catch (CompactedDataCorruptException e) {
                // fail silently and do not change default information
            }
            if (games != null && games.size() > 0) {
                CustomGame game = (CustomGame) games.get(0);
                for (AbstractPlayerTeam team : game.getTeams()) {
                    String[] players = new String[CustomGame.MAX_PLAYER_PER_TEAM];
                    int playerIndex = 0;
                    for (Player p : team) {
                        players[playerIndex] = p.getName();
                        playerIndex++;
                    }
                    teams.add(players);
                    teamColors.add(team.getColor());
                    teamNames.add(team.getTeamName());
                }
                options.setGameName(game.getName());
                options.setRoundBased(game.isRoundBased());
            }
        }
        
        // make teams, first add the ones extracted from the given loaded game if any
        TeamSetupTeamsController.Builder teamsBuilder = new TeamSetupTeamsController.Builder(true, true, true);
        for (int i = 0; i < teams.size(); i++) {
            teamsBuilder.addTeam(1, CustomGame.MAX_PLAYER_PER_TEAM, i > 0, teamNames.get(i), true, teamColors.get(i), true, teams.get(i));
        }
        // fill up to maximum possible teams
        for (int i = teams.size(); i < CustomGame.MAX_TEAMS; i++) {
            teamsBuilder.addTeam(1, CustomGame.MAX_PLAYER_PER_TEAM, i > 0, null, true, TeamSetupViewController.DEFAULT_TEAM_COLOR, true, null);
        }
        
        Intent i = GameSetupActivity.makeIntent(this, GameKey.CUSTOMGAME, false, options.build(), teamsBuilder.build());
        startActivityForResult(i, GAME_SETUP_ACTIVITY);
    }

}
