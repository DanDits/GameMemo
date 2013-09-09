package dan.dit.gameMemo.appCore.tichu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupOptionsController;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;

public class GameSetupOptions extends GameSetupOptionsController {
    
    public static class Builder {
        static final String EXTRA_OPTION_MERCY_RULE = "dan.dit.gameMemo.EXTRA_OPTION_MERCY_RULE"; // boolean
        static final String EXTRA_OPTION_SCORE_LIMIT = "dan.dit.gameMemo.EXTRA_OPTION_SCORE_LIMIT"; // int
        private Bundle mParameters = new Bundle();
        
        public Builder() {
            setMercyRuleEnabled(TichuGame.DEFAULT_USE_MERCY_RULE);
            setScoreLimit(TichuGame.DEFAULT_SCORE_LIMIT);
        }
        
        public void setMercyRuleEnabled(boolean value) {
            mParameters.putBoolean(EXTRA_OPTION_MERCY_RULE, value);
        }
        
        public void setScoreLimit(int value) {
            mParameters.putInt(EXTRA_OPTION_SCORE_LIMIT, value);
        }
        
        public Bundle build() {
            return mParameters;
        }
    }
    
    private static final int SCORE_STEPS = TichuGame.MIN_SCORE_LIMIT; // dividend of min and max limit and of default limit
    private CheckBox mMercyRule;
    private TextView mScoreLimitDescr;
    private SeekBar mScoreLimit;
    
    public GameSetupOptions(Context context, ViewGroup container, Bundle parameters) {
        super(context, container, parameters, GameKey.TICHU);
    }

    @Override
    protected View init(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.tichu_game_setup_options, null);
        mMercyRule = (CheckBox) root.findViewById(R.id.mercy_rule);
        mScoreLimitDescr = (TextView) root.findViewById(R.id.score_limit_descr);
        mScoreLimit = (SeekBar) root.findViewById(R.id.score_limit);
        // init listeners
        mScoreLimit.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onScoreLimitChange();
            }
        });
        mMercyRule.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onMercyRuleChange();
            }
        });
        // init values
        int scoreLimit = extractScoreLimit(mParameters);
        mScoreLimit.setMax((TichuGame.MAX_SCORE_LIMIT - TichuGame.MIN_SCORE_LIMIT) / SCORE_STEPS);
        setScoreLimit(scoreLimit);
        setMercyRule(extractMercyRule(mParameters));
        return root;
    }

    private void onScoreLimitChange() {
        mParameters.putInt(Builder.EXTRA_OPTION_SCORE_LIMIT, SCORE_STEPS * mScoreLimit.getProgress() + TichuGame.MIN_SCORE_LIMIT);
        mScoreLimitDescr.setText(mContext.getResources().getString(R.string.tichu_game_score_limit_with_value, extractScoreLimit(mParameters)));
    }
    
    private void onMercyRuleChange() {
        mParameters.putBoolean(Builder.EXTRA_OPTION_MERCY_RULE, mMercyRule.isChecked());
    }
    
    private void setScoreLimit(int limit) {
        mScoreLimit.setProgress((limit - TichuGame.MIN_SCORE_LIMIT) / SCORE_STEPS);        
    }
    
    private void setMercyRule(boolean useMercyRule) {
        mMercyRule.setChecked(useMercyRule);
    }
    
    @Override
    public void reset() {
        setScoreLimit(TichuGame.DEFAULT_SCORE_LIMIT); // also changes parameter value
        setMercyRule(TichuGame.DEFAULT_USE_MERCY_RULE); // also changes parameter value
    }

    @Override
    public void prepareParameters() {
        // nothing to prepare, we update changes as they happen
    }

    public static int extractScoreLimit(Bundle options) {
        return options.getInt(Builder.EXTRA_OPTION_SCORE_LIMIT, TichuGame.DEFAULT_SCORE_LIMIT);
    }
    
    public static boolean extractMercyRule(Bundle options) {
        return options.getBoolean(Builder.EXTRA_OPTION_MERCY_RULE, TichuGame.DEFAULT_USE_MERCY_RULE);
    }

}
