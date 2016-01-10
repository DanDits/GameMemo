package dan.dit.gameMemo.appCore.tichu;

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
import dan.dit.gameMemo.gameData.game.tichu.TichuBid;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.game.tichu.TichuRound;
/**
 * This adapter is used to visualize single rounds of a TichuGame inside a ListView
 * of a {@link TichuGameDetailActivity}.<br>
 * For a round the user can optionally show tichu bids and show the delta to the previous round.
 * A row can be marked, which should be interpreted as the last selected row, as this row will get highlighted.
 * For each round, the score up to this round for each team will be displayed.
 * @author Daniel
 *
 */
public class TichuGameRoundAdapter extends ArrayAdapter<GameRound> {
	public static final boolean PREFERENCE_SHOW_DELTA_DEFAULT = true;
	public static final boolean PREFERENCE_SHOW_TICHUS_DEFAULT = true;
	private static final int TICHU_BID_SMALL_DRAWABLE_ID = R.drawable.tichu_small_bid_plain;
	private static final int TICHU_BID_SMALL_LOST_DRAWABLE_ID = R.drawable.tichu_small_bid_lost_plain;
	private static final int TICHU_BID_BIG_DRAWABLE_ID = R.drawable.tichu_big_bid_plain;
	private static final int TICHU_BID_BIG_LOST_DRAWABLE_ID = R.drawable.tichu_big_bid_lost_plain;
	
	private int layoutResourceId;
	private TichuGame game;
	private LayoutInflater inflater;
	private Context context;
	private boolean showDelta;
	private boolean showTichus;
	
	public TichuGameRoundAdapter(TichuGame game, Context context, int layoutResourceId, List<GameRound> data,
			boolean showDelta, boolean showTichus) {
		super(context, layoutResourceId, data);
		this.game = game;
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.showDelta = showDelta;
		this.showTichus = showTichus;
		inflater = ((Activity)context).getLayoutInflater();
	}
	
	/**
	 * Toggles the display behavior for round deltas.
	 * @param show If the delta to the previous round should be shown or not.
	 */
	public void setShowDelta(boolean show) {
		if (show != showDelta) {
			showDelta = show;
			notifyDataSetChanged();
		}
	}
	
	/**
	 * Toggles the display behavior for tichu bids.
	 * @param show If tichu bids should be shown or not.
	 */
	public void setShowTichus(boolean show) {
		if (show != showTichus) {
			showTichus = show;
			notifyDataSetChanged();
		}
	}

	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			// if round row layout not yet created, create with default round layout
			row = inflater.inflate(layoutResourceId, parent, false);
		}
		visualizeTichus(row, position);
		visualizeScores(row, position);
		
		return row;
	}
	
	private void visualizeTichus(View row, int position) {
		ImageView[] playerBids = new ImageView[TichuGame.TOTAL_PLAYERS];
		playerBids[0] = (ImageView) row.findViewById(R.id.player1_tichu_bid_image);
		playerBids[1] = (ImageView) row.findViewById(R.id.player2_tichu_bid_image);
		playerBids[2] = (ImageView) row.findViewById(R.id.player3_tichu_bid_image);
		playerBids[3] = (ImageView) row.findViewById(R.id.player4_tichu_bid_image);
		if (showTichus) {
			for (int i = TichuGame.PLAYER_ONE_ID; i <= TichuGame.PLAYER_FOUR_ID; i++) {
				TichuBid bid = ((TichuRound) getItem(position)).getTichuBid(i);
				int id = 0;
				switch (bid.getType()) {
				case SMALL:
					id = bid.isWon() ? TICHU_BID_SMALL_DRAWABLE_ID : TICHU_BID_SMALL_LOST_DRAWABLE_ID; 
					break;
				case BIG:
					id = bid.isWon() ? TICHU_BID_BIG_DRAWABLE_ID : TICHU_BID_BIG_LOST_DRAWABLE_ID;
					break;
				}
				if (id != 0) {
					playerBids[i - TichuGame.PLAYER_ONE_ID].setImageDrawable(context.getResources().getDrawable(id));
				} else {
					playerBids[i - TichuGame.PLAYER_ONE_ID].setImageDrawable(null);
				}
			}
		} else {
			for (int i = TichuGame.PLAYER_ONE_ID; i <= TichuGame.PLAYER_FOUR_ID; i++) {
				playerBids[i - TichuGame.PLAYER_ONE_ID].setImageDrawable(null);
			}
		}
	}
	
	private void visualizeScores(View row, int position) {

		TextView team1Delta = (TextView) row.findViewById(R.id.team1_delta);
		TextView team2Delta= (TextView) row.findViewById(R.id.team2_delta);
		TextView team1Score = (TextView) row.findViewById(R.id.team1_score);
		TextView team2Score= (TextView) row.findViewById(R.id.team2_score);

		// if last round or if wanted show delta, but not for the first round
		if ((position == game.getRoundCount() - 1 || showDelta) && position > 0) {
			int delta1 = game.getScoreUpToRound(position, true) - game.getScoreUpToRound(position - 1, true);
			team1Delta.setText((delta1 >= 0) ? ("+" + String.valueOf(delta1)) : String.valueOf(delta1));
			int delta2 = game.getScoreUpToRound(position, false) - game.getScoreUpToRound(position - 1, false);
			team2Delta.setText((delta2 >= 0) ? ("+" + String.valueOf(delta2)) : String.valueOf(delta2));
		} else {
			team1Delta.setText("");
			team2Delta.setText("");
		}
		team1Score.setText(String.valueOf(game.getScoreUpToRound(position, true)));
		team2Score.setText(String.valueOf(game.getScoreUpToRound(position, false)));
		if (position == game.getRoundCount() - 1) {
			// highlight last round score specially, since it is the current score
			team1Score.setPaintFlags(team1Score.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			team2Score.setPaintFlags(team2Score.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			team1Delta.setPaintFlags(team1Score.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			team2Delta.setPaintFlags(team2Score.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
		} else {
			team1Score.setPaintFlags(team1Score.getPaintFlags() & (~Paint.FAKE_BOLD_TEXT_FLAG));
			team2Score.setPaintFlags(team2Score.getPaintFlags() & (~Paint.FAKE_BOLD_TEXT_FLAG));
			team1Delta.setPaintFlags(team1Score.getPaintFlags() & (~Paint.FAKE_BOLD_TEXT_FLAG));
			team2Delta.setPaintFlags(team2Score.getPaintFlags() & (~Paint.FAKE_BOLD_TEXT_FLAG));
		}
	}
}
