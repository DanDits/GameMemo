package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.LinearLayout;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.TeamSetupViewController.TeamSetupCallback;
import dan.dit.gameMemo.util.ColorPickerDialog;
import dan.dit.gameMemo.util.ColorPickerView.OnColorChangedListener;
import dan.dit.gameMemo.util.NotifyMajorChangeCallback;

public class TeamSetupTeamsController implements TeamSetupCallback, OnColorChangedListener {
    public static final String EXTRA_TEAM_COLORS = "dan.dit.gameMemo.EXTRA_TEAM_COLORS"; // int[], changeable if TEAM_COLOR_EDITING is true
    public static final String EXTRA_TEAM_NAMES = "dan.dit.gameMemo.EXTRA_TEAM_NAMES"; //  String[], changeable if TEAM_NAME_EDITING is true
    private static final String STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX = "STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX"; //int ignored if no choose player dialog open, index of controller to chose player for 
    private static final String STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX = "STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX"; //int ignored if no color chooser dialog open, index of controller to chose color for
    public static final String EXTRA_PLAYER_NAMES = "dan.dit.gameMemo.EXTRA_PLAYER_NAMES"; // String[], changeable, contains null for not selected but allowed slots in a team
    
    private Map<Integer, TeamSetupViewController> mTeamControllers;
    private LinearLayout mTeamsContainer;
    private SortedSet<Integer> mHiddenTeams;
    private int mMaxTeamCount;
    private int mGameKey;
    private FragmentActivity mActivity;
    private Bundle mParameters;
    private NotifyMajorChangeCallback mCallback;
    private int mChoosingPlayerControllerIndex = -1;
    private int mChoosingColorControllerIndex = -1;

    public static class Builder {
        private static final String PARAMETER_FLAG_USE_DUMMY_PLAYERS = "dan.dit.gameMemo.USE_DUMMY_PLAYERS"; // boolean
        private static final String PARAMETER_FLAG_ALLOW_PLAYER_COLOR_EDITING = "dan.dit.gameMemo.ALLOW_PLAYER_COLOR_EDITING"; // boolean
        private static final String PARAMETER_ALLOW_TEAM_COLOR_EDITING = "dan.dit.gameMemo.ALLOW_TEAM_COLOR_EDITING"; // boolean
        private static final String PARAMETER_ALLOW_TEAM_NAME_EDITING = "dan.dit.gameMemo.ALLOW_TEAM_NAME_EDITING"; // boolean
        private static final String PARAMETER_TEAM_IS_OPTIONAL = "dan.dit.gameMemo.EXTRA_TEAM_IS_OPTIONAL"; //  boolean[]
        private static final String PARAMETER_TEAM_MIN_PLAYERS = "dan.dit.gameMemo.EXTRA_TEAM_MIN_PLAYERS"; // int[]
        private static final String PARAMETER_TEAM_MAX_PLAYERS = "dan.dit.gameMemo.EXTRA_TEAM_MAX_PLAYERS"; // int[]
        
        private Bundle mParameters = new Bundle();
        private List<Integer> mMinPlayers = new ArrayList<Integer>(5);
        private List<Integer> mMaxPlayers = new ArrayList<Integer>(5);
        private List<Boolean> mIsOptional = new ArrayList<Boolean>(5);
        private List<String> mTeamName = new ArrayList<String>(5);
        private List<Integer> mTeamColor = new ArrayList<Integer>(5);
        private List<Boolean> mAllowTeamColorEditing = new ArrayList<Boolean>(5);
        private List<Boolean> mAllowTeamNameEditing = new ArrayList<Boolean>(5);
        private List<String> mPlayerNames = new ArrayList<String>(5);
        
