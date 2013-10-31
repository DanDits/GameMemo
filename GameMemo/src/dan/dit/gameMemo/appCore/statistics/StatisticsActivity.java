package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
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
        initListeners();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.statistics, menu);
        mAddTeam = menu.findItem(R.id.add_team);
        mReset = menu.findItem(R.id.reset);
        mPresType = menu.findItem(R.id.pres_type);
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
            mPresType.setVisible(false);
            mReset.setVisible(mTeamsController != null);
            mAddTeam.setVisible(mTeamsController != null);
            if (mTeamsController != null) {
                mAddTeam.setEnabled(mTeamsController.hasMissingTeam());
            }
        } else {
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
        }
        return super.onOptionsItemSelected(item);
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
    }
    
    private StatisticAndGameLoader mLoader;
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
    
    private void onShowStatistic() {
        if (mStateSpecification) {
            mStateSpecification = false;
            if (mSwitcher.getDisplayedChild() == 0) {
                mSwitcher.showNext();
            }
            switch (mMode) {
            case STATISTICS_MODE_ALL:
                mModeAllContainer.setVisibility(View.VISIBLE);
                mModeChartContainer.setVisibility(View.GONE);
                mDisplayedStatistic = null;
                StatisticsFactoryAll factory = new StatisticsFactoryAll(mGameKey, getUserTeams(), mGames, (ListView) mModeAllContainer.findViewById(R.id.statistic_mode_all_list), (TextView) mModeAllContainer.findViewById(R.id.statistic_mode_all_header));
                factory.build(StatisticsActivity.this);
                factory.notifyDataSetChanged();
                break;
            case STATISTICS_MODE_CHRONO:
                mModeAllContainer.setVisibility(View.GONE);
                mModeChartContainer.setVisibility(View.VISIBLE);
                mModeChartContainer.removeAllViews();
                mDisplayedStatistic = getSelectedStatistic();
                mModeChartContainer.addView(new StatisticFactoryChrono(mDisplayedStatistic, mAttributeManager.getStatistic(mDisplayedStatistic.getReference())).build(StatisticsActivity.this));
                break;
            case STATISTICS_MODE_OVERVIEW:
                mModeAllContainer.setVisibility(View.GONE);
                mModeChartContainer.setVisibility(View.VISIBLE);
                mModeChartContainer.removeAllViews();
                mDisplayedStatistic = getSelectedStatistic();
                mModeChartContainer.addView(new StatisticFactoryOverview(mDisplayedStatistic, mAttributeManager.getStatistic(mDisplayedStatistic.getReference())).build(StatisticsActivity.this));
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
        if (!mStateSpecification) {
            mStateSpecification = true;
            if (mSwitcher.getDisplayedChild() == 1) {
                mSwitcher.showPrevious();
            }
            mModeChartContainer.removeAllViews();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (getActionBar() != null) {
                    getActionBar().setTitle(getResources().getString(R.string.statistics_activity_header));
                }
            }
            applyMenuButtonsState();
        }
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
        Bundle params = mAttributeManager.applyMode(mMode, mTeamsController, getResources());
        if (params != null) {
            mTeamsController = new TeamSetupTeamsController(mGameKey, this, new NotifyMajorChangeCallback() {
                
                @Override
                public void onMajorChange() {
                    applyMenuButtonsState();
                }
            }, params, mTeamsContainer);
            mTeamsController.setNoFilter(true);
            mAttributeManager.applyMode(mMode, mTeamsController, getResources());
            applyMenuButtonsState();
        } 
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
