package dan.dit.gameMemo.dataExchange;

import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
/**
 * A egoistic {@link GameDataExchanger}. Does not offer any data and requests
 * all available games. Used to fetch data from a normal GameDataExchanger without
 * sending anything itself.
 * @author Daniel
 *
 */
public class GameDataRequester extends GameDataExchanger {


	public GameDataRequester(ContentResolver resolver,
			ExchangeService service, int gameKey) {
		super(resolver, service, gameKey);
		setOffer(Collections.<Long> emptyList());
	}

	protected List<Long> filterReceivedOffer(List<Long> receivedTimes) {
		return receivedTimes; // request all games
	}

	@Override
	protected void onReceiveGames(String message) {
		// by default do nothing with received games, they are handled elsewhere
	}
}