        public Builder(boolean useDummys, boolean allowPlayerColorEditing) {
            mParameters.putBoolean(PARAMETER_FLAG_USE_DUMMY_PLAYERS, useDummys);
            mParameters.putBoolean(PARAMETER_FLAG_ALLOW_PLAYER_COLOR_EDITING, allowPlayerColorEditing);
        }
        public void addTeam(int minPlayers, int maxPlayers, boolean isOptional, 
                String teamName, boolean allowTeamNameEditing, 
                int teamColor, boolean allowTeamColorEditing, String[] playerNames) {
            if (minPlayers < 1 || maxPlayers < minPlayers || maxPlayers <= 0) {
                throw new IllegalArgumentException("Illegal min/max players for team: " + minPlayers + "/" + maxPlayers);
            }
            mMinPlayers.add(minPlayers);
            mMaxPlayers.add(maxPlayers);
            mIsOptional.add(isOptional);
            mTeamName.add(teamName);
            mTeamColor.add(teamColor);
            mAllowTeamColorEditing.add(allowTeamColorEditing);
            mAllowTeamNameEditing.add(allowTeamNameEditing);
            int namesAdded = 0;
            if (playerNames != null) {
                for (String name : playerNames) {
                    if (namesAdded < maxPlayers) {
                        mPlayerNames.add(name);
                        namesAdded++;
                    }
                }
            }                
            for (int i = namesAdded; i < maxPlayers; i++) {
                mPlayerNames.add(null);
            }
        }
        public Bundle build() {
            if (mMinPlayers.size() == 0) {
                throw new IllegalStateException("Cannot build with no team added.");
            }
            mParameters.putIntArray(PARAMETER_TEAM_MIN_PLAYERS, toIntArray(mMinPlayers));
            mParameters.putIntArray(PARAMETER_TEAM_MAX_PLAYERS, toIntArray(mMaxPlayers));
            mParameters.putBooleanArray(PARAMETER_TEAM_IS_OPTIONAL, toBooleanArray(mIsOptional));
            mParameters.putStringArray(EXTRA_TEAM_NAMES, toStringArray(mTeamName));
            mParameters.putIntArray(EXTRA_TEAM_COLORS, toIntArray(mTeamColor));
            mParameters.putBooleanArray(PARAMETER_ALLOW_TEAM_COLOR_EDITING, toBooleanArray(mAllowTeamColorEditing));
            mParameters.putBooleanArray(PARAMETER_ALLOW_TEAM_NAME_EDITING, toBooleanArray(mAllowTeamNameEditing));
            mParameters.putStringArray(EXTRA_PLAYER_NAMES, toStringArray(mPlayerNames));
            return mParameters;
        }
        
        private static int[] toIntArray(List<Integer> list) {
            int[] a = new int[list.size()]; int index = 0;
            for (int i : list) {
                a[index++] = i;
            }
            return a;
        }
        
        private static boolean[] toBooleanArray(List<Boolean> list) {
            boolean[] a = new boolean[list.size()]; int index = 0;
            for (boolean b : list) {
                a[index++] = b;
            }
            return a;
        }   
        
        private static String[] toStringArray(List<String> list) {
            String[] a = new String[list.size()]; int index = 0;
            for (String s : list) {
                a[index++] = s;
            }
            return a;
        }
    }
    
    public TeamSetupTeamsController(int gameKey, FragmentActivity activity, NotifyMajorChangeCallback callback,
            Bundle parameters,
            LinearLayout teamsContainer) {
        mGameKey = gameKey;
        mTeamControllers = new TreeMap<Integer, TeamSetupViewController>();
        mParameters = parameters;
        mMaxTeamCount = mParameters.getIntArray(Builder.PARAMETER_TEAM_MIN_PLAYERS).length;
        mCallback = callback;
        mHiddenTeams = new TreeSet<Integer>();
        for (int i = 0; i < mMaxTeamCount; i++) {
            mHiddenTeams.add(Integer.valueOf(i));
        }
        mActivity = activity;
        mTeamsContainer = teamsContainer;
        mTeamsContainer.removeAllViews();
        mChoosingColorControllerIndex = parameters.getInt(STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX, -1);
        mChoosingPlayerControllerIndex = parameters.getInt(STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX, -1);
        init();
    }
    
    private void init() {
        int index = 0;
        String[] playerNames = mParameters.getStringArray(EXTRA_PLAYER_NAMES);
        for (int i = 0; i < mMaxTeamCount; i++) {
            int maxTeamSize = mParameters.getIntArray(Builder.PARAMETER_TEAM_MAX_PLAYERS)[i];
            boolean showController = false;
            if (!isTeamDeletable(i)) {
                // if team is not optional show it
                showController = true;
            } else if (playerNames != null) {
                // if there is a not-null name in the range for this team, then use a controller, else keep it hidden
                boolean hasNotNull = false;
                for (int j = index; j < index + maxTeamSize && j < playerNames.length; j++) {
                    hasNotNull |= playerNames[j] != null;
                }
                if (hasNotNull) {
                    showController = true;
                }
            }
            if (showController) {
                List<Player> range = null;
                if (playerNames != null) {
                    range = new ArrayList<Player>(maxTeamSize);
                    // get the players for the names, in case the player is a dummy by name, use the dummy in this case
                    PlayerPool pool = getPool();
                    for (int j = index; j < index + maxTeamSize && j < playerNames.length; j++) {
                        if (playerNames[j] == null) {
                            range.add(null);
                        } else {
                            int dummyNumber = DummyPlayer.extractNumber(playerNames[j]);
                            if (dummyNumber >= DummyPlayer.FIRST_NUMBER) {
                                range.add(new DummyPlayer(dummyNumber));
                            } else {
                                range.add(pool.populatePlayer(playerNames[j]));
                            }
                        }
                    }
                }
                addTeam(i, range);
            }
            index += maxTeamSize;
        }
        assertNoGapContained();
    }
    
