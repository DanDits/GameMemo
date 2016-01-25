package dan.dit.gameMemo.appCore.binokel;

import android.content.Intent;

import java.util.Collection;
import java.util.List;

import dan.dit.gameMemo.appCore.GamesActivity;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.binokel.BinokelGame;
import dan.dit.gameMemo.gameData.game.binokel.BinokelTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelGamesActivity extends GamesActivity {
    @Override
    protected void startGameSetup(long id) {
        // make options, in case there is a game to copy values from, change the option values
        dan.dit.gameMemo.appCore.binokel.GameSetupOptions.Builder options = new dan.dit.gameMemo
                .appCore.binokel.GameSetupOptions.Builder();

        // priority to copy info from: parameter id, highlighted id, single checked id
        long copyGameSetupId = Game.isValidId(id) ? id : getHighlightedGame();
        if (!Game.isValidId(copyGameSetupId)) {
            Collection<Long> checked = mOverviewFragment.getCheckedIds();
            if (checked.size() == 1) {
                copyGameSetupId = checked.iterator().next();
            }
        }
        final int maxPlayerPerTeam = BinokelGame.MAX_PLAYERS_PER_TEAM;
        String[][] teams = new String[BinokelGame.MAX_TEAMS][maxPlayerPerTeam];

        if (Game.isValidId(copyGameSetupId)) {
            List<Game> games = null;
            try {
                games = Game.loadGames(GameKey.BINOKEL, getContentResolver(), GameStorageHelper
                        .getUriWithId(GameKey.BINOKEL, copyGameSetupId), true);
            } catch (CompactedDataCorruptException e) {
                // fail silently and do not change default information
            }
            if (games != null && games.size() > 0) {
                BinokelGame game = (BinokelGame) games.get(0);
                int teamIndex = 0;
                for (BinokelTeam team : game.getTeams()) {
                    int index = 0;
                    for (Player p : team.getPlayers()) {
                        teams[teamIndex][index] = p.getName();
                        index++;
                    }
                    teamIndex++;
                }
                options.setScoreLimit(game.getScoreLimit());
                options.setUntenDurchValue(game.getUntenDurchValue());
                options.setDurchValue(game.getDurchValue());
            }
        }

        // make teams
        TeamSetupTeamsController.Builder teamsBuilder = new TeamSetupTeamsController.Builder
                (false, true, false);
        for (int i = 0; i < BinokelGame.MAX_TEAMS; i++) {
            teamsBuilder.addTeam(BinokelGame.MIN_PLAYERS_PER_TEAM, BinokelGame
                    .MAX_PLAYERS_PER_TEAM,
                    i >= BinokelGame.MIN_TEAMS,
                    null, false, TeamSetupViewController.DEFAULT_TEAM_COLOR, false, teams[i]);
        }

        Intent i = GameSetupActivity.makeIntent(this, GameKey.BINOKEL, true, options.build(),
                teamsBuilder.build());
        startActivityForResult(i, GAME_SETUP_ACTIVITY);
    }

}
