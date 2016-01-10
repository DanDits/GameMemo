package dan.dit.gameMemo.appCore.doppelkopf;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRound;

public class DoppelkopfGameRoundAdapter extends ArrayAdapter<GameRound> {
	private static final int COLOR_ROUND_WON = 0xFF18B603;
	private static final int COLOR_ROUND_LOST = 0xFF000000;
	private static final int COLOR_ROUND_INACTIVE = 0xC0708483;
	
	private static final int ICON_SOLO_WON = R.drawable.s_icon_green;
	private static final int ICON_SOLO_LOST = R.drawable.s_icon;
	public static final boolean PREFERENCE_SHOW_DELTA_DEFAULT = false;
	
	private int layoutResourceId;
	private DoppelkopfGame game;
	private LayoutInflater inflater;
	private boolean mShowDelta;
	
	public DoppelkopfGameRoundAdapter(DoppelkopfGame game, Context context, int layoutResourceId, List<GameRound> data, boolean showDelta) {
		super(context, layoutResourceId, data);
		this.game = game;
		this.layoutResourceId = layoutResourceId;
		mShowDelta = showDelta;
		inflater = ((Activity)context).getLayoutInflater();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			// if round row layout not yet created, create with default round layout
			row = inflater.inflate(layoutResourceId, parent, false);
		}
		visualizeRound(row, position);
		
		return row;
	}
	
	public void setShowDelta(boolean show) {
		if (show != mShowDelta) {
			mShowDelta = show;
			notifyDataSetChanged();
		}
	}
	
	private void visualizeRound(View row, int position) {
		TextView[] playerInfos = new TextView[DoppelkopfGame.MAX_PLAYERS];
		playerInfos[0] = (TextView) row.findViewById(R.id.round_player1);
		playerInfos[1] = (TextView) row.findViewById(R.id.round_player2);
		playerInfos[2] = (TextView) row.findViewById(R.id.round_player3);
		playerInfos[3] = (TextView) row.findViewById(R.id.round_player4);
		playerInfos[4] = (TextView) row.findViewById(R.id.round_player5);
		ImageView[] playerImages = new ImageView[DoppelkopfGame.MAX_PLAYERS];
		playerImages[0] = (ImageView) row.findViewById(R.id.round_player1_image);
		playerImages[1] = (ImageView) row.findViewById(R.id.round_player2_image);
		playerImages[2] = (ImageView) row.findViewById(R.id.round_player3_image);
		playerImages[3] = (ImageView) row.findViewById(R.id.round_player4_image);
		playerImages[4] = (ImageView) row.findViewById(R.id.round_player5_image);
		if (game.getPlayerCount() == DoppelkopfGame.MAX_PLAYERS) {
            playerInfos[DoppelkopfGame.MAX_PLAYERS - 1].setVisibility(View.VISIBLE);
            playerImages[DoppelkopfGame.MAX_PLAYERS - 1].setVisibility(View.VISIBLE);
		}
		DoppelkopfRound round = ((DoppelkopfRound) getItem(position));
		int reScore = game.getRuleSystem().getTotalScore(true, round);
		int contraScore = game.getRuleSystem().getTotalScore(false, round);
		boolean reScoredMore =  reScore > contraScore;
		int maxScore = Math.max(reScore, contraScore);
		for (int i = 0; i < game.getPlayerCount(); i++) {
			playerInfos[i].setText(Integer.toString(game.getPlayerScoreUpToRound(i, position)));
			if (maxScore > 0 && ((reScoredMore && round.isPlayerRe(i)) || (!reScoredMore && !round.isPlayerRe(i)))) {
				playerInfos[i].setTextColor(COLOR_ROUND_WON);
			} else {
				playerInfos[i].setTextColor(COLOR_ROUND_LOST);				
			}
			if (round.isSolo() && round.isPlayerRe(i)) {
			    playerImages[i].setImageResource(reScoredMore ? ICON_SOLO_WON : ICON_SOLO_LOST);
			} else {
				playerImages[i].setImageResource(0);	
			}
			if (round.isPlayerRe(i)) {
				playerInfos[i].setPaintFlags(playerInfos[i].getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			} else {
				playerInfos[i].setPaintFlags(playerInfos[i].getPaintFlags() & (~Paint.FAKE_BOLD_TEXT_FLAG));				
			}
			if (!game.isPlayerActive(i, position)) {
				playerInfos[i].setTextColor(COLOR_ROUND_INACTIVE);
			}
		}
		TextView deltaView = (TextView) row.findViewById(R.id.round_delta);
		deltaView.setVisibility(mShowDelta ? View.VISIBLE : View.GONE );
		if (mShowDelta) {
			deltaView.setText((maxScore >= 0 ? "+" : "-") + Integer.toString(maxScore));
		}
	}
}
