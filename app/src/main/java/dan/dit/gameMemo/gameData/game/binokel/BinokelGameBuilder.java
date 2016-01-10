package dan.dit.gameMemo.gameData.game.binokel;

import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelGameBuilder extends GameBuilder {
    //TODO implements methods
    @Override
    public GameBuilder loadMetadata(Compacter metaData) throws CompactedDataCorruptException {
        return null;
    }

    @Override
    public GameBuilder loadPlayer(Compacter playerData) throws CompactedDataCorruptException {
        return null;
    }

    @Override
    public GameBuilder loadRounds(Compacter roundData) throws CompactedDataCorruptException {
        return null;
    }
}
