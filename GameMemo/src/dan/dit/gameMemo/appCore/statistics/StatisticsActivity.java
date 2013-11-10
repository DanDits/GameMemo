package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerColors;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.statistics.StatisticAndGameLoader;
import dan.dit.gameMemo.gameData.statistics.StatisticAndGameLoader.LoadingListener;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ColorPickerView.OnColorChangedListener;
import dan.dit.gameMemo.util.NotifyMajorChangeCallback;

@SuppressLint("NewApi")
public class StatisticsActivity extends FragmentActivity implements OnColorChangedListener, ChoosePlayerDialogListener {
    private static final String PREFERENCES_MODE = "dan.dit.gameMemo.PREF_STATISTIC_MODE";
    private static final String PREFERENCES_CHRONO_ALPHA = "dan.dit.gameMemo.PREF_CHRONO_ALPHA";
    private static final String PREFERENCES_CHRONO_MODE = "dan.dit.gameMemo.PREF_CHRONO_MODE";
    
    private static final String EXTRA_ALLOWED_STARTTIMES = "dan.dit.gameMemo.EXTRA_ALLOWED_STARTTIMES";
    public static final int STATISTICS_MODE_ALL = 0;
    public static final int STATISTICS_MODE_OVERVIEW = 1;
    public static final int STATISTICS_MODE_CHRONO = 2;
    private static final int DEFAULT_STATISTICS_MODE = STATISTICS_MODE_OVERVIEW;
    
    private Set<Long> mAllowedStarttimes;
    private int mGameKey;
    private ImageButton mBtnModeAll;
    private ImageButton mBtnModeOverview;
    private ImageButton mBtnModeChrono;
    private Spinner mStatisticsSelect;
    private ImageButton mStatisticsEdit;
    private Button mStatisticsShow;
    private ViewSwitcher mSwitcher;
    private ViewGroup mTeamsContainer;
    private ViewGroup mModeAllContainer;
    private ViewGroup mModeChartContainer;
    private MenuItem mAddTeam;
    private MenuItem mReset;
    private MenuItem mPresType;
    private MenuItem mChronoSettings;
    
    private boolean mStateSpecification;
    private GameStatistic mDisplayedStatistic;
    private int mMode;
    private List<Game> mGames;
    private TeamSetupTeamsController mTeamsController;
    private GameStatisticAttributeManager mAttributeManager;
    private SimpleStatisticsAdapter mStatisticAdapter;
    
