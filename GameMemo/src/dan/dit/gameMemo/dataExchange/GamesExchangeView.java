package dan.dit.gameMemo.dataExchange;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import dan.dit.gameMemo.R;


public class GamesExchangeView extends LinearLayout {
	private ProgressBar mProgress;
	private Button mShowGames;
	private CharSequence mShowGamesBaseText;
	
	public GamesExchangeView(Context context) {
		super(context);
		initLayout(context);
	}
	
	public GamesExchangeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initLayout(context);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public GamesExchangeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initLayout(context);
	}
	
	private void initLayout(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View child = inflater.inflate(R.layout.games_exchange_overview, this, true);
		mShowGames = (Button) child.findViewById(R.id.open_games_overview);
		mShowGamesBaseText = mShowGames.getText();
		mProgress = (ProgressBar) child.findViewById(R.id.exchange_progress);
	}
	
	public void setProgress(int progress) {
		mProgress.setProgress(progress);
	}
	
	public void setMaxProgress(int max) {
		mProgress.setMax(max);
	}
	
	public void addProgress(int delta) {
		mProgress.setProgress(mProgress.getProgress() + delta);
	}
	
	public void setProgressIndeterminate(boolean indeterminate) {
		mProgress.setIndeterminate(indeterminate);
	}
	
	public void setOnShowGamesClickListener(OnClickListener listener) {
		mShowGames.setOnClickListener(listener);
	}

	public void setSelectedGamesCount(int selected, int total) {
		mShowGames.setText(mShowGamesBaseText + " " + selected + "/" + total);
	}


}
