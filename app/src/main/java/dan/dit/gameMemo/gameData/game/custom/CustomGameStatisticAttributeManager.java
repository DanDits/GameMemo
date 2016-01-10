package dan.dit.gameMemo.gameData.game.custom;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;
import android.os.Bundle;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.appCore.statistics.StatisticsActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.statistics.AttributeData;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;
import dan.dit.gameMemo.gameData.statistics.UserStatisticAttribute;

public class CustomGameStatisticAttributeManager extends
        GameStatisticAttributeManager {
    public static final CustomGameStatisticAttributeManager INSTANCE = new CustomGameStatisticAttributeManager();

    public CustomGameStatisticAttributeManager() {
        super(GameKey.CUSTOMGAME);
    }

    @Override
    protected boolean containsAllTeams(Game pGame, AttributeData data) {
        CustomGame game = (CustomGame) pGame;
        for (int i = 0; i < data.getTeamsCount(); i++) {
            if (!game.containsTeam(data.getTeam(i))) {
                return false;
            }
        }
        return true;
    }
    
    private static final String IDENTIFIER_ATT_GAME_NAME = "game_name";

    @Override
    protected Collection<StatisticAttribute> createPredefinedAttributes() {
        Map<String, StatisticAttribute> attrs = new HashMap<String, StatisticAttribute>();
        for (StatisticAttribute generalAtt : getGeneralPredefinedAttributes()) {
            addAndCheck(generalAtt, attrs, true);
        }
        StatisticAttribute.Builder attBuilder;
        //GameStatistic.Builder statBuilder;
        StatisticAttribute att;

        // game name
        att = new UserStatisticAttribute() {
            
            @Override public boolean requiresCustomValue() {return true;}
            @Override public boolean acceptGame(Game game, AttributeData data) {
                if (!acceptGameAllSubattributes(game, data)) {
                    return false;
                }
                if (data.getCustomValue() == null) {
                    return true; // nothing to do here
                }
                return ((CustomGame) game).getName().equalsIgnoreCase(data.getCustomValue());
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_GAME_NAME, GameKey.CUSTOMGAME);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setCustomValue("Wizard");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_customgame_game_name_name, R.string.attribute_customgame_game_name_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        return attrs.values();
    }

    @Override
    public Bundle createTeamsParameters(int mode, Resources res,
            List<AbstractPlayerTeam> teamSuggestions) {
        String defaultTeamName = res.getString(dan.dit.gameMemo.R.string.statistics_team_name_player);
        TeamSetupTeamsController.Builder builder = new TeamSetupTeamsController.Builder(false, true, true);
        String[] teams = makeFittingNamesArray(teamSuggestions);
        if (mode == StatisticsActivity.STATISTICS_MODE_ALL) {
            builder.addTeam(1, CustomGame.MAX_PLAYER_PER_TEAM, false, defaultTeamName, false, TeamSetupViewController.DEFAULT_TEAM_COLOR, true, teamNamesToArray(teamSuggestions, 0, teams));
        } else {
            for (int i = 0; i < CustomGame.MAX_TEAMS; i++) {
                builder.addTeam(1, CustomGame.MAX_PLAYER_PER_TEAM, i > 0, null, true, TeamSetupViewController.DEFAULT_TEAM_COLOR, true, null);
            }
        }
        return builder.build();
    }

}
