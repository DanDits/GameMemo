package dan.dit.gameMemo.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import dan.dit.gameMemo.gameData.player.PlayerColors;
import dan.dit.gameMemo.util.ColorPickerView.OnColorChangedListener;

public class ColorPickerDialog extends DialogFragment {
	public static final String EXTRA_COLOR = "dan.dit.gameMemo.EXTRA_START_COLOR";
	private OnColorChangedListener mCallback;
	private ColorPickerView mColorView;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallback = (OnColorChangedListener) activity;
	}
	
	  @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
		  	ColorPickerView baseView = new ColorPickerView(getActivity());
		  	mColorView = baseView;
		  	if (savedInstanceState == null) {
		  		baseView.setColor(getArguments() != null ? getArguments().getInt(EXTRA_COLOR, PlayerColors.DEFAULT_COLOR) : PlayerColors.DEFAULT_COLOR);
		  	} else {
		  		baseView.setColor(savedInstanceState.getInt(EXTRA_COLOR, PlayerColors.DEFAULT_COLOR));
		  	}
		  	// Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setView(baseView)
	               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   onConfirmation();
	                   }
	               })
	               .setNegativeButton(android.R.string.no, null);
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	   
	  @Override
	  public void onSaveInstanceState(Bundle savedInstanceState) {
		  super.onSaveInstanceState(savedInstanceState);
		  savedInstanceState.putInt(EXTRA_COLOR, mColorView.getColor());
	  }
	  
	   private void onConfirmation() {
		  mCallback.onColorChanged(mColorView.getColor());
	   }
}
