package dan.dit.gameMemo.appCore.binokel;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.binokel.BinokelGame;
import dan.dit.gameMemo.gameData.game.binokel.BinokelRound;
import dan.dit.gameMemo.gameData.game.tichu.TichuBid;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.game.tichu.TichuRound;

public class BinokelGameRoundAdapter extends ArrayAdapter<GameRound> {

	private int layoutResourceId;
	private BinokelGame game;
	private LayoutInflater inflater;
	private Context context;

	public BinokelGameRoundAdapter(BinokelGame game, Context context, int layoutResourceId, List<GameRound>
			data) {
		super(context, layoutResourceId, data);
		this.game = game;
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		inflater = ((Activity)context).getLayoutInflater();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			// if round row layout not yet created, create with default round layout
			row = inflater.inflate(layoutResourceId, parent, false);
		}
		visualizeReizen(row, position);
		visualizeScores(row, position);
		
		return row;
	}
	
	private void visualizeReizen(View row, int position) {
        ImageView[] teamsReizenWon = new ImageView[] {
                (ImageView) row.findViewById(R.id.reizenwon_team1),
                (ImageView) row.findViewById(R.id.reizenwon_team2),
                (ImageView) row.findViewById(R.id.reizenwon_team3)
        };
        for (int i = 0; i < game.getTeamsCount(); i++) {
            boolean won = ((BinokelRound) game.getRound(position)).getReizenWinningTeam() == i;
            teamsReizenWon[i].setVisibility(won ? View.VISIBLE : View.INVISIBLE);
        }
	}
	
	private void visualizeScores(View row, int position) {

		TextView[] teamsDelta = new TextView[] {
                (TextView) row.findViewById(R.id.team1_delta),
                (TextView) row.findViewById(R.id.team2_delta),
                (TextView) row.findViewById(R.id.team3_delta)
        };
        TextView[] teamsScore = new TextView[]{
                (TextView) row.findViewById(R.id.team1_score),
                (TextView) row.findViewById(R.id.team2_score),
                (TextView) row.findViewById(R.id.team3_score)
        };
        boolean isLastRound = position == game.getRoundCount() -1;

        if (game.getTeamsCount() < 3) {
            row.findViewById(R.id.team3_container).setVisibility(View.GONE);
        }

		if ((position == game.getRoundCount() - 1) && position > 0) {
			for (int i = 0; i < game.getTeamsCount(); i++) {
                visualizeDelta(teamsDelta[i], position, i, isLastRound);
            }
		} else {
            for (TextView view : teamsDelta) {
                view.setText("");
            }
		}

        for (int i = 0; i < game.getTeamsCount(); i++) {
            visualizeScore(teamsScore[i], position, i, isLastRound);
        }
	}

    private void visualizeScore(TextView view, int position, int teamIndex, boolean isLastRound) {
        view.setText(String.valueOf(game.getScoreUpToRound(position, teamIndex)));
        applyIsLastRound(view, isLastRound);
    }

    private void visualizeDelta(TextView view, int position, int teamIndex, boolean isLastRound) {
        int delta = game.getScoreUpToRound(position, teamIndex) - game.getScoreUpToRound(position - 1,
                teamIndex);
        view.setText((delta >= 0) ? ("+" + String.valueOf(delta)) : String.valueOf(delta));
        applyIsLastRound(view, isLastRound);
    }

    private void applyIsLastRound(TextView view, boolean isLastRound) {
        if (isLastRound) {
            view.setPaintFlags(view.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
        } else {
            view.setPaintFlags(view.getPaintFlags() & (~Paint.FAKE_BOLD_TEXT_FLAG));
        }
    }
}
