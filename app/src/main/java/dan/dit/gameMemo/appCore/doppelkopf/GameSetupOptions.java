package dan.dit.gameMemo.appCore.doppelkopf;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupOptionsController;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRuleSystem;

public class GameSetupOptions extends GameSetupOptionsController {
    
    public static class Builder {
        static final String EXTRA_OPTION_DUTY_SOLI = "dan.dit.gameMemo.EXTRA_OPTION_DUTY_SOLI"; // int
        static final String EXTRA_OPTION_DURCHLAEUFE = "dan.dit.gameMemo.EXTRA_OPTION_DURCHLAEUFE"; // int
        private Bundle mParameters = new Bundle();
        
        public Builder(DoppelkopfRuleSystem ruleSys) {
            if (ruleSys != null) {
                setDutySoli(ruleSys.getDefaultDutySoli());
                setDurchlauefe(ruleSys.getDefaultDurchlaeufe());
            } else {
                setDutySoli(DoppelkopfGame.DEFAULT_DUTY_SOLI);
                setDurchlauefe(DoppelkopfGame.DEFAULT_DURCHLAEUFE);
            }
        }
        
        public void setDutySoli(int value) {
            mParameters.putInt(EXTRA_OPTION_DUTY_SOLI, value);
        }
        
        public void setDurchlauefe(int value) {
            mParameters.putInt(EXTRA_OPTION_DURCHLAEUFE, value);
        }
        
        public Bundle build() {
            return mParameters;
        }
    }
    
    private TextView mDutySoliDescr;
    private TextView mDurchlaeufeDescr;
    private SeekBar mDutySoli;
    private SeekBar mDurchlaeufe;
    
    public GameSetupOptions(Context context, ViewGroup container, Bundle parameters) {
        super(context, container, parameters, GameKey.DOPPELKOPF);
    }

    @Override
    protected View init(LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.doppelkopf_game_setup_options, null);
        mDutySoli = (SeekBar) root.findViewById(R.id.duty_soli);
        mDutySoliDescr = (TextView) root.findViewById(R.id.duty_soli_descr);
        mDurchlaeufe = (SeekBar) root.findViewById(R.id.durchlaeufe);
        mDurchlaeufeDescr = (TextView) root.findViewById(R.id.durchlaeufe_descr);
        // init listeners
        mDutySoli.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onDutySoliChange();
            }
        });
        mDurchlaeufe.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onDurchlaeufeChange();
            }
        });
        // init values
        int durchlaeufe = extractDurchlaeufe(mParameters);
        mDurchlaeufe.setMax(DoppelkopfGame.MAX_DURCHLAEUFE);
        setDurchlauefe(durchlaeufe);
        int dutySoli = extractDutySoli(mParameters);
        mDutySoli.setMax(DoppelkopfGame.MAX_DUTY_SOLI);
        setDutySoli(dutySoli);
        return root;
    }

    private void onDutySoliChange() {
        mParameters.putInt(Builder.EXTRA_OPTION_DUTY_SOLI, mDutySoli.getProgress());
        mDutySoliDescr.setText(mContext.getResources().getString(R.string.doppelkopf_option_duty_soli_with_value, extractDutySoli(mParameters)));
    }
    
    private void onDurchlaeufeChange() {
        mParameters.putInt(Builder.EXTRA_OPTION_DURCHLAEUFE, mDurchlaeufe.getProgress());
        int durchlaeufe = extractDurchlaeufe(mParameters);
        if (durchlaeufe == DoppelkopfGame.NO_LIMIT) {
            mDurchlaeufeDescr.setText(mContext.getResources().getString(R.string.doppelkopf_option_cycles_no_limit));            
        } else {
            mDurchlaeufeDescr.setText(mContext.getResources().getString(R.string.doppelkopf_option_cycles_with_value, durchlaeufe));
        }
    }
    
    private void setDurchlauefe(int durchlaeufe) {
        mDurchlaeufe.setProgress(durchlaeufe);        
    }
    
    private void setDutySoli(int dutySoli) {
        mDutySoli.setProgress(dutySoli);  
    }
    
    @Override
    public void reset() {
        setDurchlauefe(DoppelkopfGame.DEFAULT_DURCHLAEUFE); // also changes parameter value
        setDutySoli(DoppelkopfGame.DEFAULT_DUTY_SOLI); // also changes parameter value
    }

    @Override
    public void prepareParameters() {
        // nothing to prepare, we update changes as they happen
    }

    public static int extractDurchlaeufe(Bundle options) {
        return options.getInt(Builder.EXTRA_OPTION_DURCHLAEUFE, DoppelkopfGame.DEFAULT_DURCHLAEUFE);
    }
    
    public static int extractDutySoli(Bundle options) {
        return options.getInt(Builder.EXTRA_OPTION_DUTY_SOLI, DoppelkopfGame.DEFAULT_DUTY_SOLI);
    }

}
