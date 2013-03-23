package dan.dit.gameMemo.gameData.player;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compression.Compressor;

public class Player extends PlayerTeam {	
	public static final Comparator<Player> NAME_COMPARATOR = new Comparator<Player>() {
		@Override
		public int compare(Player p1, Player p2) {
			return p1.getName().toLowerCase(Locale.US).compareTo(p2.getName().toLowerCase(Locale.US));
		}
	};
	private String name;
	private List<Player> singletonSelfList;
	
	public Player(String name) {
		if (!isValidPlayerName(name)) {
			throw new IllegalArgumentException("Illegal player name: " + name);
		}
		this.name = name.trim();
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public int hashCode() {
		return name.toLowerCase(Locale.US).hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Player) {
			return name.equalsIgnoreCase(((Player) other).name);
		} else {
			return super.equals(other);
		}
	}
	
	public static boolean isValidPlayerName(String name) {
		return name != null && name.length() > 0 && name.trim().length() > 0;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public Player getFirst() {
		return this;
	}

	@Override
	public List<Player> getPlayers() {
		if (singletonSelfList == null) {
			// getPlayers() makes no guarantee to always return the same list, so no need to synchronize here
			singletonSelfList = Collections.singletonList(this);
		}
		return singletonSelfList;
	}
	
	@Override
	public String compress() {
		return name;
	}

	@Override
	public int getPlayerCount() {
		return 1;
	}

	@Override
	public boolean contains(Player p) {
		return equals(p);
	}
	
	boolean rename(int gameKey, ContentResolver resolver, String pNewName) {
		String oldName = name;
		String newName = pNewName.trim();
		if (!Player.isValidPlayerName(newName)) {
			return false;
		}
		
		String[] projection = { GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_PLAYERS};
		// get all those games that at least contain a player which has the old name as a substring
		StringBuilder where = new StringBuilder();
		where.append(GameStorageHelper.COLUMN_PLAYERS);
		where.append(" like ?");
		String[] selectionArgs = new String[1];
		selectionArgs[0] = '%' + oldName + '%';
		Cursor cursor = resolver.query(GameStorageHelper.getUriAllItems(gameKey), projection, where.toString(), selectionArgs,
				null);
		if (cursor != null) {
			cursor.moveToFirst();
			// check all candidates: if this player actually participated in this game, change the player data
			while (!cursor.isAfterLast()) {
				Compressor playersData = new Compressor(cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_PLAYERS)));
				Compressor newPlayersdata = new Compressor(playersData.getSize());
				boolean foundChange = false;
				for (String playerName : playersData) {
					if (playerName.equalsIgnoreCase(oldName) || playerName.equalsIgnoreCase(newName)) {
						foundChange = true;
						newPlayersdata.appendData(newName);
					} else {
						newPlayersdata.appendData(playerName);
					}
				}
				if (foundChange) {
					ContentValues values = new ContentValues();
					values.put(GameStorageHelper.COLUMN_PLAYERS, newPlayersdata.compress());
					long id = cursor.getInt(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ID));
					Uri uri = GameStorageHelper.getUri(gameKey, id);
					if (uri != null) {
						resolver.update(uri, values, null, null);
					}
				}
				cursor.moveToNext();
			}
			cursor.close();
		}
		this.name = newName;
		return true;
	}

	public static void loadPlayers(int gameKey, PlayerPool pool, ContentResolver resolver) {
		String[] projection = {GameStorageHelper.COLUMN_PLAYERS};
		Cursor cursor = resolver.query(GameStorageHelper.getUriAllItems(gameKey), projection, null, null,
				null);
		if (cursor != null) {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Compressor playersData = new Compressor(cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_PLAYERS)));
				for (String playerName : playersData) {
					if (Player.isValidPlayerName(playerName)) {
						pool.populatePlayer(playerName);
					}
				}
				cursor.moveToNext();
			}
			cursor.close();
		}
	}

	void adapterNameLetterCase(String newName) {
		String trimmedNewName = newName.trim();
		if (!name.equalsIgnoreCase(trimmedNewName)) {
			return;
		}
		name = trimmedNewName;
	}
}
