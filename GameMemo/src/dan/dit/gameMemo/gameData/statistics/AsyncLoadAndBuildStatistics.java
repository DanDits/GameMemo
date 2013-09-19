package dan.dit.gameMemo.gameData.statistics;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.GameStatisticBuilder.StatisticBuildCompleteListener;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class AsyncLoadAndBuildStatistics extends
		AsyncTask<Uri, Integer, GameStatistic> {
	private ContentResolver resolver;
	private List<Player> players;
	private List<StatisticBuildCompleteListener> listeners;
	private GameStatistic finishedStat;
	private final int gameKey;
	
	public AsyncLoadAndBuildStatistics(ContentResolver resolver, List<Player> players, int gameKey) {
		this.resolver = resolver;
		this.players = players;
		this.listeners = new LinkedList<StatisticBuildCompleteListener>();
		this.gameKey = gameKey;
		if (resolver == null || players == null) {
			throw new IllegalArgumentException("No parameter must be null.");
		} else if (!GameKey.isGameSupported(gameKey)) {
			throw new IllegalArgumentException("Gamekey " + gameKey + " not supported.");
		}
	}

	public void addListener(StatisticBuildCompleteListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}
	
	public boolean removeListener(StatisticBuildCompleteListener listener) {
		return listeners.remove(listener);
	}
	
	@Override
	protected GameStatistic doInBackground(Uri... uri) {
		List<Game> games = null;
		try {
			games = GameKey.loadGames(gameKey, resolver, uri[0]);
		} catch(CompactedDataCorruptException e) {
		}
		publishProgress(75);
		if (games == null || games.size() == 0 || isCancelled()) {
			return null;
		}
		GameStatisticBuilder builder = GameKey.getStatisticBuilder(gameKey, players);
		if (builder != null) {
			builder.addGames(games);
			finishedStat = builder.build();
			return finishedStat;
		} else {
			return null;
		}
	}
	
	@Override
    protected void onPostExecute(GameStatistic result) {
		for (StatisticBuildCompleteListener listener : listeners) {
			listener.statisticComplete(result);
		}
    }
	
}