    public boolean hasRequiredPlayers() {
        for (TeamSetupViewController ctr : mTeamControllers.values()) {
            if (!ctr.hasRequiredPlayers(true)) {
                return false;
            }
        }
        return true;
    }
    
    public List<Player> getAllPlayers(boolean includeDummys) {
        List<Player> team = new ArrayList<Player>();
        for (TeamSetupViewController ctr : mTeamControllers.values()) {
            team.addAll(ctr.getPlayers(includeDummys));
        }
        return team;
    }
    
    @Override
    public PlayerPool getPool() {
        return GameKey.getPool(mGameKey);
    }

    @Override
    public List<Player> toFilter() {
        List<Player> filter = new LinkedList<Player>();
        if (mTeamControllers != null) {
            for (TeamSetupViewController ctr : mTeamControllers.values()) {
                filter.addAll(ctr.toFilter());
            }
        }
        return filter;
    }

    @Override
    public void playerChosen(int playerIndex, Player chosen) {
        if (chosen == null || !isValidControllerIndex(mChoosingPlayerControllerIndex)) {
            return;
        }
        TeamSetupViewController ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
        if (ctr == null) {
            addTeam(mChoosingPlayerControllerIndex, null); 
            ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
        }
        if (ctr != null) {
            // maybe the player entered a dummy player name, then we want this dummy and not a player that mimiks the dummy but is not one
            Player reallyChosen = obtainAsDummyIfNeeded(chosen);
            if (reallyChosen == null) {
                reallyChosen = chosen;
            }
            ctr.playerChosen(playerIndex, reallyChosen);
            closeDummyNumberGaps();
        }
    }

    private boolean isValidControllerIndex(int index) {
        return index >= 0 && index < mMaxTeamCount;
    }
    
    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {
        if (!isValidControllerIndex(mChoosingColorControllerIndex)) {
            return;
        }
        TeamSetupViewController ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
        if (ctr == null) {
            addTeam(mChoosingPlayerControllerIndex, null); 
            ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
        }
        if (ctr != null) {
            ctr.notifyDataSetChanged();
        }
    }   

    @Override
    public void onColorChanged(int color) {
        if (!isValidControllerIndex(mChoosingColorControllerIndex)) {
            return;
        }
        TeamSetupViewController ctr = mTeamControllers.get(mChoosingColorControllerIndex);
        if (ctr == null) {
            addTeam(mChoosingColorControllerIndex, null); 
            ctr = mTeamControllers.get(mChoosingColorControllerIndex);
        }
        if (ctr != null) {
            ctr.setTeamColor(color);
        }
    }

    @Override
    public List<DummyPlayer> obtainNewDummys(int amount) {
        SortedSet<Integer> usedDummyNumbers = getUsedDummyNumbers();
        int freeNumber = DummyPlayer.FIRST_NUMBER;
        List<DummyPlayer> list = new ArrayList<DummyPlayer>(amount);
        if (!usedDummyNumbers.isEmpty()) {
            freeNumber = usedDummyNumbers.last() + 1;
        }
        for (int i = 0; i < amount; i++) {
            list.add(new DummyPlayer(freeNumber++));
        }
        return list;
    }

    private void assertNoGapContained() {
        SortedSet<Integer> usedDummyNumbers = getUsedDummyNumbers();
        Log.d("GameMemo", "Used dummy numbers: " + usedDummyNumbers);
        int last = DummyPlayer.FIRST_NUMBER - 1;
        for (int n : usedDummyNumbers) {
            if (n != last + 1) {
                throw new IllegalStateException("Gap found of dummy numbers: " + usedDummyNumbers);
            }
            last = n;
        }
    }
        
