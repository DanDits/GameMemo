package dan.dit.gameMemo.appCore;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.SportGame;

public abstract class SportGameDetailFragment extends GameDetailFragment {
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.location:
            askForLocation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // requires a valid SportGame for getGame()
    private void askForLocation() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final AutoCompleteTextView input = new AutoCompleteTextView(getActivity());
        final int VIEW_ID = 1;
        input.setId(VIEW_ID);        
        input.setText(((SportGame) getGame()).getLocation());
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(R.string.sport_game_location_hint);
        input.setThreshold(1);
        input.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, SportGame.getLocations(getGameKey())));
        alert.setView(input)
        .setIcon(R.drawable.location)
        .setTitle(R.string.sport_game_location)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                Editable value = input.getText();
                ((SportGame) getGame()).setLocation(value.toString());
            }
        })
        .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .create()
        .show();
    }

}
