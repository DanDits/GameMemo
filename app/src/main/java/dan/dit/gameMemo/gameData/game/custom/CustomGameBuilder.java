package dan.dit.gameMemo.gameData.game.custom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class CustomGameBuilder extends GameBuilder {
    private Compacter mMetaData;
    public CustomGameBuilder() {
        mInst = new CustomGame(null, CustomGame.DEFAULT_ROUND_BASED);
    }
    
    @Override
    public GameBuilder loadMetadata(Compacter metaData)
            throws CompactedDataCorruptException {
        mMetaData = metaData;
        CustomGame game = (CustomGame) mInst;
        game.unloadMetadata(metaData);
        return this;
    }

    @Override
    public GameBuilder loadPlayer(Compacter playerData)
            throws CompactedDataCorruptException {
        List<Integer> teamSizes = CustomGame.getTeamSizeDataOfMetadata(mMetaData);
        List<Integer> teamColors = CustomGame.getTeamColorDataOfMetadata(mMetaData);
        List<String> teamNames = CustomGame.getTeamNameDataOfMetadata(mMetaData);
        List<AbstractPlayerTeam> teams = new ArrayList<AbstractPlayerTeam>(teamSizes.size());
        if (teamSizes.size() == 0) {
            throw new CompactedDataCorruptException("No teams for CustomGame.").setCorruptData(mMetaData);
        }
        Iterator<Integer> currTeamSizeIt = teamSizes.iterator();
        int currTeamSize = currTeamSizeIt.next();
        PlayerTeam team = new PlayerTeam(); team.setColor(teamColors.get(0)); team.setTeamName(teamNames.get(0));
        teams.add(team);
        int index = 1;
        for (String name : playerData) {
            if (Player.isValidPlayerName(name)) {
                if (team.getPlayerCount() >= currTeamSize) {
                    if (!currTeamSizeIt.hasNext()) {
                        throw new CompactedDataCorruptException("No more team sizes but players left!").setCorruptData(mMetaData);
                    }
                    currTeamSize = currTeamSizeIt.next();
                    team = new PlayerTeam(); team.setColor(teamColors.get(index)); team.setTeamName(teamNames.get(index));
                    teams.add(team);
                    index++;
                }
                team.addPlayer(CustomGame.PLAYERS.populatePlayer(name));
            }
        }
        ((CustomGame) mInst).setupTeams(teams);
        return this;
    }

    @Override
    public GameBuilder loadRounds(Compacter roundData)
            throws CompactedDataCorruptException {
        for (String round : roundData) {
            CustomRound currRound = null;
            currRound = new CustomRound(new Compacter(round));
            mInst.addRound(currRound);
        }
        return this;
    }

}
