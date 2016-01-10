package dan.dit.gameMemo.gameData.game.binokel;

import android.content.res.Resources;
import android.os.Bundle;

import java.util.Collection;
import java.util.List;

import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.statistics.AttributeData;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelGameStatisticAttributeManager extends GameStatisticAttributeManager {
    public static final BinokelGameStatisticAttributeManager INSTANCE = new BinokelGameStatisticAttributeManager();
    //TODO implement methods
    private BinokelGameStatisticAttributeManager() {
        super(GameKey.BINOKELGAME);
    }

    @Override
    protected boolean containsAllTeams(Game pGame, AttributeData data) {
        return false;
    }

    @Override
    protected Collection<StatisticAttribute> createPredefinedAttributes() {
        return null;
    }

    @Override
    public Bundle createTeamsParameters(int mode, Resources res, List<AbstractPlayerTeam> teamSuggestions) {
        return null;
    }
}
