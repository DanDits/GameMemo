package dan.dit.gameMemo.gameData.game.minigolf;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;
import android.os.Bundle;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.appCore.statistics.StatisticsActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.statistics.AttributeData;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;

public class MinigolfGameStatisticAttributeManager extends
        GameStatisticAttributeManager {
    public static final MinigolfGameStatisticAttributeManager INSTANCE = new MinigolfGameStatisticAttributeManager();

    public MinigolfGameStatisticAttributeManager() {
        super(GameKey.DOPPELKOPF);
    }

    @Override
    protected boolean containsAllTeams(Game pGame, AttributeData data) {
        MinigolfGame game = (MinigolfGame) pGame;
        return game.containsPlayers(data.getTeam(0));
    }

    @Override
    protected Collection<StatisticAttribute> createPredefinedAttributes() {
        Map<String, StatisticAttribute> attrs = new HashMap<String, StatisticAttribute>();
        for (StatisticAttribute generalAtt : getGeneralPredefinedAttributes()) {
            addAndCheck(generalAtt, attrs, true);
        }
        StatisticAttribute.Builder attBuilder;
        GameStatistic.Builder statBuilder;
        StatisticAttribute att;
        //TODO some stats
        return attrs.values();
    }

    @Override
    public Bundle createTeamsParameters(int mode, Resources res,
            List<AbstractPlayerTeam> teamSuggestions) {
        String defaultTeamName = res.getString(dan.dit.gameMemo.R.string.statistics_team_name_player);
        TeamSetupTeamsController.Builder builder = new TeamSetupTeamsController.Builder(false, true, true);
        String[] teams = makeFittingNamesArray(teamSuggestions);
        if (mode == StatisticsActivity.STATISTICS_MODE_ALL) {
            builder.addTeam(1, MinigolfGame.MAX_PLAYERS, false, defaultTeamName, false, TeamSetupViewController.DEFAULT_TEAM_COLOR, true, teamNamesToArray(teamSuggestions, 0, teams));
        } else {
            for (int i = 0; i < DEFAULT_STATISTIC_MAX_TEAMS; i++) {
                builder.addTeam(1, MinigolfGame.MAX_PLAYERS, i >= 1, defaultTeamName, false, TeamSetupViewController.DEFAULT_TEAM_COLOR , true, teamNamesToArray(teamSuggestions, i, teams));
            }
        }
        return builder.build();
    }

}
