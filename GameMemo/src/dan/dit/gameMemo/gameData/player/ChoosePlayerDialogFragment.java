package dan.dit.gameMemo.gameData.player;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.util.ColorPickerView;
import dan.dit.gameMemo.util.ColorPickerView.OnColorChangedListener;

public class ChoosePlayerDialogFragment extends DialogFragment {
	public static final String EXTRA_PLAYER_CALLBACK_ARG = "dan.dit.gameMemo.PLAYER_CALLBACK_ARG"; // optional int that is supplied to playerChosen() and onPlayerColorChanged, else -1
	// optional String, player name of player to select in spinner, will be displayed even if contained in toFilter()
	public static final String EXTRA_PLAYER_SELECTION_ON_START = "dan.dit.gameMemo.PLAYER_SELECTION_ON_START"; 
	public static final String EXTRA_ALLOW_COLOR_CHOOSING = "dan.dit.gameMemo.ALLOW_COLOR_CHOOSING"; // optional, default false
	private AutoCompleteTextView mNewName;
	private PlayerAdapter mNewNameAdapter;
	private PlayerAdapter mPlayersAdapter;
	private Spinner mPlayers;
	private ChoosePlayerDialogListener mListener;
	private int mCallbackArg;
	private boolean mAllowColorChoosing;
	private ColorPickerView mColorPicker;
	
	public interface ChoosePlayerDialogListener {
		PlayerPool getPool();
		List<Player> toFilter();
		void playerChosen(int arg, Player chosen);
		void onPlayerColorChanged(int arg, Player concernedPlayer); // player color can be changed of any player in the pool
	}
	
