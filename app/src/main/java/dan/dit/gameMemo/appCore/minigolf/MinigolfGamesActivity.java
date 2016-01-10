package dan.dit.gameMemo.appCore.minigolf;

import java.util.Collection;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.SportGame;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class MinigolfGamesActivity extends GamesActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SportGame.initLocations(getContentResolver(), GameKey.MINIGOLF);
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
        String[] players = null;
        
        if (Game.isValidId(copyGameSetupId)) {
            List<Game> games = null;
            try {
                games = Game.loadGames(GameKey.MINIGOLF, getContentResolver(), GameStorageHelper.getUriWithId(GameKey.MINIGOLF, copyGameSetupId), true);
            } catch (CompactedDataCorruptException e) {
                // fail silently and do not change default information
            }
            if (games != null && games.size() > 0) {
                MinigolfGame game = (MinigolfGame) games.get(0);
                players = new String[game.getPlayerCount()];
                int index = 0;
                for (Player p : game.getPlayers()) {
                    players[index++] = p.getName();
                }
                options.setLocation(game.getLocation());
                options.setDefaultLanesCount(game.getLanesCount());
            }
        }
        
        // make teams
        TeamSetupTeamsController.Builder teamsBuilder = new TeamSetupTeamsController.Builder(true, true, true);
        teamsBuilder.addTeam(1, MinigolfGame.MAX_PLAYERS, false, getResources().getString(R.string.game_setup_general_player_list), false, TeamSetupViewController.DEFAULT_TEAM_COLOR, false, players);
        
        Intent i = GameSetupActivity.makeIntent(this, GameKey.MINIGOLF, true, options.build(), teamsBuilder.build());
        startActivityForResult(i, GAME_SETUP_ACTIVITY);
    }

}