    public static Intent getIntent(Context context, int gamekey, Collection<Long> allowedStarttimes) {
        Intent i = new Intent(context, StatisticsActivity.class);
        i.putExtra(GameKey.EXTRA_GAMEKEY, gamekey);
        if (allowedStarttimes != null) {
            i.putExtra(EXTRA_ALLOWED_STARTTIMES, GameKey.toArray(allowedStarttimes));            
        }
        return i;
    }
    
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        mGameKey = getIntent().getExtras().getInt(GameKey.EXTRA_GAMEKEY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getActionBar() != null) {
                getActionBar().setTitle(getResources().getString(R.string.statistics_activity_header));
                getActionBar().setDisplayShowTitleEnabled(true);
                getActionBar().setDisplayHomeAsUpEnabled(true);
                getActionBar().setIcon(GameKey.getGameIconId(mGameKey));
            }
        }
        setContentView(R.layout.statistics);
        mBtnModeAll = (ImageButton) findViewById(R.id.mode_all);
        mBtnModeOverview = (ImageButton) findViewById(R.id.mode_overview);
        mBtnModeChrono = (ImageButton) findViewById(R.id.mode_chrono);
        mStatisticsSelect = (Spinner) findViewById(R.id.statistic_select);
        mStatisticsSelect.setEnabled(false);
        mStatisticsSelect.setAdapter(null);
        mStatisticsEdit = (ImageButton) findViewById(R.id.statistic_edit);
        mStatisticsEdit.setEnabled(false);
        mStatisticsShow = (Button) findViewById(R.id.statistic_show);
        mSwitcher = (ViewSwitcher) findViewById(R.id.statistic_switcher);
        mTeamsContainer = (ViewGroup) findViewById(R.id.teams_container);
        mModeAllContainer = (ViewGroup) findViewById(R.id.statistic_mode_all_holder);
        mModeChartContainer = (ViewGroup) findViewById(R.id.statistic_mode_chart_holder);
        mStateSpecification = true;
        loadStatisticsAndGame();
        applyTheme();
        initMode();
        loadChronoAlphaAndMode();
        initListeners();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.statistics, menu);
        mAddTeam = menu.findItem(R.id.add_team);
        mReset = menu.findItem(R.id.reset);
        mPresType = menu.findItem(R.id.pres_type);
        mChronoSettings = menu.findItem(R.id.chrono_settings);
        applyMenuButtonsState();
        return true;
    }
    
    public void onStart() {
        super.onStart();
        if (mStatisticAdapter != null) {
            refreshStatisticsAdapter();
        }
    }
    
    private void applyMenuButtonsState() {
        if (mPresType == null || mReset == null  || mAddTeam == null) {
            return; // not yet initialized
        }
        if (mStateSpecification) {
            mChronoSettings.setVisible(false);
            mPresType.setVisible(false);
            mReset.setVisible(mTeamsController != null);
            mAddTeam.setVisible(mTeamsController != null);
            if (mTeamsController != null) {
                mAddTeam.setEnabled(mTeamsController.hasMissingTeam());
            }
        } else {
            mChronoSettings.setVisible(mChrono != null);
            mPresType.setVisible(mDisplayedStatistic != null);
            if (mDisplayedStatistic != null) {
                int presTypeTitleResId = GameStatistic.getPresTypeTextResId(mDisplayedStatistic.getPresentationType());
                int presTypeIconResId = GameStatistic.getPresTypeDrawableResId(mDisplayedStatistic.getPresentationType());
                
                mPresType.setTitle(presTypeTitleResId);
                mPresType.setIcon(presTypeIconResId);
            }
            mAddTeam.setVisible(false);
            mReset.setVisible(false);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if (mStateSpecification) {
                setResult(RESULT_CANCELED);
                finish();
            } else {
                switchToSpecification();
            }
            return true;
        case R.id.reset:
            if (mTeamsController != null) {
                mTeamsController.resetFields();
            }
            return true;
        case R.id.add_team:
            if (mTeamsController != null) {
                mTeamsController.addMissingTeam();
                applyMenuButtonsState();
            }
            return true;
        case R.id.pres_type:
            nextPresType();
            return true;
        case R.id.chrono_settings:
            showChronoSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showChronoSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = this.getLayoutInflater().inflate(R.layout.statistic_chrono_smoothing, null);
        final SeekBar bar = (SeekBar) v.findViewById(R.id.smoothBar);
        final RadioGroup group = (RadioGroup) v.findViewById(R.id.radioMode);
        group.check(mChronoModeSingleValues ? R.id.chrono_mode_single_values : R.id.chrono_mode_summed_values);
        bar.setMax(1000);
        bar.setKeyProgressIncrement(100);
        if (mAlpha < convertToAlpha(1)) {
            bar.setProgress(bar.getMax());
        } else {
            bar.setProgress((int) (- bar.getMax() * Math.log(mAlpha) / 6.0));
        }
        bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                applyAlphaAndMode(convertToAlpha(((double) bar.getProgress()) / bar.getMax()), mChronoModeSingleValues);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
            }
        });
        builder.setView(v)
        .setTitle(getResources().getString(R.string.statistics_chrono_settings_title))
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                setAlphaAndMode(convertToAlpha(((double) bar.getProgress()) / bar.getMax()), group.getCheckedRadioButtonId() == R.id.chrono_mode_single_values);
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                applyAlphaAndMode(mAlpha, mChronoModeSingleValues);
            }
        }).show();
    }
    
    private double convertToAlpha(double fraction) {
        return Math.pow(Math.E, -6.0 * fraction);
    }
    
    private void nextPresType() {
        if (mDisplayedStatistic != null) {
            // change and force refresh
            mDisplayedStatistic.setPresentationType(mDisplayedStatistic.nextPresentationType());
            if (!mStateSpecification) {
                mStateSpecification = true;
                onShowStatistic();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mStatTask != null) {
            mStatTask.cancel(true);
            return;
        }
        if (mStateSpecification) {
            super.onBackPressed();
            return;
        }
        switchToSpecification();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoader != null) {
            mLoader.cancel(true);
        }
        if (mStatTask != null) {
            mStatTask.cancel(true);
        }
    }
    
    private StatisticAndGameLoader mLoader;
    private boolean mChronoModeSingleValues;
    private double mAlpha; // for the chrono mode
    private StatisticFactoryChrono mChrono;
    
    private void loadStatisticsAndGame() {
        mAttributeManager = GameKey.getGameStatisticAttributeManager(mGameKey);
        mStatisticsShow.setEnabled(false);
        long[] allowed = getIntent().getExtras().getLongArray(EXTRA_ALLOWED_STARTTIMES);
        mStatisticsShow.setText(allowed == null ? getResources().getString(R.string.statistics_show_chart_all_games) :
            getResources().getQuantityString(R.plurals.statistics_show_chart_x_games, allowed.length, allowed.length));
        if (allowed != null) {
            mAllowedStarttimes = new HashSet<Long>();
            for (long st : allowed) {
                mAllowedStarttimes.add(st);
            }
        }
        mLoader = new StatisticAndGameLoader(getApplicationContext(), mGameKey, mAllowedStarttimes);
        Uri[] uris = new Uri[] {GameStorageHelper.getUriAllItems(mGameKey)};
        mLoader.addListener(new LoadingListener() {
            
            @Override
            public void loadingComplete(List<Game> games, int gameKey) {
                if (games != null) {
                    mGames = games;
                    mStatisticsShow.setEnabled(true);
                }
                mLoader = null;
                initStatisticSelectUI();
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
            }

            @Override
            public void loadingProgress(int percentage, int gameKey) {
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 100 * percentage);
            }
        });
        mLoader.execute(uris);
    }
    
    private void initStatisticSelectUI() {
        refreshStatisticsAdapter();
        mStatisticsEdit.setEnabled(true);
        mStatisticsShow.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onShowStatistic();
            }
            
        });
    }
    
    private void refreshStatisticsAdapter() {
        List<GameStatistic> stats = mAttributeManager.getStatistics(false);
        List<StatisticAttribute> attrs = new ArrayList<StatisticAttribute>(stats);
        mStatisticAdapter = new SimpleStatisticsAdapter(this, attrs);
        mStatisticsSelect.setAdapter(mStatisticAdapter);
    }
    
    private StatisticFactory.StatisticBuildTask mStatTask;
    private void onShowStatistic() {
        if (mStateSpecification) {
            mStateSpecification = false;
            mBtnModeAll.setEnabled(false);
            mBtnModeChrono.setEnabled(false);
            mBtnModeOverview.setEnabled(false);
            mStatisticsEdit.setEnabled(false);
            mStatisticsSelect.setEnabled(false);
            mStatisticsShow.setEnabled(false);
            mPresType.setEnabled(false);
            
            StatisticFactory.BuildListener listener = new StatisticFactory.BuildListener() {

                @Override
                public void buildUpdate(int percentage) {
                    setProgress(percentage * 100);
                }
                
                @Override
                public void buildComplete(View result) {
                    setProgress(10000);
                    mStatTask = null;
                    mPresType.setEnabled(true);
                    switch (mMode) {
                    case STATISTICS_MODE_CHRONO:
                        /* fall through */
                    case STATISTICS_MODE_OVERVIEW:
                        mModeChartContainer.removeAllViews();
                        mModeChartContainer.addView(result);
                        break;
                    case STATISTICS_MODE_ALL:
                        // nothing to do
                        break;
                    }
                    if (mSwitcher.getDisplayedChild() == 0) {
                        mSwitcher.showNext();
                    }
                }

                @Override
                public void buildCancelled() {
                    mStatTask = null;
                    setProgress(10000);
                    switchToSpecification();
                }
            };
            switch (mMode) {
            case STATISTICS_MODE_ALL:
                mModeAllContainer.setVisibility(View.VISIBLE);
                mModeChartContainer.setVisibility(View.GONE);
                mDisplayedStatistic = null;
                StatisticsFactoryAll factory = new StatisticsFactoryAll(mGameKey, getUserTeams(), mGames, (ListView) mModeAllContainer.findViewById(R.id.statistic_mode_all_list), (TextView) mModeAllContainer.findViewById(R.id.statistic_mode_all_header));
                mStatTask = factory.build(StatisticsActivity.this, listener);
                break;
            case STATISTICS_MODE_CHRONO:
                mModeAllContainer.setVisibility(View.GONE);
                mModeChartContainer.setVisibility(View.VISIBLE);
                mDisplayedStatistic = getSelectedStatistic();
                mChrono = new StatisticFactoryChrono(mDisplayedStatistic, mAttributeManager.getStatistic(mDisplayedStatistic.getReference()), mAlpha, mChronoModeSingleValues);
                mStatTask = mChrono.build(StatisticsActivity.this, listener);
                break;
            case STATISTICS_MODE_OVERVIEW:
                mModeAllContainer.setVisibility(View.GONE);
                mModeChartContainer.setVisibility(View.VISIBLE);
                mDisplayedStatistic = getSelectedStatistic();
                mStatTask = new StatisticFactoryOverview(mDisplayedStatistic, mAttributeManager.getStatistic(mDisplayedStatistic.getReference())).build(StatisticsActivity.this, listener);
                break;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (getActionBar() != null && mDisplayedStatistic != null) {
                    getActionBar().setTitle(mDisplayedStatistic.getName(getResources()));
                }
            }
            applyMenuButtonsState();
        }
    }
    
    private List<AbstractPlayerTeam> getUserTeams() {
        List<AbstractPlayerTeam> teams = mTeamsController.getAllTeams(false);
        // remove appended empty teams
        ListIterator<AbstractPlayerTeam> it = teams.listIterator(teams.size());
        while (it.hasPrevious()) {
            AbstractPlayerTeam team = it.previous();
            if (team == null || team.getPlayerCount() == 0) {
                it.remove();
            } else {
                break;
            }
        }
        for (AbstractPlayerTeam team : teams) {
            if (team.getPlayerCount() > 0) {
                int currColor = team.getColor();
                if (!team.loadCachedColor(this)) {
                    // no color saved yet so save current
                    team.saveColor(this);
                } else if (team.getColor() != currColor) {
                    // there is a different color saved and loaded
                    if (currColor != PlayerColors.DEFAULT_COLOR) {
                        team.setColor(currColor); // if the team has a non default color, use this color, else the loaded color
                    }
                    team.saveColor(this);
                }
                if (team.getColor() == PlayerColors.DEFAULT_COLOR) {
                    team.setColor(team.getFirst().getColor());
                    if (team.getPlayerCount() > 1) {
                        team.saveColor(this);
                    }
                }
            }
        }
        return teams;
    }
    
    private GameStatistic getSelectedStatistic() {
        GameStatistic stat = (GameStatistic) mStatisticsSelect.getSelectedItem();
        if (stat != null) {
            List<AbstractPlayerTeam> userTeams = getUserTeams();
            stat.setGameList(mGames);
            stat.setTeams(userTeams);
            GameStatistic ref = mAttributeManager.getStatistic(stat.getReference());
            if (ref != null) {
                ref.setGameList(mGames);
                ref.setTeams(userTeams);
            }
        }
        return stat;
    }
    
    private void onStatisticSelected() {
        GameStatistic stat = (GameStatistic) mStatisticsSelect.getSelectedItem();
        if (stat != null && stat.isUserAttribute()) {
            if (mStatisticAdapter != null) {
                mStatisticAdapter.sort();
                mStatisticsSelect.setSelection(mStatisticAdapter.getPosition(stat));
            }
        }
    }
    
    private void switchToSpecification() {
        mStateSpecification = true;
        mChrono = null;
        mDisplayedStatistic = null;
        if (mSwitcher.getDisplayedChild() == 1) {
            mSwitcher.showPrevious();
        }
        mStatisticsEdit.setEnabled(true); // disabled when building a stat
        mStatisticsShow.setEnabled(mGames != null);
        mPresType.setEnabled(true);
        applyModeButtonsState();
        mModeChartContainer.removeAllViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getActionBar() != null) {
                getActionBar().setTitle(getResources().getString(R.string.statistics_activity_header));
            }
        }
        applyMenuButtonsState();
    }
    
    private void applyTheme() {
        Resources res = getResources();
        GameKey.applyTheme(mGameKey, res, mBtnModeAll);
        GameKey.applyTheme(mGameKey, res, mBtnModeChrono);
        GameKey.applyTheme(mGameKey, res, mBtnModeOverview);
        GameKey.applyTheme(mGameKey, res, mStatisticsEdit);
        GameKey.applyTheme(mGameKey, res, mStatisticsShow);
    }
    
    private void initMode() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int mode = sharedPref.getInt(PREFERENCES_MODE, DEFAULT_STATISTICS_MODE);
        setMode(mode);
    }
    
    private void loadChronoAlphaAndMode() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        mAlpha = Double.longBitsToDouble(sharedPref.getLong(PREFERENCES_CHRONO_ALPHA, Double.doubleToLongBits(StatisticFactoryChrono.DEFAULT_ALPHA)));
        mChronoModeSingleValues = sharedPref.getBoolean(PREFERENCES_CHRONO_MODE, StatisticFactoryChrono.DEFAULT_CHRONO_MODE_SINGLE_VALUES);
    }
    
    private void setAlphaAndMode(double alpha, boolean modeSingleValues) {
        mAlpha = alpha;
        mChronoModeSingleValues = modeSingleValues;
        applyAlphaAndMode(mAlpha, modeSingleValues);
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putLong(PREFERENCES_CHRONO_ALPHA, Double.doubleToLongBits(mAlpha));
        editor.putBoolean(PREFERENCES_CHRONO_MODE, modeSingleValues);
        editor.apply();
    }
    
    private void applyAlphaAndMode(double alpha, boolean modeSingleValues) {
        if (mChrono != null) {
            mChrono.setAlphaAndMode(this, alpha, modeSingleValues);
        }
    }
    
    private void setMode(int newMode) {
        if (!mStateSpecification) {
            return; // ignore if in specification state
        }
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putInt(PREFERENCES_MODE, newMode);
        editor.apply();
        mMode = newMode;
        applyMode();
        
    }
    
    private void applyMode() {
        Bundle params = mAttributeManager.createTeamsParameters(mMode, getResources(), mTeamsController != null ? mTeamsController.getAllTeams(true) : null);
        mTeamsController = new TeamSetupTeamsController(mGameKey, this, new NotifyMajorChangeCallback() {
            
            @Override
            public void onMajorChange() {
                applyMenuButtonsState();
            }
        }, params, mTeamsContainer);
        mTeamsController.setNoFilter(true);
        applyMenuButtonsState();
        applyModeButtonsState();
    }
    
    private void applyModeButtonsState() {
        switch (mMode) {
        case STATISTICS_MODE_ALL:
            mBtnModeAll.setEnabled(false);
            mBtnModeChrono.setEnabled(true);
            mBtnModeOverview.setEnabled(true);
            mStatisticsSelect.setEnabled(false);
            break;
        case STATISTICS_MODE_CHRONO:
            mBtnModeAll.setEnabled(true);
            mBtnModeChrono.setEnabled(false);
            mBtnModeOverview.setEnabled(true);
            mStatisticsSelect.setEnabled(true);
            break;    
        case STATISTICS_MODE_OVERVIEW:
            mBtnModeAll.setEnabled(true);
            mBtnModeChrono.setEnabled(true);
            mBtnModeOverview.setEnabled(false);
            mStatisticsSelect.setEnabled(true);
            break; 
        }
    }
    
    private void initListeners() {
        mBtnModeAll.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                setMode(STATISTICS_MODE_ALL);
            }
        });
        mBtnModeChrono.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                setMode(STATISTICS_MODE_CHRONO);
            }
        });
        mBtnModeOverview.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                setMode(STATISTICS_MODE_OVERVIEW);
            }
        });
        mStatisticsSelect.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                onStatisticSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                onStatisticSelected();
            }
        });
        mStatisticsEdit.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onEditStatistics();
            }
        });
    }

    private void onEditStatistics() {
        StatisticAttribute stat = getSelectedStatistic();
        if (stat == null) {
            mAttributeManager.getAllAttributes(true).get(0);
        }
        Intent i = StatisticEditActivity.getIntent(this, mGameKey, stat);
        startActivity(i);
    }
    
    @Override
    public void onColorChanged(int color) {
        mTeamsController.onColorChanged(color);        
    }

    @Override
    public PlayerPool getPool() {
        return GameKey.getPool(mGameKey);
    }

    @Override
    public List<Player> toFilter() {
        return mTeamsController.toFilter();
    }

    @Override
    public void playerChosen(int arg, Player chosen) {
        mTeamsController.playerChosen(arg, chosen);
    }

    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {
        mTeamsController.onPlayerColorChanged(arg, concernedPlayer);
        concernedPlayer.saveColor(this);
    }
}
