package dan.dit.gameMemo.gameData.player;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import dan.dit.gameMemo.R;

public class ChoosePlayerDialogFragment extends DialogFragment {
	private AutoCompleteTextView mNewName;
	private ArrayAdapter<Player> mPlayersAdapter;
	private Spinner mPlayers;
	private PlayerPool mPool;
	private ChoosePlayerDialogListener mListener;
	
	public interface ChoosePlayerDialogListener {
		PlayerPool getPool();
		List<Player> toFilter();
		void playerChosen(Player chosen);
	}
	
	   @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
		   View baseView = getActivity().getLayoutInflater().inflate(R.layout.choose_player, null);
	        mNewName = (AutoCompleteTextView) baseView.findViewById(R.id.choose_player_new_name);
	        mPlayers = (Spinner) baseView.findViewById(R.id.choose_player_list);
			mPool = mListener.getPool();
			mPlayersAdapter = mPool.makeAdapter(getActivity(), mListener.toFilter(), false);
	        mPlayers.setAdapter(mPlayersAdapter);
	        mNewName.setAdapter(mPool.makeAdapter(getActivity(), mListener.toFilter(), true));
	        mNewName.setThreshold(1);
	        mNewName.setOnEditorActionListener(new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
						onConfirmation();
						getDialog().dismiss();
						//InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
						//imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
						return true;
					}
					return false;
				}
			});
	        mNewName.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable arg0) {}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1,
						int arg2, int arg3) {}

				@Override
				public void onTextChanged(CharSequence arg0, int arg1,
						int arg2, int arg3) {
					if (TextUtils.isEmpty(mNewName.getText())) {
						mPlayers.setVisibility(View.VISIBLE);
					} else {
						mPlayers.setVisibility(View.GONE);
					}
				}
	        	
	        });
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setTitle(R.string.select_player)
	        		.setView(baseView)
	               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   onConfirmation();
	                   }
	               })
	               .setNegativeButton(android.R.string.no, null);
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	   
	   private void onConfirmation() {
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
	   
	   @Override
	   public void onStart() {
		   super.onStart();
		   mPlayersAdapter.sort(Player.NAME_COMPARATOR);
		   if (mNewName.hasFocus() && getResources().getConfiguration().orientation ==  Configuration.ORIENTATION_PORTRAIT) {
				getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		   }
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