	public static ChoosePlayerDialogFragment newInstance(int arg, Player selectPlayer, boolean allowColorChoosing) {
		ChoosePlayerDialogFragment frag = new ChoosePlayerDialogFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_PLAYER_CALLBACK_ARG, arg);
		if (selectPlayer != null) {
			args.putString(EXTRA_PLAYER_SELECTION_ON_START, selectPlayer.getName());
		}
		args.putBoolean(EXTRA_ALLOW_COLOR_CHOOSING, allowColorChoosing);
		frag.setArguments(args);
		return frag;
	}
	
	   @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
		   Bundle args = getArguments();
		   PlayerPool pool = mListener.getPool();
		   mCallbackArg = -1;
		   Player selectPlayer = null;
		   if (args != null) {
			   mCallbackArg = args.getInt(EXTRA_PLAYER_CALLBACK_ARG, -1);
			   String playerName = args.getString(EXTRA_PLAYER_SELECTION_ON_START);
			   if (Player.isValidPlayerName(playerName)) {
				   selectPlayer = pool.populatePlayer(playerName);
			   }
			   mAllowColorChoosing = args.getBoolean(EXTRA_ALLOW_COLOR_CHOOSING);
		   }
		   View baseView = getActivity().getLayoutInflater().inflate(R.layout.choose_player, null);
	        mNewName = (AutoCompleteTextView) baseView.findViewById(R.id.choose_player_new_name);
	        mNewName.setTextColor(Color.BLACK);
	        mPlayers = (Spinner) baseView.findViewById(R.id.choose_player_list);
	        mPlayers.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int pos, long id) {
					setShowSpinner(true);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});
			mPlayersAdapter = pool.makeAdapter(getActivity(), getSpinnerFilter(selectPlayer), false);
	        mPlayers.setAdapter(mPlayersAdapter);
	        if (selectPlayer != null) {
	        	mPlayers.setSelection(mPlayersAdapter.getPosition(selectPlayer));
	        }
	        mNewNameAdapter = pool.makeAdapter(getActivity(), mListener.toFilter(), true);
	        mNewName.setAdapter(mNewNameAdapter);
	        mNewName.setThreshold(1);
	        if (mAllowColorChoosing) {
	        	mColorPicker = (ColorPickerView) baseView.findViewById(R.id.color_picker);
	        	Player sel = getSelectedPlayer();
	        	if (mPlayers.getVisibility() == View.VISIBLE && sel != null) {
	        		mColorPicker.setColor(sel.getColor());	        		
	        	} else {
	        		mColorPicker.setColor(PlayerColors.DEFAULT_COLOR);
	        	}
	        	mColorPicker.setOnColorChangedListener(new OnColorChangedListener() {
					
					@Override
					public void onColorChanged(int color) {
						if (mPlayers.getVisibility() == View.VISIBLE) {
							Player sel = getSelectedPlayer();
							if (sel != null) {
								sel.setColor(color);
								mListener.onPlayerColorChanged(mCallbackArg, sel);
							}
						}
					}
				});
	        	Button mToggleShowColorChooser = (Button) baseView.findViewById(R.id.choose_color);
	        	mToggleShowColorChooser.setVisibility(View.VISIBLE);
	        	mToggleShowColorChooser.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						toggleShowColorChooser();
					}
	        		
	        	});
	        }
	        mNewName.setOnEditorActionListener(new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
						onConfirmation();
						getDialog().dismiss();
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
					setShowSpinner(TextUtils.isEmpty(mNewName.getText()) && (mPlayersAdapter == null || !mPlayersAdapter.isEmpty()));
					if (mColorPicker != null && mListener.getPool().contains(mNewName.getText().toString())) {
			        	Player sel = mListener.getPool().populatePlayer(mNewName.getText().toString());
			        	mColorPicker.setColor(sel.getColor());	        		
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
	   
	private void toggleShowColorChooser() {
		if (mAllowColorChoosing) {
			if (mColorPicker.getVisibility() == View.VISIBLE) {
				mColorPicker.setVisibility(View.GONE);
			} else {
				mColorPicker.setVisibility(View.VISIBLE);
			}
		}
	}

	private void onConfirmation() {
		   String newPlayerName = mNewName.getText().toString();
    	   if (Player.isValidPlayerName(newPlayerName)) {
    		   Player p = mListener.getPool().populatePlayer(newPlayerName);
    		   if (mAllowColorChoosing) {
    			   p.setColor(mColorPicker.getColor());
    		   }
    		   mListener.playerChosen(mCallbackArg, p);
    	   } else {
        	   Object selected = mPlayers.getSelectedItem();
        	   if (selected != null && selected instanceof Player) {
        		   mListener.playerChosen(mCallbackArg, (Player) selected);
        	   }
    	   }
	   }
	   
	   @Override
	   public void onResume() {
		   super.onResume();
		   if (mNewName.hasFocus() && getResources().getConfiguration().orientation ==  Configuration.ORIENTATION_PORTRAIT && !mAllowColorChoosing) {
				getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		   }
		   mPlayersAdapter.addFilterPlayers(getSpinnerFilter(getSelectedPlayer()));
		   mNewNameAdapter.addFilterPlayers(mListener.toFilter());
		   if (mPlayersAdapter.isEmpty()) {
			   setShowSpinner(false);
		   } else {
			   setShowSpinner(true);
			   mPlayersAdapter.sort(Player.NAME_COMPARATOR);
		   }
	   }
	   
	private void setShowSpinner(boolean show) {
		if (show) {
			if (mPlayers.getVisibility() != View.VISIBLE) {
				hideKeyboard();
				mPlayers.setVisibility(View.VISIBLE);
			}
			if (mColorPicker != null) {
				Player sel = getSelectedPlayer();
				if (sel != null) {
					mColorPicker.setColor(sel.getColor());
				}
			}
		} else {
			mPlayers.setVisibility(View.GONE);
			if (mColorPicker != null) {
				mColorPicker.setColor(PlayerColors.DEFAULT_COLOR);
			}
		}
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)mNewName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mNewName.getWindowToken(), 0);
	}

	private List<Player> getSpinnerFilter(Player keep) {
		    List<Player> spinnerFilter = mListener.toFilter();
		    if (keep != null) {
		    	spinnerFilter.remove(keep);
		    }
		    return spinnerFilter;
	   }
	   
	   @Override
	   public void onSaveInstanceState(Bundle savedInstanceState) {
		   super.onSaveInstanceState(savedInstanceState);
		   Bundle args = getArguments();
		   if (args != null) {
			   Player toSelect = getSelectedPlayer();
			   if (toSelect == null) {
				   args.remove(EXTRA_PLAYER_SELECTION_ON_START);
			   } else {
				   args.putString(EXTRA_PLAYER_SELECTION_ON_START, toSelect.getName());
			   }
		   }
	   }
	   
	   private Player getSelectedPlayer() {
		   Object sel = mPlayers.getSelectedItem();
		   if (sel != null && sel instanceof Player) {
			   return (Player) sel;
		   }
		   return null;
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
