package dan.dit.gameMemo.gameData.player;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import dan.dit.gameMemo.R;

public class ChoosePlayerDialogFragment extends DialogFragment {
	private EditText mNewName;
	private ArrayAdapter<Player> mPlayersAdapter;
	private Spinner mPlayers;
	private PlayerPool mPool;
	private ChoosePlayerDialogListener mListener;
	
	public interface ChoosePlayerDialogListener {
		PlayerPool getPool();
		List<Player> toFilter();
		void playerChosen(Player chosen);
	}
	
	public ChoosePlayerDialogFragment() {
		super();
	}
	
	   @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
		   View baseView = getActivity().getLayoutInflater().inflate(R.layout.choose_player, null);
	        mNewName = (EditText) baseView.findViewById(R.id.choose_player_new_name);
	        mPlayers = (Spinner) baseView.findViewById(R.id.choose_player_list);
			mPool = mListener.getPool();
			mPlayersAdapter = mPool.makeAdapter(getActivity(), mListener.toFilter());
	        mPlayers.setAdapter(mPlayersAdapter);
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setTitle(R.string.game_select_player)
	        		.setView(baseView)
	               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   String newPlayerName = mNewName.getText().toString();
	                	   if (Player.isValidPlayerName(newPlayerName)) {
	                		   mListener.playerChosen(mPool.populatePlayer(newPlayerName));
	                	   } else {
		                	   Object selected = mPlayers.getSelectedItem();
		                	   if (selected != null && selected instanceof Player) {
		                		   mListener.playerChosen((Player) selected);
		                	   }
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
	   public void onAttach(Activity activity) {
		   super.onAttach(activity);
	        try {
	            // Instantiate the RenamePlayerDialogListener so we can send events to the host
	            mListener = (ChoosePlayerDialogListener) activity;
	        } catch (ClassCastException e) {
	            // The activity doesn't implement the interface, throw exception
	            throw new ClassCastException(activity.toString()
	                    + " must implement ChoosePlayerDialogListener");
	        }
	   }
}
