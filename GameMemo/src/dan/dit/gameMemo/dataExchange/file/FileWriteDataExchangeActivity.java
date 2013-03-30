package dan.dit.gameMemo.dataExchange.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.DataExchangeActivity;
import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.gameData.game.GameKey;

public class FileWriteDataExchangeActivity extends DataExchangeActivity {
	public static final String EXTRA_FLAG_START_SHARE_IMMEDIATELY = "dan.dit.gameMemo.START_SHARE_IMMEDIATELY";
	private static final String GAMES_DATA_FILE_NAME = "games.gamememo";
	private FileWriteService mService;
	private File mTempGamesData;
	private List<Integer> mTempGamesDataForGames;
	private Button mStartShare;
	private boolean mStartImmediately;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File cacheDir = getExternalCacheDir();
			if (cacheDir != null) {
				mTempGamesData = new File(cacheDir, GAMES_DATA_FILE_NAME);
			}
		}
		if (mTempGamesData == null) {
			Toast.makeText(this, getResources().getString(R.string.share_storage_unavailable), Toast.LENGTH_SHORT).show();
			setResult(RESULT_CANCELED);
			super.onCreate(savedInstanceState);
			finish();
			return;
		}
		int[] toShare = GameKey.calculateUsedGames(getContentResolver());
		if (toShare.length == 0) {
			setResult(RESULT_CANCELED);
			Toast.makeText(this, getResources().getString(R.string.share_no_data), Toast.LENGTH_SHORT).show();
			super.onCreate(savedInstanceState);
			finish();
			return;
		}
		getIntent().putExtra(DataExchangeActivity.EXTRA_ALL_GAMES, toShare);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data_exchange_filewrite);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mStartImmediately = extras.getBoolean(EXTRA_FLAG_START_SHARE_IMMEDIATELY);
		}
		mStartShare = (Button) findViewById(R.id.share);
		mStartShare.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startShare();
			}
			
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (mStartImmediately) {
			mStartImmediately = false;
			startShare();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mService != null) {
			try {
				mService.close();
			} catch (IOException e) {
				// failed closing file stream but service still closed, ignore
			}
		}
	}
	
	@Override
	protected ExchangeService getExchangeService() {
		return mService;
	}

    @Override
    protected void onConnectionTerminated(int successfullyExchanged) {
    	super.onConnectionTerminated(successfullyExchanged);
    	if (successfullyExchanged == 0) {
    		onFailure();
    	} else {
    		shareGamesData();
    	}
    	try {
			mService.close();
		} catch (IOException e) {
			// failed closing file stream but service still closed, ignore
		}
    	mStartShare.setEnabled(true);
    }
    
    private void onFailure() {
		Toast.makeText(this, getResources().getString(R.string.data_exchange_filewriter_failed), Toast.LENGTH_SHORT).show();
    	mStartShare.setEnabled(true);    	
    }
    
    private void shareGamesData() {  
    	Intent shareIntent = new Intent();
    	shareIntent.setAction(Intent.ACTION_SEND);
    	shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mTempGamesData));
    	shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
    	shareIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_text));
    	shareIntent.setType("application/gamememo");
    	startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
    }
    
	@Override
	protected void setConnectionStatusText(int newState) {
		// not sent by FileWriteService
	}
	
	private void startShare() {
		mStartShare.setEnabled(false);
		int[] shareFor = mManager.getSelectedGames();
		if (mTempGamesDataForGames == null || mTempGamesDataForGames.size() != shareFor.length || !containsAllGames(shareFor)) {
			if (mTempGamesData.exists() && !mTempGamesData.delete()) {
				onFailure();
	    		return;
			}
			mTempGamesDataForGames = new ArrayList<Integer>(shareFor.length);
			for (int gameKey : shareFor) {
				mTempGamesDataForGames.add(Integer.valueOf(gameKey));
			}
			try {
				mService = new FileWriteService(mHandler, getContentResolver(), mTempGamesData);
			} catch (IOException e) {
				onFailure();
				return;
			}
		} else {
			shareGamesData();
			mStartShare.setEnabled(true);
		}
	}

	private boolean containsAllGames(int[] check) {
		for (int curr : check) {
			if (!mTempGamesDataForGames.contains(Integer.valueOf(curr))) {
				return false;
			}
		}
		return true;
	}
}
