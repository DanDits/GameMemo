package dan.dit.gameMemo.gameData.player;

import java.util.Collection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.Game.PlayerRenamedListener;
import dan.dit.gameMemo.gameData.game.GameKey;
/**
 * This dialog fragment enables the user to rename a player from a list of players to any
 * new valid player name. After renaming all references to player variables should be considered
 * out of synch and its better to get the player from the pool.<br>
 * Supports renaming a player for a certain game only. Any instance of this game must be invalidated and closed
 * as it will not change the player.<br>
 * If the hosting activity implements the {@link RenamePlayerCallback} interface then on user confirmation 
 * the callback onRenameSuccess will be invoked with the new player and the name of the
 * (old) player that was renamed.<br>
 * The new player will be null if renaming for all games. Do not make any assumptions
 * for the new player being really 'new' or unique in the given or any pool.
 * @author Daniel
 *
 */
public class RenamePlayerDialogFragment extends DialogFragment  {
	private static final String EXTRA_RENAME_IN_GAME_IDS = "dan.dit.gameMemo.EXTRA_RENAME_IN_GAME_IDS";
	private PlayerRenamedListener mListener;
	private EditText mNewName;
	private ArrayAdapter<Player> mPlayersAdapter;
	private Spinner mPlayers;
	private int[] mGameKey;
	private long[] mRenameInGameIds;

	public static DialogFragment newInstance(int[] gameKeys, long[] gameIdsToRename) {
		RenamePlayerDialogFragment dialog = new RenamePlayerDialogFragment();
        Bundle args = new Bundle();
        args.putIntArray(GameKey.EXTRA_GAMEKEY, gameKeys);
        args.putLongArray(RenamePlayerDialogFragment.EXTRA_RENAME_IN_GAME_IDS, gameIdsToRename);
        dialog.setArguments(args);
        return dialog;
	}
	
	private int containedInPoolCount(String name) {
		int count = 0;
		for (int key : mGameKey) {
			if (GameKey.getPool(key).contains(name)) {
				count++;
			}
		}
		return count;
	}
	
	public void wantsToRename(Player selected, String pNewName) {
		String newName = pNewName.trim();
		if (selected != null && Player.isValidPlayerName(newName)) {
			if (!selected.getName().equalsIgnoreCase(newName)) {
				int containedCount = containedInPoolCount(newName);
				if (containedCount > 0) {
					// there already is a player with the new name (and its not the current one with other case letters)
					warnAndConsultAboutNameConflict(getActivity(), selected, newName, containedCount);
				} else {
					performRenaming(getActivity(), selected, newName);
				}
			} else {
				performRenaming(getActivity(), selected, newName);
			}
		} else {
			Toast.makeText(getActivity(), getResources().getString(R.string.rename_failure), Toast.LENGTH_SHORT).show();
		}
	}

	private void warnAndConsultAboutNameConflict(final Context context, final Player selected, final String newName, int containedInGamesCount) {
		new AlertDialog.Builder(getActivity())
			.setTitle(getResources().getQuantityString(R.plurals.rename_conflict_title, containedInGamesCount, containedInGamesCount))
			.setMessage(getResources().getString(R.string.rename_conflict, newName, selected.getName(), newName))
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

			    public void onClick(DialogInterface dialog, int whichButton) {
			    	performRenaming(context, selected, newName);
			    }})
			 .setNegativeButton(android.R.string.no, null).show();
	}
	
	private void performRenaming(Context context, Player toRename, String newName) {
		for (int key : mGameKey) {
			Game.rename(key, context.getContentResolver(), toRename, newName, mRenameInGameIds, mListener);
		}
	}
	
	   @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
		   Bundle args = getArguments();
		   mGameKey = args.getIntArray(GameKey.EXTRA_GAMEKEY);
		   //use the given gamekey to only rename the given ids, if empty then rename for all ids of this game
		   mRenameInGameIds = args.getLongArray(EXTRA_RENAME_IN_GAME_IDS);
		   if (mRenameInGameIds == null) {
			   mRenameInGameIds = new long[0];
		   }
		   View baseView = getActivity().getLayoutInflater().inflate(R.layout.rename_player, null);
	        mNewName = (EditText) baseView.findViewById(R.id.rename_new_name);
	        mPlayers = (Spinner) baseView.findViewById(R.id.rename_select_players);
	        Collection<Player> allPlayers = GameKey.getAllPlayers();
	        if (allPlayers.size() == 0) {
				Toast.makeText(getActivity(), getResources().getString(R.string.rename_no_players), Toast.LENGTH_SHORT).show();
				return new AlertDialog.Builder(getActivity()).create();
	        } else {
				mPlayersAdapter = new PlayerAdapter(false, getActivity(), allPlayers);
				for (Player p : allPlayers) {
					mPlayersAdapter.add(p);
				}
		        mPlayers.setAdapter(mPlayersAdapter);
		        // Use the Builder class for convenient dialog construction
		        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		        builder.setIcon(getDialogIcon())
		        .setTitle(getDialogTitle())
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
	    }
	
	   @Override
	   public void onStart() {
		   super.onStart();
		   if (mPlayersAdapter != null) {
			   mPlayersAdapter.sort(Player.NAME_COMPARATOR);
		   } else {
			   dismiss();
		   }
	   }
	   
	   private int getDialogIcon() {
		   if (mGameKey.length == 1) {
			   return GameKey.getGameIconId(mGameKey[0]);
		   } else {
			   return 0;
		   }
	   }
	   private String getDialogTitle() {
		   StringBuilder builder = new StringBuilder();
		   builder.append(getResources().getString(R.string.rename_player));
		   builder.append("\n(");
		   if (mGameKey.length == 1) {
			   if (mRenameInGameIds.length == 0) {
				   builder.append(getResources().getString(R.string.single_rename_in_games_all, GameKey.getGameName(mGameKey[0])));
			   } else {
				   builder.append(getResources().getQuantityString(R.plurals.single_rename_in_games_count, mRenameInGameIds.length, mRenameInGameIds.length, GameKey.getGameName(mGameKey[0])));
			   }
		   } else {
			   builder.append(getResources().getString(R.string.rename_in_games, mGameKey.length));
		   }
		   builder.append(")");
		   return builder.toString();
	   }
	   
	   @Override
	   public void onAttach(Activity activity) {
		   super.onAttach(activity);
		   mListener = (PlayerRenamedListener) activity;
	   }

}
