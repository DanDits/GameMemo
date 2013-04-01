package dan.dit.gameMemo.gameData.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import dan.dit.gameMemo.R;


public class TeamViewController {
	private View mRoot;
	private Context mContext;
	private LayoutInflater mInflater;
	private EditText mTeamName;
	private Button mTeamDelete;
	private int mTeamColor;
	private Button mTeamColorChooser;
	private Button mAddPlayer;
	private int mMinPlayer;
	private int mMaxPlayer;
	
	public TeamViewController(Context context, int minPlayer, int maxPlayer, String teamName, String[] playerNames) {
		if (minPlayer < 1 || maxPlayer < minPlayer) {
			throw new IllegalArgumentException("Min/Max player illegal: " + minPlayer + "/" + maxPlayer);
		}
		mMinPlayer = minPlayer;
		mMaxPlayer = maxPlayer;
		mContext = context;
		mInflater = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
		mRoot = mInflater.inflate(R.layout.game_setup_team, null);
		mTeamName = (EditText) mRoot.findViewById(R.id.team_name);
		mTeamDelete = (Button) mRoot.findViewById(R.id.team_delete);
		mTeamColorChooser = (Button) mRoot.findViewById(R.id.team_color);
		mAddPlayer = (Button) mRoot.findViewById(R.id.team_add_player);
		mTeamName.setText(context.getResources().getString(R.string.team_default_name));
		initPlayers(playerNames);
	}
	
	private void initPlayers(String[] playerNames) {
		//TODO creates and saves team_player views for minPlayers, giving them the given names or else 'pick player' if null or no name given
		applyAddPlayerState();
	}
	
	private void applyAddPlayerState() {
		//TODO called at the beginning and whenever a player is deleted or added and enables disables addPlayer button
	}
	
	public void offerDefaultForUnpickedPlayers() {
		//TODO needs callback : nextFreeDefaultPlayerId()
	}
	
	public int getNextFreeDefaultPlayerId() {
		// TODO check default player names if used and return the next unpicked number, default return: 1
		return 1;
	}
	
	public void setTeamDeletable(boolean deleteable) {
		mTeamDelete.setVisibility(deleteable ? View.VISIBLE : View.GONE);
	}

	public void setTeamNameEditable(boolean editable) {
		mTeamName.setEnabled(editable);
	}
	
	public String getTeamName() {
		return mTeamName.getText().toString();
	}
	
	public void setTeamColorChoosable(boolean choosable) {
		mTeamColorChooser.setVisibility(choosable ? View.VISIBLE : View.GONE);		
	}
	
	public void setTeamColor(int color) {
		mTeamName.setTextColor(color);
		mTeamColor = color;
	}
	
	public int getTeamColor() {
		return mTeamColor;
	}
}
