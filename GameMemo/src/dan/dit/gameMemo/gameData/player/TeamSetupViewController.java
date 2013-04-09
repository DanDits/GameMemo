package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.util.LinearLayoutList;

public class TeamSetupViewController implements ChoosePlayerDialogListener {
	public static final boolean DEFAULT_TEAM_COLOR_CHOOSABLE = false;
	public static final int DEFAULT_TEAM_COLOR = 0xFF000000;
	private View mRoot;
	private Context mContext;
	private LayoutInflater mInflater;
	private EditText mTeamName;
	private ImageButton mTeamDelete;
	private int mTeamColor = DEFAULT_TEAM_COLOR;
	private Button mTeamColorChooser;
	private ImageButton mAddPlayer;
	private int mMinPlayer;
	private int mMaxPlayer;
	private LinearLayoutList mPlayerContainer;
	private List<Player> mPlayers;
	private TeamPlayerAdapter mTeamPlayerAdapter;
	private boolean mUseDummys;
	private TeamSetupCallback mCallback;
	private int mTeamNumber;
	
	public interface TeamSetupCallback extends ChoosePlayerDialogListener {
		DummyPlayer obtainNewDummy();
		void choosePlayer(int teamIndex, int playerInex);
		void chooseTeamColor(int teamIndex);		
		void notifyPlayerCountChanged();
		void requestTeamDelete(int teamIndex);
	}
	
