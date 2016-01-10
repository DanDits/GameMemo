package dan.dit.gameMemo.appCore.custom;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupOptionsController;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.custom.CustomGame;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class GameSetupOptions extends GameSetupOptionsController {
    
    public static class Builder {
        static final String EXTRA_OPTION_GAME_NAME = "dan.dit.gameMemo.EXTRA_OPTION_GAME_NAME"; // String
        static final String EXTRA_OPTION_ROUND_BASED = "dan.dit.gameMemo.EXTRA_OPTION_ROUND_BASED"; // boolean
        private Bundle mParameters = new Bundle();
        
        public Builder() {
            setGameName("");
            setRoundBased(CustomGame.DEFAULT_ROUND_BASED);
        }

        public void setGameName(String value) {
            mParameters.putString(EXTRA_OPTION_GAME_NAME, value);
        }
        
        public void setRoundBased(boolean roundBased) {
            mParameters.putBoolean(EXTRA_OPTION_ROUND_BASED, roundBased);
        }
        
        public Bundle build() {
            return mParameters;
        }
    }
    
    private AutoCompleteTextView mGameNames;
    private CheckBox mRoundBased;
    
    public GameSetupOptions(Context context, ViewGroup container, Bundle parameters) {
        super(context, container, parameters, GameKey.CUSTOMGAME);
    }

    @Override
    protected View init(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.custom_game_setup_options, null);
        mGameNames = (AutoCompleteTextView) root.findViewById(R.id.gameNames);
        mRoundBased = (CheckBox) root.findViewById(R.id.roundBased);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_dropdown_item_1line, new LinkedList<String>(CustomGame.ALL_NAMES));
        mGameNames.setAdapter(adapter);
        mGameNames.setTextColor(Color.BLACK); // else color is wrong on lower android versions
        mGameNames.setThreshold(1);
        
        // init values
        String name = extractGameName(mParameters);
        if (TextUtils.isEmpty(name)) {
            name = CustomGamesActivity.getSavedSelection(mContext);
        }
        mGameNames.setText(name);
        if (!adaptInfoToGameName(name)) {
            mRoundBased.setChecked(extractRoundBased(mParameters));
        }
        mGameNames.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                adaptInfoToGameName(mGameNames.getText().toString());
            }

        });
        return root;
    }
    
    private boolean adaptInfoToGameName(String name) {
        CustomGame baseOn = null;
        if (!TextUtils.isEmpty(name)) {
            List<Game> games;
            try {
                games = CustomGame.loadGamesWithSameName(mContext.getContentResolver(), name, false);
            } catch (CompactedDataCorruptException e) {
                // will not throw
                games = null;
            }
            if (games != null && games.size() > 0) {
                baseOn = (CustomGame) games.get(0);
            }
        }
        if (baseOn != null) {
            mRoundBased.setChecked(baseOn.isRoundBased());
            return true;
        } 
        return false;
    }

    @Override
    public void reset() {
        mGameNames.setText("");
        mRoundBased.setChecked(CustomGame.DEFAULT_ROUND_BASED);
    }
    

    @Override
    public void prepareParameters() {
        mParameters.putString(Builder.EXTRA_OPTION_GAME_NAME, mGameNames.getText().toString());
        mParameters.putBoolean(Builder.EXTRA_OPTION_ROUND_BASED, mRoundBased.isChecked());
    }

    public static String extractGameName(Bundle options) {
        String name = options.getString(Builder.EXTRA_OPTION_GAME_NAME);
        return !TextUtils.isEmpty(name) ? name : "";
    }
    
    public static boolean extractRoundBased(Bundle options) {
        return options.getBoolean(Builder.EXTRA_OPTION_ROUND_BASED, CustomGame.DEFAULT_ROUND_BASED);
    }
}
