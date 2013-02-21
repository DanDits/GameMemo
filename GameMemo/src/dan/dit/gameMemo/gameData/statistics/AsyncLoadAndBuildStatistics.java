package dan.dit.gameMemo.gameData.statistics;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.tichu.TichuGameStatisticBuilder;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;

public class AsyncLoadAndBuildStatistics extends
		AsyncTask<Uri, Integer, GameStatistic> {
	private ContentResolver resolver;
	private List<Player> players;
	private List<onStatisticBuildCompleteListener> listeners;
	private GameStatistic finishedStat;
	private final int gameKey;
	
	public AsyncLoadAndBuildStatistics(ContentResolver resolver, List<Player> players, int gameKey) {
		this.resolver = resolver;
		this.players = players;
		this.listeners = new LinkedList<onStatisticBuildCompleteListener>();
		this.gameKey = gameKey;
		if (resolver == null || players == null) {
			throw new NullPointerException("No parameter must be null.");
		} else if (!isGamekeySupported(gameKey)) {
			throw new IllegalArgumentException("Gamekey " + gameKey + " not supported.");
		}
	}
	
	public static boolean isGamekeySupported(int key) {
		 // add every game key that this class supports, if it supports a game the cases must be added to the methods accordingly
		return key == GameKey.TICHU;
	}

	public void addListener(onStatisticBuildCompleteListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}
	
	public boolean removeListener(onStatisticBuildCompleteListener listener) {
		return listeners.remove(listener);
	}
	
	@Override
	protected GameStatistic doInBackground(Uri... uri) {
		List<Game> games = null;
		try {
			switch(gameKey) {
			case GameKey.TICHU:
				games = TichuGame.loadGames(resolver, uri[0], false);
				break;
			}
		} catch(CompressedDataCorruptException e) {
			assert false; // will not throw but ignore all any exceptions
		}
		publishProgress(70);
		if (isCancelled()) {
			return null;
		}
		GameStatisticBuilder builder = null;
		switch(gameKey) {
		case GameKey.TICHU:
			builder = new TichuGameStatisticBuilder(players);
			break;
		}
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
		for (onStatisticBuildCompleteListener listener : listeners) {
			listener.statisticComplete(result);
		}
    }
	
}
