package dan.dit.gameMemo.gameData.game.binokel;

import java.util.ArrayList;
import java.util.List;

import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.InadequateRoundInfo;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelGameBuilder extends GameBuilder {
    public BinokelGameBuilder() {
        mInst = new BinokelGame();
    }

    @Override
    public GameBuilder loadPlayer(Compacter data) throws CompactedDataCorruptException {
        if (data.getSize() < BinokelGame.MIN_PLAYER_COUNT) {
            throw new CompactedDataCorruptException("A binokel game needs at least 2 teams, not " +
                    data.getSize()).setCorruptData(data);
        }
        List<Player> players = new ArrayList<>(data.getSize());
        for (String player : data) {
            players.add(GameKey.getPool(GameKey.BINOKEL).populatePlayer(player));
        }
        ((BinokelGame) mInst).addAsTeams(players);
        return this;
    }

    @Override
    public GameBuilder loadMetadata(Compacter metaData) throws CompactedDataCorruptException {
        // parsing meta data, no data is required to construct a valid game

        if (metaData.getSize() >= 1) {
            int scoreLimit = metaData.getInt(0);
            ((BinokelGame) mInst).setScoreLimit(scoreLimit);
        }
        if (metaData.getSize() >= 3) {
            int durchScore = metaData.getInt(1);
            int untenDurchScore = metaData.getInt(2);
            ((BinokelGame) mInst).setSpecialGamesScores(durchScore, untenDurchScore);
        }
        return this;
    }

    @Override
    public GameBuilder loadRounds(Compacter roundsData) throws CompactedDataCorruptException {
        // parsing round data
        for (String round : roundsData) {
            BinokelRound currRound = null;
            try {
                currRound = new BinokelRound((BinokelGame) mInst, new Compacter(round));
            } catch (InadequateRoundInfo e) {
                throw new CompactedDataCorruptException("Could not parse round, info is not adequate.", e);
            }
            if (currRound != null) {
                if (mInst.isFinished()) {
                    break;
                }
                mInst.addRound(currRound);
            }
        }
        return this;
    }
}