	public TeamSetupViewController(Context context, int teamNumber, int minPlayer, int maxPlayer, List<Player> players, boolean useDummys, TeamSetupCallback callback) {
		if (minPlayer < 1 || maxPlayer < minPlayer) {
			throw new IllegalArgumentException("Min/Max player illegal: " + minPlayer + "/" + maxPlayer);
		} else if (callback == null) {
			throw new NullPointerException();
		}
		mTeamNumber = teamNumber;
		mCallback = callback;
		mMinPlayer = minPlayer;
		mMaxPlayer = maxPlayer;
		mContext = context;
		mUseDummys = useDummys;
		mInflater = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
		mRoot = mInflater.inflate(R.layout.game_setup_team, null);
		mTeamName = (EditText) mRoot.findViewById(R.id.team_name);
		setTeamName(null, false);
		mTeamDelete = (ImageButton) mRoot.findViewById(R.id.team_delete);
		setTeamDeletable(false);
		mTeamDelete.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mCallback.requestTeamDelete(mTeamNumber);
			}
		});
		mTeamColorChooser = (Button) mRoot.findViewById(R.id.team_color);
		mTeamColorChooser.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mCallback.chooseTeamColor(mTeamNumber);
			}
		});
		setTeamColorChoosable(DEFAULT_TEAM_COLOR_CHOOSABLE);
		mAddPlayer = (ImageButton) mRoot.findViewById(R.id.team_add_player);
		mAddPlayer.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addPlayer();
				choosePlayer(mPlayers.size() - 1);
			}
		});
		setTeamName(null, false);
		mPlayerContainer = (LinearLayoutList) mRoot.findViewById(R.id.player_container);
		mPlayers = new ArrayList<Player>(mMaxPlayer);
		setPlayers(players);
		mTeamPlayerAdapter = new TeamPlayerAdapter();
		mPlayerContainer.setAdapter(mTeamPlayerAdapter);
	}

	private void choosePlayer(int playerIndex) {
		if (playerIndex < 0 || playerIndex >= mPlayers.size()) {
			throw new ArrayIndexOutOfBoundsException(playerIndex);
		}
		mCallback.choosePlayer(mTeamNumber, playerIndex);
	}
	
	private boolean addPlayer() {
		return addPlayer(mPlayers.size());
	}
	
	private boolean addPlayer(int index) {
		if (mPlayers.size() < mMaxPlayer) {
			if (mUseDummys) {
				mPlayers.add(index, mCallback.obtainNewDummy());
			} else {
				mPlayers.add(index, new NoPlayer());
			}
			applyAddRemovePlayerState();
			return true;
		}
		applyAddRemovePlayerState();
		return false;
	}
	
	private void replacePlayer(int index, Player p) {
		if (mPlayers.get(index) instanceof DummyPlayer) {
			((DummyPlayer) mPlayers.get(index)).release();
		}
		if (p instanceof DummyPlayer) {
			((DummyPlayer) p).obtain();
		}
		mPlayers.set(index, p);
		applyAddRemovePlayerState();
	}
	
	private Player removePlayer(int index) {
		Player removed = null;
		boolean isDummy = mPlayers.get(index) instanceof DummyPlayer;
		if (isDummy || mPlayers.get(index) instanceof NoPlayer) {
			if (mPlayers.size() > mMinPlayer) {
				if (isDummy) {
					((DummyPlayer) mPlayers.get(index)).release();
				}
				removed = mPlayers.remove(index);
			}
		} else {
			removed = mPlayers.remove(index);
			if (mPlayers.size() < mMinPlayer) {
				addPlayer(index); // dummy or no player
			}
		}
		applyAddRemovePlayerState();
		return removed;
	}
	
	public void setTeamName(String name, boolean allowEditing) {
		if (TextUtils.isEmpty(name)) {
			mTeamName.setText(mContext.getResources().getString(R.string.team_default_name, mTeamNumber + 1));
		} else {
			mTeamName.setText(name);
		}
		mTeamName.setEnabled(allowEditing);
	}
	
	public View getView() {
		return mRoot;
	}
	
	private void clearPlayers() {
		for (Player p : mPlayers) {
			if (p instanceof DummyPlayer) {
				((DummyPlayer) p).release();
			}
		}
		mPlayers.clear();
	}
	
	private void fillRequiredSlots() {
		// fill required slots with no players or dummy players based on wishes
		for (int i = mPlayers.size(); i < mMinPlayer; i++) {
			addPlayer();
		}
	}
	
	public void reset() {
		clearPlayers();
		fillRequiredSlots();
		applyAddRemovePlayerState();
	}
	
	public void setPlayers(List<Player> players) {
		clearPlayers();
		// first use given players
		if (players != null) {
			for (Player p: players) {
				if (p != null && mPlayers.size() < mMaxPlayer) {
					if (p instanceof DummyPlayer) {
						((DummyPlayer) p).obtain();
					}
					mPlayers.add(p);
				}
			}
		}
		fillRequiredSlots();
		applyAddRemovePlayerState();
	}
	
	public boolean hasRequiredPlayers(boolean includeDummys) {
		int count = 0;
		for (Player p : mPlayers) {
			if (includeDummys && p instanceof DummyPlayer) {
				count++;
			} else if (!(p instanceof DummyPlayer || p instanceof NoPlayer)) {
				count++;
			}
		}
		return count >= mMinPlayer;
	}
	
	public List<Player> getPlayers(boolean includeDummys) {
		// exclude NoPlayer (and DummyPlayer if wanted)
		List<Player> players = new ArrayList<Player>(mPlayers.size());
		for (Player p : mPlayers) {
			if (!(p instanceof NoPlayer) && (includeDummys || !(p instanceof DummyPlayer))) {
				players.add(p);
			}
		}
		return players;
	}
	
	private void applyAddRemovePlayerState() {
		boolean enableAdd = mPlayers.size() < mMaxPlayer;
		mAddPlayer.setEnabled(enableAdd);
		mAddPlayer.setVisibility(enableAdd ? View.VISIBLE : View.GONE);
		if (mTeamPlayerAdapter != null) {
			mTeamPlayerAdapter.notifyDataSetChanged();
		}
		mCallback.notifyPlayerCountChanged();
	}
	
	private boolean canRemovePlayer() {
		return mPlayers.size() > mMinPlayer;
	}
	
	public void setTeamDeletable(boolean deleteable) {
		mTeamDelete.setVisibility(deleteable ? View.VISIBLE : View.GONE);
	}
	
	public String getTeamName() {
		return mTeamName.getText().toString();
	}
	
	public void setTeamColorChoosable(boolean choosable) {
		mTeamColorChooser.setVisibility(choosable ? View.VISIBLE : View.GONE);		
	}
	
	public void setTeamColor(int color) {
		mTeamColorChooser.setTextColor(color);
		mTeamColor = color;
	}
	
	public int getTeamColor() {
		return mTeamColor;
	}
	
	private class TeamPlayerAdapter extends BaseAdapter {
		private int mBackgroundRes = -1;
		private OnClickListener mPlayerRemoveListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				ViewHolder holder = (ViewHolder) v.getTag();
				removePlayer(holder.mPosition);
			}
		};
		private OnClickListener mPlayerNameChooserListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				ViewHolder holder = (ViewHolder) v.getTag();
				choosePlayer(holder.mPosition);
			}
		};
		
		@Override
		public int getCount() {
			return mPlayers.size();
		}

		@Override
		public Object getItem(int pos) {
			return mPlayers.get(pos);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View playerView = convertView;
			ViewHolder holder;
			if (playerView == null) {
				playerView = mInflater.inflate(R.layout.game_setup_player, null);
				holder = new ViewHolder();
				holder.mPlayerName = (Button) playerView.findViewById(R.id.player_name);
				holder.mPlayerDelete = (ImageButton) playerView.findViewById(R.id.player_delete);
				holder.mPlayerName.setOnClickListener(mPlayerNameChooserListener);
				holder.mPlayerDelete.setOnClickListener(mPlayerRemoveListener);
				playerView.setTag(holder);
				holder.mPlayerName.setTag(holder);
				holder.mPlayerDelete.setTag(holder);
			}
			holder = (ViewHolder) playerView.getTag();
			holder.mPosition = position;
			updateUI(holder, position);
			return playerView;
		}
		
		private void updateUI(ViewHolder holder, int position) {
			assert position == holder.mPosition;
			holder.mPlayerDelete.setVisibility(canRemovePlayer() ? View.VISIBLE : View.GONE);
			if (mPlayers.get(position) instanceof NoPlayer) {
				holder.mPlayerName.setText(mContext.getString(R.string.game_setup_select_player));
			} else {
				holder.mPlayerName.setText(mPlayers.get(position).getName());
			}
			holder.mPlayerName.setTextColor(mPlayers.get(position).getColor());
			if (mBackgroundRes != -1) {
				holder.mPlayerName.setBackgroundResource(mBackgroundRes);
				holder.mPlayerDelete.setBackgroundResource(mBackgroundRes);
			}
		}
	}

	private static class ViewHolder {
		int mPosition;
		Button mPlayerName;
		ImageButton mPlayerDelete;
	}
	
	@Override
	public PlayerPool getPool() {
		assert false; // method not being used
		return mCallback.getPool();
	}

	@Override
	public List<Player> toFilter() {
		return getPlayers(true);
	}

	@Override
	public void playerChosen(int playerIndex, Player chosen) {
		// only accept valid new player
		if (chosen == null || mCallback.toFilter().contains(chosen)) return;
		replacePlayer(playerIndex, chosen);
	}

	public int getMaxPlayers() {
		return mMaxPlayer;
	}

	public int getMinPlayers() {
		return mMinPlayer;
	}
	
	public Player getPlayer(int index) {
		Player p = mPlayers.get(index);
		if (p instanceof NoPlayer) {
			return null;
		} 
		return p;
	}
	
	public void applyBackgroundTheme(int btnResId) {
		mTeamPlayerAdapter.mBackgroundRes = btnResId;
		mTeamColorChooser.setBackgroundResource(btnResId);
		mAddPlayer.setBackgroundResource(btnResId);
		mTeamPlayerAdapter.notifyDataSetChanged();
		mTeamDelete.setBackgroundResource(btnResId);
	}

	public void close() {
		clearPlayers();
	}

	public int getTeamNumber() {
		return mTeamNumber;
	}

	@Override
	public void onPlayerColorChanged(int arg, Player concernedPlayer) {
		// player color changed
		mTeamPlayerAdapter.notifyDataSetChanged();
	}

	public void notifyDataSetChanged() {
		mTeamPlayerAdapter.notifyDataSetChanged();
	}
}
