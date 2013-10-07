package dan.dit.gameMemo.gameData.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;

/**
 * Simple structure to hold all data that is required for calculating if a game or round is
 * to be accepted.
 * @author Daniel
 *
 */
public class AttributeData {

    protected List<AbstractPlayerTeam> mTeams;
    protected Set<StatisticAttribute> mAttributes = new HashSet<StatisticAttribute>();
    protected String mCustomValue;
    
    @Override
    public String toString() {
        return "AttributeData: " + mAttributes + "; cv=" + mCustomValue + "; teams=" + mTeams;
    }
}