    private void closeDummyNumberGaps() {
        Log.d("GameMemo", "Used dummyNumbers before closing: " + getUsedDummyNumbers());
        SortedSet<Integer> usedDummyNumbers = getUsedDummyNumbers();
        SortedSet<Integer> correctNotTakenNumbers = new TreeSet<Integer>();
        SparseIntArray map = new SparseIntArray(usedDummyNumbers.size());
        // iterate through all correct numbers and find those that are not taken
        for (int number = 0; number < usedDummyNumbers.size(); number++) {
            Integer correctNumber = Integer.valueOf(number + DummyPlayer.FIRST_NUMBER);
            if (!usedDummyNumbers.contains(correctNumber)) {
                correctNotTakenNumbers.add(correctNumber);
            }
        }
        // map any wrong number to the correct number, if it is not wrong, then map it to itself
        Iterator<Integer> correctNumbersIt = correctNotTakenNumbers.iterator();
        for (int dummyNumber : usedDummyNumbers) {
            if (dummyNumber - DummyPlayer.FIRST_NUMBER < usedDummyNumbers.size()) {
                map.put(dummyNumber, dummyNumber); // map correct numbers to themselves
            } else {
                map.put(dummyNumber, correctNumbersIt.next());
            }
        }
        for (TeamSetupViewController ctr : mTeamControllers.values()) {
            for (int i = 0; i < ctr.getPlayerCount(); i++) {
                Player curr = ctr.getPlayer(i);
                if (curr != null && curr instanceof DummyPlayer) {
                    int currNumber = ((DummyPlayer) curr).getNumber();
                    int mapping = map.get(currNumber); 
                    if (mapping != currNumber) {
                        ctr.replacePlayer(i, new DummyPlayer(mapping));
                    }
                }
            }
        }
        Log.d("GameMemo", "Used dummyNumbers after closing: " + getUsedDummyNumbers());
    }
    
    private SortedSet<Integer> getUsedDummyNumbers() {
        SortedSet<Integer> usedDummyNumbers = new TreeSet<Integer>();
        for (TeamSetupViewController ctr : mTeamControllers.values()) {
            for (Player p : ctr.getPlayers(true)) {
                if (p instanceof DummyPlayer) {
                    usedDummyNumbers.add(((DummyPlayer) p).getNumber());
                }
            }
        }
        return usedDummyNumbers;
    }
    
    private Player obtainAsDummyIfNeeded(Player potentialDummy) {
        if (usesDummys() && potentialDummy != null) {
            return DummyPlayer.isDummyName(potentialDummy.getName()) ? obtainNewDummys(1).get(0) : potentialDummy;
        }
        return potentialDummy;
    }
    
    @Override
    public void choosePlayer(int teamIndex, int playerIndex) {
        mChoosingPlayerControllerIndex = teamIndex;
        // do not change colors of dummys (since this is also not supported, would only change color of copy of dummy in players pool
        Player oldPlayer = mTeamControllers.get(teamIndex).getPlayer(playerIndex);
        if (oldPlayer instanceof DummyPlayer) {
            oldPlayer = null;
        }
        ChoosePlayerDialogFragment dialog = ChoosePlayerDialogFragment.newInstance(playerIndex, 
                oldPlayer, mParameters.getBoolean(Builder.PARAMETER_FLAG_ALLOW_PLAYER_COLOR_EDITING));
        dialog.show(mActivity.getSupportFragmentManager(), "ChoosePlayerDialog");
    }

    @Override
    public void chooseTeamColor(int teamIndex) {
        mChoosingColorControllerIndex = teamIndex;
        showColorChooserDialog(mTeamControllers.get(teamIndex).getTeamColor(), "ChooseTeamColorDialog");        
    }
    
    private void showColorChooserDialog(int color, String tag) {
        ColorPickerDialog dialog = new ColorPickerDialog();
        Bundle args = new Bundle();
        args.putInt(ColorPickerDialog.EXTRA_COLOR, color);
        dialog.setArguments(args);
        dialog.show(mActivity.getSupportFragmentManager(), tag);
    }
    
    @Override
    public void notifyPlayerCountChanged() {
        closeDummyNumberGaps();
        mCallback.onMajorChange();
    }

    @Override
    public void requestTeamDelete(int teamIndex) {
        if (isTeamDeletable(teamIndex)) {
            removeTeam(teamIndex);
        }
    }

