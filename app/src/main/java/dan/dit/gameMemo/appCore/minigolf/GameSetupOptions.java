package dan.dit.gameMemo.appCore.minigolf;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupOptionsController;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.SportGame;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfGame;

public class GameSetupOptions extends GameSetupOptionsController {
    
    public static class Builder {
        static final String EXTRA_OPTION_LOCATION = "dan.dit.gameMemo.EXTRA_OPTION_LOCATION"; // String
        static final String EXTRA_OPTION_DEFAULT_LANES_COUNT = "dan.dit.gameMemo.EXTRA_OPTION_LANES_COUNT"; // Integer
        private Bundle mParameters = new Bundle();
        
        public Builder() {
            setLocation("");
            setDefaultLanesCount(MinigolfGame.DEFAULT_LANES_COUNT);
        }
        
        public void setDefaultLanesCount(int lanesCount) {
            mParameters.putInt(EXTRA_OPTION_DEFAULT_LANES_COUNT, lanesCount);
        }

        public void setLocation(String value) {
            mParameters.putString(EXTRA_OPTION_LOCATION, value);
        }
        
        public Bundle build() {
            return mParameters;
        }
    }
    
    private AutoCompleteTextView mLocation;
    private SeekBar mDefaultLanes;
    private TextView mDefaultLanesDescr;
    
    public GameSetupOptions(Context context, ViewGroup container, Bundle parameters) {
        super(context, container, parameters, GameKey.MINIGOLF);
    }

    @Override
    protected View init(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.minigolf_game_setup_options, null);
        mLocation = (AutoCompleteTextView) root.findViewById(R.id.location);
        mLocation.setTextColor(Color.BLACK); // else we got white onw hite for lower android versions...
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_dropdown_item_1line, new LinkedList<String>(SportGame.ALL_LOCATIONS));
        mLocation.setAdapter(adapter);

        mDefaultLanesDescr = (TextView) root.findViewById(R.id.default_lanes_count_descr);
        mDefaultLanes = (SeekBar) root.findViewById(R.id.lanes_count);
        
        // init listeners
        mDefaultLanes.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onDefaultLanesChange();
            }
        });
        
        // init values
        String loc = extractLocation(mParameters);
        mLocation.setText(loc);
        mDefaultLanes.setProgress(extractLanesCount(mParameters) - 1);
        onDefaultLanesChange();
        
        return root;
    }
    
    private void onDefaultLanesChange() {
        mParameters.putInt(Builder.EXTRA_OPTION_DEFAULT_LANES_COUNT, mDefaultLanes.getProgress() + 1);
        int lanesCount = extractLanesCount(mParameters);
        mDefaultLanesDescr.setText(mContext.getResources().getString(R.string.minigolf_default_lanes_count, lanesCount));
    }
    

    public static int extractLanesCount(Bundle options) {
        return options.getInt(Builder.EXTRA_OPTION_DEFAULT_LANES_COUNT, MinigolfGame.DEFAULT_LANES_COUNT);
    }

    @Override
    public void reset() {
        mLocation.setText("");
        mDefaultLanes.setProgress(MinigolfGame.DEFAULT_LANES_COUNT - 1);
    }

    @Override
    public void prepareParameters() {
        mParameters.putString(Builder.EXTRA_OPTION_LOCATION, mLocation.getText().toString());
        mParameters.putInt(Builder.EXTRA_OPTION_DEFAULT_LANES_COUNT, mDefaultLanes.getProgress() + 1);
    }

    public static String extractLocation(Bundle options) {
        String loc = options.getString(Builder.EXTRA_OPTION_LOCATION);
        return loc != null ? loc : "";
    }

}
