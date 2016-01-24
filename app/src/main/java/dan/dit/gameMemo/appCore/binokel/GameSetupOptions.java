package dan.dit.gameMemo.appCore.binokel;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupOptionsController;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.binokel.BinokelGame;
import dan.dit.gameMemo.gameData.game.binokel.BinokelRoundType;

/**
 * Created by daniel on 10.01.16.
 */
public class GameSetupOptions extends GameSetupOptionsController {

    public static class Builder {
        static final String EXTRA_OPTION_UNTENDURCH_VALUE = "dan.dit.gameMemo" +
                ".EXTRA_OPTION_UNTENDURCH_VALUE"; //int
        static final String EXTRA_OPTION_DURCH_VALUE = "dan.dit.gameMemo" +
                ".EXTRA_OPTION_DURCH_VALUE"; //int
        static final String EXTRA_OPTION_SCORE_LIMIT = "dan.dit.gameMemo.EXTRA_OPTION_SCORE_LIMIT"; // int

        private Bundle mParameters = new Bundle();

        public Builder() {
            setUntenDurchValue(BinokelRoundType.UNTEN_DURCH.getSpecialDefaultScore());
            setDurchValue(BinokelRoundType.DURCH.getSpecialDefaultScore());
            setScoreLimit(BinokelGame.DEFAULT_SCORE_LIMIT);
        }

        public void setUntenDurchValue(int value) {
            mParameters.putInt(EXTRA_OPTION_UNTENDURCH_VALUE, value);
        }

        public void setDurchValue(int value) {
            mParameters.putInt(EXTRA_OPTION_DURCH_VALUE, value);
        }

        public void setScoreLimit(int value) {
            mParameters.putInt(EXTRA_OPTION_SCORE_LIMIT, value);
        }

        public Bundle build() {
            return mParameters;
        }
    }

    private static final int SPECIALGAMES_STEPS = BinokelRoundType.MIN_SPECIAL_GAME_VALUE;
    private static final int SCORE_STEPS = BinokelGame.MIN_SCORE_LIMIT; // dividend of min and max
    // limit and of default limit
    private TextView mScoreLimitDescr;
    private SeekBar mScoreLimit;
    private TextView mUntenDurchDescr;
    private SeekBar mUntenDurch;
    private TextView mDurchDescr;
    private SeekBar mDurch;

    public GameSetupOptions(Context context, ViewGroup container, Bundle parameters) {
        super(context, container, parameters, GameKey.BINOKEL);
    }

    @Override
    protected View init(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.binokel_game_setup_options, null);
        mScoreLimitDescr = (TextView) root.findViewById(R.id.score_limit_descr);
        mScoreLimit = (SeekBar) root.findViewById(R.id.score_limit);
        mUntenDurchDescr = (TextView) root.findViewById(R.id.unten_durch_descr);
        mUntenDurch = (SeekBar) root.findViewById(R.id.unten_durch_value);
        mDurchDescr = (TextView) root.findViewById(R.id.durch_descr);
        mDurch = (SeekBar) root.findViewById(R.id.durch_value);

        // init listeners
        mScoreLimit.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onScoreLimitChange();
            }
        });
        mUntenDurch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onUntenDurchChange();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mDurch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onDurchChange();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // init values
        int scoreLimit = extractScoreLimit(mParameters);
        mScoreLimit.setMax((BinokelGame.MAX_SCORE_LIMIT - BinokelGame.MIN_SCORE_LIMIT) /
                SCORE_STEPS);
        setScoreLimit(scoreLimit);

        int untenDurch = extractUntenDurchValue(mParameters);
        mUntenDurch.setMax((BinokelRoundType.MAX_SPECIAL_GAME_VALUE - BinokelRoundType
                .MIN_SPECIAL_GAME_VALUE) / SPECIALGAMES_STEPS);
        setUntenDurchValue(untenDurch);

        int durch = extractDurchValue(mParameters);
        mDurch.setMax((BinokelRoundType.MAX_SPECIAL_GAME_VALUE - BinokelRoundType
                .MIN_SPECIAL_GAME_VALUE) / SPECIALGAMES_STEPS);
        setDurchValue(durch);

        return root;
    }

    private void setScoreLimit(int limit) {
        mScoreLimit.setProgress((limit - BinokelGame.MIN_SCORE_LIMIT) / SCORE_STEPS);
    }

    private void setUntenDurchValue(int value) {
        mUntenDurch.setProgress((value - BinokelRoundType.MIN_SPECIAL_GAME_VALUE) / SPECIALGAMES_STEPS);
    }

    private void setDurchValue(int value) {
        mDurch.setProgress((value - BinokelRoundType.MIN_SPECIAL_GAME_VALUE) / SPECIALGAMES_STEPS);
    }

    private void onScoreLimitChange() {
        mParameters.putInt(Builder.EXTRA_OPTION_SCORE_LIMIT, SCORE_STEPS * mScoreLimit
                .getProgress() + BinokelGame.MIN_SCORE_LIMIT);
        mScoreLimitDescr.setText(mContext.getResources().getString(R.string
                .binokel_game_score_limit_with_value, extractScoreLimit(mParameters)));
    }

    private void onUntenDurchChange() {
        mParameters.putInt(Builder.EXTRA_OPTION_UNTENDURCH_VALUE,
                SPECIALGAMES_STEPS * mUntenDurch.getProgress() + BinokelRoundType
                        .MIN_SPECIAL_GAME_VALUE);
        mUntenDurchDescr.setText(mContext.getResources().getString(R.string
                .binokel_unten_durch_score, extractUntenDurchValue(mParameters)));
    }

    private void onDurchChange() {
        mParameters.putInt(Builder.EXTRA_OPTION_DURCH_VALUE,
                SPECIALGAMES_STEPS * mDurch.getProgress() + BinokelRoundType
                        .MIN_SPECIAL_GAME_VALUE);
        mDurchDescr.setText(mContext.getResources().getString(R.string
                .binokel_durch_score, extractDurchValue(mParameters)));
    }

    @Override
    public void reset() {
        setScoreLimit(BinokelGame.DEFAULT_SCORE_LIMIT);
        setUntenDurchValue(BinokelRoundType.UNTEN_DURCH.getSpecialDefaultScore());
        setDurchValue(BinokelRoundType.DURCH.getSpecialDefaultScore());
    }

    @Override
    public void prepareParameters() {
        // nothing to prepare, we update changes as they happen
    }

    public static int extractScoreLimit(Bundle options) {
        return options.getInt(Builder.EXTRA_OPTION_SCORE_LIMIT, BinokelGame.DEFAULT_SCORE_LIMIT);
    }

    public static int extractUntenDurchValue(Bundle options) {
        return options.getInt(Builder.EXTRA_OPTION_UNTENDURCH_VALUE, BinokelRoundType
                .UNTEN_DURCH.getSpecialDefaultScore());
    }

    public static int extractDurchValue(Bundle options) {
        return options.getInt(Builder.EXTRA_OPTION_DURCH_VALUE, BinokelRoundType
                .DURCH.getSpecialDefaultScore());
    }
}