    @Override
    public void applyTheme(View view) {
        GameKey.applyTheme(mGameKey, mActivity.getResources(), view); 
    }

    public void applyTheme() {
        for (TeamSetupViewController ctr : mTeamControllers.values()) {
            ctr.applyTheme();
        }
    }
    
    public void resetFields() {
        for (Integer index : mTeamControllers.keySet()) {
            TeamSetupViewController ctr = mTeamControllers.get(index);
            if (ctr.isDeletable()) {
                removeTeam(index);
            } else {
                ctr.reset();
            }
        }
    }
    
    private void removeTeam(int teamIndex) {
        TeamSetupViewController ctr = mTeamControllers.remove(teamIndex);
        if (ctr != null) {
            mTeamsContainer.removeView(ctr.getView());
        }
        mHiddenTeams.add(teamIndex);
        closeDummyNumberGaps();
        mCallback.onMajorChange();
    }

    public boolean hasMissingTeam() {
        return mTeamControllers.size() < mMaxTeamCount;
    }
    
    public void addMissingTeam() {
        if (hasMissingTeam()) {
            Integer index = mHiddenTeams.first();
            addTeam(index, null);
            mTeamControllers.get(index).getView().requestFocus();
        }
    }
    
    private boolean isTeamDeletable(int teamIndex) {
        return mParameters.getBooleanArray(Builder.PARAMETER_TEAM_IS_OPTIONAL)[teamIndex];
    }
    
    private boolean usesDummys() {
        return mParameters.getBoolean(Builder.PARAMETER_FLAG_USE_DUMMY_PLAYERS);
    }
    
    private void addTeam(int teamIndex, List<Player> defaultPlayers) {
        if (teamIndex < 0 || teamIndex >= mMaxTeamCount) {
            return;
        }
        removeTeam(teamIndex);
        TeamSetupViewController ctr = new TeamSetupViewController(mActivity, teamIndex, 
                mParameters.getIntArray(Builder.PARAMETER_TEAM_MIN_PLAYERS)[teamIndex], 
                mParameters.getIntArray(Builder.PARAMETER_TEAM_MAX_PLAYERS)[teamIndex], 
                defaultPlayers, usesDummys(), 
                this, isTeamDeletable(teamIndex));
        int viewIndex = 0;
        for (int i : mTeamControllers.keySet()) {
            if (i > teamIndex) {
                break;
            } else {
                viewIndex++;
            }
        }
        mHiddenTeams.remove(Integer.valueOf(teamIndex));
        mTeamControllers.put(teamIndex, ctr);
        ctr.setTeamName(mParameters.getStringArray(EXTRA_TEAM_NAMES)[teamIndex], 
                mParameters.getBooleanArray(Builder.PARAMETER_ALLOW_TEAM_NAME_EDITING)[teamIndex]);
        ctr.setTeamColorChoosable(mParameters.getBooleanArray(Builder.PARAMETER_ALLOW_TEAM_COLOR_EDITING)[teamIndex]);
        ctr.setTeamColor(mParameters.getIntArray(EXTRA_TEAM_COLORS)[teamIndex]);
        ctr.applyTheme();
        mTeamsContainer.addView(ctr.getView(), viewIndex);
        mCallback.onMajorChange();
        assertNoGapContained();
    }

    public Bundle getParameters() {
        mParameters.putInt(STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX, mChoosingColorControllerIndex);
        mParameters.putInt(STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX, mChoosingPlayerControllerIndex);
        // refresh and store team names and colors
        String[] teamNames = mParameters.getStringArray(EXTRA_TEAM_NAMES);
        int[] teamColors = mParameters.getIntArray(EXTRA_TEAM_COLORS);
        for (int key : mTeamControllers.keySet()) {
            teamNames[key] = mTeamControllers.get(key).getTeamName();
            teamColors[key] = mTeamControllers.get(key).getTeamColor();
        }
        mParameters.putStringArray(EXTRA_TEAM_NAMES, teamNames);
        mParameters.putIntArray(EXTRA_TEAM_COLORS, teamColors);
        // store players
        mParameters.putStringArray(EXTRA_PLAYER_NAMES, allPlayersToArray());
        return mParameters;
    }
    
