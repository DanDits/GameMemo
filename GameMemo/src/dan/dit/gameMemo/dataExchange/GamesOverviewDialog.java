package dan.dit.gameMemo.dataExchange;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;


public class GamesOverviewDialog extends DialogFragment {
	private GamesOverviewDialogCallback mCallback;
	private ListView mListView;
	private GamesAdapter mGamesAdapter;
	private List<Integer> mCurrentlySelectedGames;
	private List<Integer> mAllGamesList;
	
	public interface GamesOverviewDialogCallback {
		GamesExchangeManager getManager();
	}
	
	 @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
		   View baseView = getActivity().getLayoutInflater().inflate(R.layout.games_exchange_overview_list, null);
		   mCurrentlySelectedGames = new LinkedList<Integer>();
		   for (int selectedKey : mCallback.getManager().getSelectedGames()) {
			   mCurrentlySelectedGames.add(selectedKey);
		   }
		   mListView = (ListView) baseView.findViewById(R.id.games_exchange_overview_list);
		   mAllGamesList = new ArrayList<Integer>(GameKey.ALL_GAMES.length);
		   for (int gameKey : GameKey.ALL_GAMES) {
			   mAllGamesList.add(gameKey);
		   }
		   mGamesAdapter = new GamesAdapter(getActivity(), R.layout.exchange_game_state);
	       mListView.setAdapter(mGamesAdapter);
	       mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				CheckBox checker = (CheckBox) v.findViewById(R.id.gameSelected);
				int gameKey = mAllGamesList.get(position);
				if (mCurrentlySelectedGames.contains(gameKey)) {
					// game is currently selected
					checker.setChecked(false);
					mCurrentlySelectedGames.remove(Integer.valueOf(gameKey));
				} else {
					// not selected
					checker.setChecked(true);
					mCurrentlySelectedGames.add(Integer.valueOf(gameKey));
				}
			}
	    	   
	       });

	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setTitle(R.string.games_selection_title)
	        		.setView(baseView)
	               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   onConfirmation();
	                   }
	               })
	               .setNegativeButton(android.R.string.no, null)
	               .setOnCancelListener(new OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
	                	onConfirmation();
					}
				});
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	 
	 
	 private void onConfirmation() {
		 int[] selectedGames = new int[mCurrentlySelectedGames.size()];
		 int index = 0;
		 for (int key : mCurrentlySelectedGames) {
			 selectedGames[index++] = key;
		 }
		 Log.d("Tichu", "onConfirmation of gamesoverviewdialog with currently selected keys: " + mCurrentlySelectedGames);
		 mCallback.getManager().setSelectedGames(selectedGames);
	 }
	   
	 @Override
	   public void onAttach(Activity activity) {
		   super.onAttach(activity);
	        try {
	            // Instantiate the GamesOverviewDialogCallback so we can get the manager
	        	mCallback = (GamesOverviewDialogCallback) activity;
	        } catch (ClassCastException e) {
	            // The activity doesn't implement the interface, throw exception
	            throw new ClassCastException(activity.toString()
	                    + " must implement GamesOverviewDialogCallback");
	        }
	   }
	   
	public void notifyDataSetChanged() {
		mGamesAdapter.notifyDataSetChanged();
	}
	
	private class GamesAdapter extends ArrayAdapter<Integer> {
		private int mLayoutResourceId;
		private LayoutInflater mInflater;
		public GamesAdapter(Context context, int layoutResourceId) {
			super(context, layoutResourceId, mAllGamesList);
			mLayoutResourceId = layoutResourceId;
			mInflater = ((Activity)context).getLayoutInflater();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = mInflater.inflate(mLayoutResourceId, parent, false);
			}
			int gameKey = mAllGamesList.get(position);
			GameDataExchanger exchanger = mCallback.getManager().getGameDataExchanger(gameKey);
			// icon
			ImageView icon = (ImageView) row.findViewById(R.id.gameIcon);
			icon.setImageResource(GameKey.getGameIconId(gameKey));
			// selection checkbox
			CheckBox checker = (CheckBox) row.findViewById(R.id.gameSelected);
			checker.setText(GameKey.getGameName(gameKey));
			checker.setClickable(false);
			checker.setFocusable(false);
			checker.setChecked(mCurrentlySelectedGames.contains(Integer.valueOf(gameKey)));
			// exchange state
			TextView state = (TextView) row.findViewById(R.id.exchangeState);
			state.setText("");
			if (exchanger != null) {
				if (exchanger.isClosed()) {
					Resources res = getActivity().getResources();
					StringBuilder stateText = new StringBuilder();
					if (!exchanger.exchangeFinishedSuccessfully()) {
						stateText.append(res.getString(R.string.data_exchange_status_synchronizing_failed));
						stateText.append(' ');
					}
					stateText.append(String.format(res.getString(R.string.data_exchange_games_received_sent), 
							exchanger.getGamesReceivedCount(), exchanger.getGamesSentCount()));
					state.setText(stateText);
					
				}
			}
			return row;
		}
		
	}
}
