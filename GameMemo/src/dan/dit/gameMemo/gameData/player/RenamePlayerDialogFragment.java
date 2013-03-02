package dan.dit.gameMemo.gameData.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
/**
 * This dialog fragment enables the user to rename a player from a list of players to any
 * new valid player name. After renaming all references to player variables should be considered
 * out of synch and its better to get the player from the pool.<br>
 * Supports renaming a player for a certain game only or for all games if the extra RENAME_FOR_ALL_GAMES 
 * is <code>true</code>.<br>
 * If the hosting activity implements the {@link RenamePlayerCallback} interface then on user confirmation 
 * the callback onRenameSuccess will be invoked with the new player and the name of the
 * (old) player that was renamed.<br>
 * The new player will be null if renaming for all games. Do not make any assumptions
 * for the new player being really 'new' or unique in the given or any pool.
 * @author Daniel
 *
 */
public class RenamePlayerDialogFragment extends DialogFragment {
	public static final String EXTRA_RENAME_FOR_ALL_GAMES = "dan.dit.gameMemo.EXTRA_RENAME_FOR_ALL_GAMES";
	private EditText mNewName;
	private ArrayAdapter<Player> mPlayersAdapter;
	private Spinner mPlayers;
	private int mGameKey;
	private boolean mRenameForAll;
	private RenamePlayerCallback mCallback;
	
	public interface RenamePlayerCallback {
		void onRenameSuccess(Player newPlayer, String oldName);
	}
	
	public RenamePlayerDialogFragment() {
		super();
	}
	
	public void wantsToRename(Player selected, String pNewName) {
		String newName = pNewName.trim();
		if (selected != null && Player.isValidPlayerName(newName)) {
			if (!selected.getName().equalsIgnoreCase(newName) 
					&& GameKey.getPool(mGameKey).contains(newName)) {
				// there already is a player with the new name (and its not the current one with other case letters)
				warnAndConsultAboutNameConflict(new Handler(), getActivity(), selected, newName);
			} else {
				performRenaming(new Handler(), getActivity(), selected, newName);
			}
		} else {
			Toast.makeText(getActivity(), getResources().getString(R.string.rename_failure), Toast.LENGTH_SHORT).show();
		}
	}

	private void warnAndConsultAboutNameConflict(final Handler handler, final Context context, final Player selected, final String newName) {
		new AlertDialog.Builder(getActivity())
			.setTitle(getResources().getString(R.string.rename_conflict_title))
			.setMessage(getResources().getString(R.string.rename_conflict))
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

			    public void onClick(DialogInterface dialog, int whichButton) {
			    	performRenaming(handler, context, selected, newName);
			    }})
			 .setNegativeButton(android.R.string.no, null).show();
	}
	
	private void performRenaming(final Handler toaster, final Context context, final Player toRename, final String newName) {
		// perform asynch renaming, but post success or failure message on UI thread and update name in detail fragment
		new Thread() {
			@Override
			public void run() {
				Runnable action = new Runnable() {
					@Override
					public void run() {
						if (mCallback != null) {
							mCallback.onRenameSuccess(mRenameForAll ? null : GameKey.getPool(mGameKey).populatePlayer(newName), toRename.getName());
						}
						Toast.makeText(context, context.getResources().getString(R.string.rename_success), Toast.LENGTH_SHORT).show();
					}
				};
				// renaming does not fail here since new name is a valid player name and there are no conflict checks done
				boolean success = false;
				if (mRenameForAll) {
					success = CombinedPool.ALL_POOLS.renamePlayer(GameKey.ALL_GAMES, context.getContentResolver(), toRename, newName) > 0;
				} else {
					success = (GameKey.getPool(mGameKey).renamePlayer(mGameKey, context.getContentResolver(), toRename, newName) != null);
				}
				if (success) {
					toaster.post(action);
				}
				
			}
		}.start();
	}
	   @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
		   Bundle args = getArguments();
		   if (savedInstanceState == null) {
			   mGameKey = args.getInt(GameKey.EXTRA_GAMEKEY);
			   mRenameForAll = args.getBoolean(EXTRA_RENAME_FOR_ALL_GAMES);
		   } else {
			   mGameKey = savedInstanceState.getInt(GameKey.EXTRA_GAMEKEY);
			   mRenameForAll = savedInstanceState.getBoolean(EXTRA_RENAME_FOR_ALL_GAMES);
		   }
		   View baseView = getActivity().getLayoutInflater().inflate(R.layout.rename_player, null);
	        mNewName = (EditText) baseView.findViewById(R.id.rename_new_name);
	        mPlayers = (Spinner) baseView.findViewById(R.id.rename_select_players);
			mPlayersAdapter = mRenameForAll ? CombinedPool.ALL_POOLS.makeAdapter(getActivity()) : GameKey.getPool(mGameKey).makeAdapter(getActivity());
	        mPlayers.setAdapter(mPlayersAdapter);
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setIcon(mRenameForAll ? Game.GENERAL_GAME_ICON : GameKey.getGameIconId(mGameKey))
	        .setTitle(getResources().getString(R.string.rename_player) + (mRenameForAll ? "" : (" (" + GameKey.getGameName(mGameKey) + ")")))
	        		.setView(baseView)
	               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   Object selected = mPlayers.getSelectedItem();
	                	   if (selected != null) {
	                		   wantsToRename((Player) selected, mNewName.getText().toString());
	                	   }
	                   }
	               })
	               .setNegativeButton(android.R.string.no, null);
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	   
	   @Override
	   public void onStart() {
		   super.onStart();
		   mPlayersAdapter.sort(Player.NAME_COMPARATOR);
	   }
	  
	   @Override
	   public void onSaveInstanceState(Bundle outState) {
		   super.onSaveInstanceState(outState);
		   outState.putBoolean(EXTRA_RENAME_FOR_ALL_GAMES, mRenameForAll);
		   outState.putInt(GameKey.EXTRA_GAMEKEY, mGameKey);
	   }
	   
	   @Override
	   public void onAttach(Activity activity) {
		   super.onAttach(activity);
		   if (activity instanceof RenamePlayerCallback) {
			   mCallback = (RenamePlayerCallback) activity;
		   }
	   }
}
