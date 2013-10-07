package dan.dit.gameMemo.gameData.statistics;

import android.content.ContentValues;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameRound;

public class UserStatisticAttribute extends StatisticAttribute {

    @Override
    protected void addSaveData(ContentValues val) {}
    
    @Override
    public boolean acceptGame(Game game, AttributeData data) {
        return acceptGameAllSubattributes(game);
    }
    
    @Override
    public boolean acceptRound(Game game, GameRound round, AttributeData data) {
        return acceptRoundAllSubattributes(game, round);
    }
}
