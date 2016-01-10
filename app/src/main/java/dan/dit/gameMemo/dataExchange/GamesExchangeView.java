package dan.dit.gameMemo.dataExchange;

import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;

/**
 * A view that holds some components to visualize the progress of a games exchange
 * and enable the user to select games to exchange.
 * @author Daniel
 *
 */
public class GamesExchangeView extends LinearLayout {
	private ProgressBar mProgress;
	private Button mShowGames;
	private Drawable mDefaultShowGamesBackground;
    private ColorStateList mDefaultShowGamesTextColor;
	
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

	public void setSelectedGames(List<Integer> selectedGames) {
		// only visualize the selection, do nothing else and do not save list of games
		Resources res = getContext().getResources();
		mShowGames.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		if (selectedGames.size() == 1) {
			int key = selectedGames.get(0);
			mShowGames.setCompoundDrawablesWithIntrinsicBounds(GameKey.getGameIconId(key), 0, 0, 0);
			if (mDefaultShowGamesBackground == null) {
				mDefaultShowGamesBackground = mShowGames.getBackground();
			}
			if (mDefaultShowGamesTextColor == null) {
			    mDefaultShowGamesTextColor = mShowGames.getTextColors();
			}
			GameKey.applyTheme(key, getResources(), mShowGames);
			mShowGames.setText(res.getString(R.string.games_selected_single, GameKey.getGameName(selectedGames.get(0), getResources())));
		} else if (selectedGames.size() < GameKey.ALL_GAMES.length) {
			mShowGames.setText(res.getString(R.string.games_selected, selectedGames.size(), GameKey.ALL_GAMES.length));
			resetShowGamesButton();
		} else if (selectedGames.size() == GameKey.ALL_GAMES.length) {
			mShowGames.setText(res.getString(R.string.games_selected_all));
			resetShowGamesButton();
		}
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void resetShowGamesButton() {
	    if (mDefaultShowGamesBackground != null) {
    		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
    			mShowGames.setBackgroundDrawable(mDefaultShowGamesBackground);
    		} else {
    			mShowGames.setBackground(mDefaultShowGamesBackground);
    		}
	    }
	    if (mDefaultShowGamesTextColor != null) {
	        mShowGames.setTextColor(mDefaultShowGamesTextColor);
	    }
	}
}
