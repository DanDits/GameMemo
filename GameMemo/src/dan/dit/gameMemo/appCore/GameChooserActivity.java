package dan.dit.gameMemo.appCore;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothDataExchangeActivity;
import dan.dit.gameMemo.dataExchange.file.FileWriteDataExchangeActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.Game.PlayerRenamedListener;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.RenamePlayerDialogFragment;
import dan.dit.gameMemo.storage.GameStorageHelper;

/**
 * Activity to choose a game from a list of available games. As new games get
 * added to the project, new buttons, spinner or other elements to choose a
 * game(key) will and must be added. <br>
 * Extras: EXTRA_LAST_GAME
 * 
 * @author Daniel
 * 
 */
public class GameChooserActivity extends FragmentActivity implements
		PlayerRenamedListener {

	/**
	 * Can be set by the game activity the user left to return to this activity
	 * to allow special treatment for this game, like displaying it at the top
	 * of all games.
	 */
	private static final String EXTRA_LAST_GAME = "dan.dit.gameMemo.extra_last_game_key";
	
	private int mLastGameKey;
	
	// UI elements to start a specific game
	private ListView mGamesList;
	private GameAdapter mAdapter;
	private SparseArray<CharSequence> mGameDescription = new SparseArray<CharSequence>(GameKey.ALL_GAMES.length);
    private SparseIntArray mGamesCount = new SparseIntArray(GameKey.ALL_GAMES.length);

	/**
	 * Creates an intent to start this activity, which means bringing it to the top of the
	 * activity stack.
	 * @param context The context of the intent.
	 * @param lastGameKey Optional. The key of the last game the user chose which allows special treatment of this game.
	 * @return An intent to show this activity.
	 */
	public static Intent getInstance(Context context, int lastGameKey) {
	    Intent i = new Intent(context, GameChooserActivity.class);
	    if (GameKey.isGameSupported(lastGameKey)) {
	        i.putExtra(GameChooserActivity.EXTRA_LAST_GAME, lastGameKey);
	    }
        return i;
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_chooser);
		mGamesList = (ListView) findViewById(R.id.gamesList);
		mAdapter = new GameAdapter();
		mGamesList.setAdapter(mAdapter);
        processIntent();
		Game.loadAllPlayers(mLastGameKey, this.getApplicationContext());
		initListeners();
	}

	@Override
	public void onNewIntent(Intent intent) {
	    super.onNewIntent(intent);
	    setIntent(intent);
	    processIntent();
	}
	
	private void processIntent() {
        Intent i = getIntent();
        Bundle ext = i.getExtras();
        if (ext != null) {
            mLastGameKey = ext.getInt(EXTRA_LAST_GAME, GameKey.NO_GAME);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
	public void onStart() {
		super.onStart();
		updateDescriptions();
	}
    
    private void updateDescriptions() {
        for (int gameKey : GameKey.ALL_GAMES) {
            updateDescription(gameKey);
        }
    }

	private void updateDescription(int gameKey) {
        setDescription(gameKey, getResources().getString(R.string.game_chooser_game_descr_no_count,
                GameKey.getGameName(gameKey, getResources())));
	    new GameStorageHelper.RequestStoredGamesCountTask(getContentResolver(), new GameStorageHelper.RequestStoredGamesCountTask.Callback() {
            
            @Override
            public void receiveStoredGamesCount(Integer[] gameKeys, Integer[] gamesCount) {
                for (int i = 0; i < gameKeys.length; i++) {
                    mGamesCount.put(gameKeys[i], gamesCount[i]);
                    setDescription(gameKeys[i], getResources().getString(R.string.game_chooser_game_descr,
                        GameKey.getGameName(gameKeys[i], getResources()), gamesCount[i]));
                }
                mAdapter.notifyDataSetChanged();
            }
        }).execute(gameKey);

	}
	
	private void setDescription(int gameKey, CharSequence descr) {
        mGameDescription.put(gameKey, descr);
        mAdapter.notifyDataSetChanged();
	}

	private void initListeners() {
		mGamesList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                startGameActivity(getGameKeyForPosition(position));
            }
        });
	}

	private final Comparator<Integer> GAMEKEY_GAMES_COUNT_COMPARATOR = new Comparator<Integer>() {

        @Override
        public int compare(Integer key1, Integer key2) {
            Integer count1 = mGamesCount.get(key1);
            Integer count2 = mGamesCount.get(key2);
            if (count1 == null) {
                if (count2 == null) {
                    return key1 - key2; // smaller game keys at the start
                }
                return 1; // key2 is more important, so at start
            } else if (count2 == null) {
                return -1; // key1 is more important, so at start
            } else {
                return count2 - count1;
            }
        }
	    
	};
	
	private int getGameKeyForPosition(int pos) {
	    // if we ever get a huge amount of games we probably will need to cache the result of this
	    List<Integer> keys = GameKey.toList(GameKey.ALL_GAMES);
	    Collections.sort(keys, GAMEKEY_GAMES_COUNT_COMPARATOR);
	    return keys.get(pos);
	}
	
	public class GameAdapter extends ArrayAdapter<Integer> {
	    public GameAdapter() {
	        super(GameChooserActivity.this, android.R.layout.simple_spinner_dropdown_item,
	                android.R.id.text1, GameKey.toIntegerArray(GameKey.ALL_GAMES));
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        View view = convertView;
	        if (view == null) {
	            view = getLayoutInflater().inflate(R.layout.game_chooser_game, null);
	        }
	        int gameKey = getGameKeyForPosition(position);
	        TextView descr = (TextView)view.findViewById(R.id.game_descr);
	        GameKey.applyTheme(gameKey, getResources(), view);
            GameKey.applyTheme(gameKey, getResources(), descr);
            descr.setBackgroundResource(0);
	        descr.setText(mGameDescription.get(gameKey));
	        ImageView icon = (ImageView)view.findViewById(R.id.game_icon);
	        icon.setImageResource(GameKey.getGameIconId(gameKey));
	        return view;
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_chooser, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.open_data_exchanger_bluetooth:
			i = BluetoothDataExchangeActivity.newInstance(this,
					GameKey.ALL_GAMES);
			startActivity(i);
			return true;
		case R.id.share:
			i = FileWriteDataExchangeActivity.newInstance(this,
					GameKey.ALL_GAMES, true);
			startActivity(i);
			return true;
		case R.id.rename_player:
			DialogFragment dialog = RenamePlayerDialogFragment.newInstance(
					GameKey.ALL_GAMES, null);
			dialog.show(getSupportFragmentManager(),
					"RenamePlayerDialogFragment");
			return true;
		case R.id.about:
			showAboutDialog();
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	private void showAboutDialog() {
		String versionName = "";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(),
					0).versionName;
		} catch (NameNotFoundException e) {
			// will not happen since we ask for own own package which exists
		}
		new AlertDialog.Builder(this)
				.setTitle(getResources().getString(R.string.about))
				.setMessage(
						getResources().getString(R.string.about_summary,
								versionName))
				.setIcon(android.R.drawable.ic_dialog_info)
				.setNeutralButton(android.R.string.ok, null).show();
	}

	private void startGameActivity(int gameKey) {
		Intent i = new Intent(GameChooserActivity.this,
				GameKey.getGamesActivity(gameKey));
		i.putExtra(GameKey.EXTRA_GAMEKEY, gameKey);
		startActivity(i);
	}

	@Override
	public void playerRenamed(String oldName, String newName, boolean success) {
		if (success) {
			Toast.makeText(this,
					getResources().getString(R.string.rename_success),
					Toast.LENGTH_SHORT).show();
		}
	}
}
