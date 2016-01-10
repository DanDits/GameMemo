package dan.dit.gameMemo.util;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;

public class DebugShowStacktraceActivity extends Activity {
	public static final String EXTRAS_STACKTRACE = "dan.dit.gameMemo.STACKTRACE";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_single_text);
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		String stacktrace = extras.getString(EXTRAS_STACKTRACE);
		String debugMsg = "App crashed.. sorry, show this to Daniel: \n" + stacktrace;
		Log.e(TichuGame.GAME_NAME, debugMsg);
		((TextView) findViewById(R.id.debug_text)).setText(debugMsg);
	}
}
