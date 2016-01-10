package dan.dit.gameMemo.gameData.game.minigolf;

import java.util.ArrayList;
import java.util.List;

import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;


public class MinigolfGameBuilder extends GameBuilder {

    public MinigolfGameBuilder() {
        mInst = new MinigolfGame();
    }
    
    @Override
    public GameBuilder loadMetadata(Compacter metaData)
            throws CompactedDataCorruptException {
        ((MinigolfGame) mInst).setLocation(metaData.getData(0));
        return this;
    }

    @Override
    public GameBuilder loadPlayer(Compacter playerData)
            throws CompactedDataCorruptException {
        List<Player> player = new ArrayList<Player>(playerData.getSize());
        for (String name : playerData) {
            if (Player.isValidPlayerName(name)) {
                player.add(MinigolfGame.PLAYERS.populatePlayer(name));
            }
        }
        ((MinigolfGame) mInst).setupPlayers(player);
        return this;
    }

    @Override
    public GameBuilder loadRounds(Compacter roundData)
            throws CompactedDataCorruptException {
        // parsing round data
        for (String round : roundData) {
            MinigolfRound currRound = null;
            currRound = new MinigolfRound(new Compacter(round));
            mInst.addRound(currRound);
        }
        return this;
    }

}