    private String[] allPlayersToArray() {
        List<String> allPlayerNames = new LinkedList<String>();
        for (int i = 0; i < mMaxTeamCount; i++) {
            if (mHiddenTeams.contains(i)) {
                // team not used, fill with null names
                for (int k = 0; k < mParameters.getIntArray(Builder.PARAMETER_TEAM_MAX_PLAYERS)[i]; k++) {
                    allPlayerNames.add(null);
                }
            } else {
                TeamSetupViewController ctr = mTeamControllers.get(i);
                assert ctr != null;
                List<Player> curr = ctr.getPlayers(true);
                for (Player p : curr) {
                    allPlayerNames.add(p.getName());
                }
                // fill every not taken player with a null name
                for (int k = 0; k < ctr.getMaxPlayers() - curr.size(); k++) {
                    allPlayerNames.add(null);
                }
            }
        }
        String[] playerNames = new String[allPlayerNames.size()];
        for (int i = 0; i < playerNames.length; i++) {
            playerNames[i] = allPlayerNames.get(i);
        }
        return playerNames;
    }
    
    public synchronized void performShuffle() {
        List<Integer> playerCountForTeams = new ArrayList<Integer>(mTeamControllers.size());
        List<Player> allPlayers = new ArrayList<Player>();
        for (TeamSetupViewController ctr : mTeamControllers.values()) {
            List<Player> curr = ctr.getPlayers(true);
            allPlayers.addAll(curr);
            playerCountForTeams.add(Integer.valueOf(curr.size())); 
        }
        Collections.shuffle(allPlayers);
        if (usesDummys()) {
            int index = 0;
            int currStartPos = 0;
            // give each team as many players as it had before, can be dummys or real players
            for (TeamSetupViewController ctr : mTeamControllers.values()) {
                assert playerCountForTeams.get(index) >= ctr.getMinPlayers();
                int currTeamNewPlayerCount = playerCountForTeams.get(index);
                ctr.setPlayers(allPlayers.subList(currStartPos, currStartPos + currTeamNewPlayerCount));
                currStartPos += currTeamNewPlayerCount;
                index++;
            }
        } else {
            SparseArray<List<Player>> playersForTeam = new SparseArray<List<Player>>(mTeamControllers.size());
            // first prefer filling teams to have the minimum required player count
            List<TeamSetupViewController> possibleControllers = new ArrayList<TeamSetupViewController>(mTeamControllers.values());
            Random rnd = new Random();
            Iterator<Player> it = allPlayers.iterator();
            while (it.hasNext() && possibleControllers.size() > 0) {
                Player p = it.next();
                TeamSetupViewController ctr = possibleControllers.get(rnd.nextInt(possibleControllers.size()));
                int index = ctr.getTeamNumber();
                List<Player> playersForTeamCurr = playersForTeam.get(index);
                if (playersForTeamCurr == null) {
                    playersForTeamCurr = new ArrayList<Player>(ctr.getMaxPlayers());
                    playersForTeam.put(index, playersForTeamCurr);
                }
                playersForTeamCurr.add(p);
                if (ctr.getMinPlayers() <= playersForTeamCurr.size()) {
                    possibleControllers.remove(ctr);
                }
            }
            if (it.hasNext()) {
                // second fill teams that still have capacity with remaining players
                for (TeamSetupViewController ctr : mTeamControllers.values()) {
                    if (playersForTeam.get(ctr.getTeamNumber()) == null || playersForTeam.get(ctr.getTeamNumber()).size() < ctr.getMaxPlayers() ) {
                        possibleControllers.add(ctr);
                    }
                }
                while (it.hasNext() && possibleControllers.size() > 0) {
                    Player p = it.next();
                    TeamSetupViewController ctr = possibleControllers.get(rnd.nextInt(possibleControllers.size()));
                    int index = ctr.getTeamNumber();
                    List<Player> playersForTeamCurr = playersForTeam.get(index);
                    if (playersForTeamCurr == null) {
                        playersForTeamCurr = new ArrayList<Player>(ctr.getMaxPlayers());
                        playersForTeam.put(index, playersForTeamCurr);
                    }
                    playersForTeamCurr.add(p);
                    if (playersForTeamCurr.size() >= ctr.getMaxPlayers()) {
                        possibleControllers.remove(ctr);
                    }
                }
                assert !it.hasNext();
            }
            for (TeamSetupViewController ctr : mTeamControllers.values()) {
                ctr.setPlayers(playersForTeam.get(ctr.getTeamNumber()));
            }
        }
        closeDummyNumberGaps();
    }
}
