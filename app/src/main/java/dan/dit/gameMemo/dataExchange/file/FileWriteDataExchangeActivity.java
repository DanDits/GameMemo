package dan.dit.gameMemo.dataExchange.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.DataExchangeActivity;
import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.GameStorageHelper;

public class FileWriteDataExchangeActivity extends DataExchangeActivity {
	private static final String EXTRA_FLAG_START_SHARE_IMMEDIATELY = "dan.dit.gameMemo.START_SHARE_IMMEDIATELY";
	private static final String EXTENSION = ".gamememo";
	private static final String GAMES_DATA_FILE_NAME = "games" + EXTENSION;
	private static final String SAVE_GAME_PREFIX = "save_";
	private FileWriteService mService;
    private boolean mIsShareConnection;
	private File mTempGamesData;
	private List<Integer> mTempGamesDataForGames;
	private Button mStartShare;
	private Button mSaveToSD;
	private boolean mStartImmediately;
	
	public static Intent newInstance(Context packageContext, int gameKey,
			boolean startImmediately, Collection<Long> checkedStarttimes) {
		Intent i = new Intent(packageContext, FileWriteDataExchangeActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, new int[] {gameKey});
		i.putExtra(FileWriteDataExchangeActivity.EXTRA_FLAG_START_SHARE_IMMEDIATELY, startImmediately);
		if (checkedStarttimes.size() > 0) {
			i.putExtra(DataExchangeActivity.EXTRA_SINGLE_GAME_OFFERS, GameKey.toArray(checkedStarttimes));
		}
		return i;
	}
	
	public static Intent newInstance(Context packageContext, int[] gameKeys, boolean startImmediately) {
		Intent i = new Intent(packageContext, FileWriteDataExchangeActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, gameKeys);
		i.putExtra(FileWriteDataExchangeActivity.EXTRA_FLAG_START_SHARE_IMMEDIATELY, startImmediately);
		return i;
	}
	
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
				startShareOrSave(true);
			}
			
		});
		mSaveToSD = (Button) findViewById(R.id.save_to_sd);
		mSaveToSD.setText(getResources().getString(R.string.save_to_sd_path, getStorageDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).toString()));
		mSaveToSD.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startShareOrSave(false);
				
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		calculateAllAvailableGames();
		if (mStartImmediately) {
			mStartImmediately = false;
			startShareOrSave(true);
		}
	}
	
	private void calculateAllAvailableGames() {
       new GameStorageHelper.RequestStoredGamesCountTask(getContentResolver(), new GameStorageHelper.RequestStoredGamesCountTask.Callback() {
            
            @Override
            public void receiveStoredGamesCount(Integer[] gameKeys, Integer[] gamesCount) {
                // filter out gamekeys that have save games
                List<Integer> newAllGames = new ArrayList<Integer>(gameKeys.length);
                for (int i = 0; i < gameKeys.length; i++) {
                    if (gamesCount[i] > 0) {
                        newAllGames.add(gameKeys[i]);
                    }
                }
                if (mTempGamesDataForGames != null) {
                    mTempGamesDataForGames.retainAll(newAllGames); // so that mTempGamesDataForGames is a subset of newAllGames
                }
                mManager.setAllGames(newAllGames, getResources());
            }
        }).execute(GameKey.toIntegerArray(GameKey.ALL_GAMES));
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
    		onFailure(3);
    	} else {
    		startShareOrSave(mIsShareConnection);
    	}
    	try {
			mService.close();
		} catch (IOException e) {
			// failed closing file stream but service still closed, ignore
		}
    	mStartShare.setEnabled(true);  	
    	mSaveToSD.setEnabled(true);  
    }
    
    private void onFailure(int errorCode) {
        Log.d("GameMemo", "OnFailure in FileWriteDataExchangeActivity. Error code:" + errorCode);
		Toast.makeText(this, getResources().getString(R.string.data_exchange_filewriter_failed), Toast.LENGTH_SHORT).show();
    	mStartShare.setEnabled(true);    	
    	mSaveToSD.setEnabled(true);  
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
    
    private File getStorageDir(File baseDir) {
    	return new File(baseDir.toString() +  "/gamememo");
    }
    
    private void saveGamesData() throws IOException {
    	File extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    	if (extStorageDirectory != null) {
    		if (extStorageDirectory.mkdirs() || extStorageDirectory.isDirectory()) {
    			// dir exists
    			File myDir = getStorageDir(extStorageDirectory);
    			if (myDir.mkdir() || myDir.isDirectory()) {
    				// myDir exists
    				int freeNumber = findNextFreeFileNumber(myDir);
    				File saveFile = new File(myDir, SAVE_GAME_PREFIX + Integer.toString(freeNumber) + EXTENSION);
    				mSaveToSD.setText(getResources().getString(R.string.save_to_sd_path_done, saveFile.toString()));
    				assert !saveFile.exists();
    				FileChannel source = null;
    				FileChannel destination = null;
    			    try {
    			        source = new FileInputStream(mTempGamesData).getChannel();
    			        destination = new FileOutputStream(saveFile).getChannel();
    			        destination.transferFrom(source, 0, source.size());
    			    }
    			    finally {
    			        if(source != null) {
    			            source.close();
    			        }
    			        if(destination != null) {
    			            destination.close();
    			        }
    			    }
    			}
    		}
    		
    	}
    }
    
	private int findNextFreeFileNumber(File inDir) {
    	TreeSet<Integer> taken = new TreeSet<Integer>();
    	File[] filesInDir = inDir.listFiles();
    	if (filesInDir == null) {
    	    return 404;
    	}
    	for (File f : filesInDir) {
    		String name = f.getName();
    		if (name.startsWith(SAVE_GAME_PREFIX) && name.length() > SAVE_GAME_PREFIX.length() + EXTENSION.length()) {
    			String rest = name.substring(SAVE_GAME_PREFIX.length(), name.length() - EXTENSION.length());
    			int number = -1;
    			try {
    				number = Integer.parseInt(rest);
    			} catch (NumberFormatException nfe) {
    				// ignore, no valid gamememo savegame
    			}
    			if (number >= 0) {
    				taken.add(Integer.valueOf(number));
    			}
    		}
    	}
    	final Integer firstNumber = Integer.valueOf(0);
    	Integer currNumber = null;
    	
    	/*// Use to take the lowest free number >= firstNumber
    	 * currNumber = firstNumber;
    	while (taken.contains(currNumber)) {
    		currNumber = currNumber + 1; // cannot use TreeSet methods like higher since I support version 8
    	}*/
    	// Use to take a number by one higher than the highest number
    	currNumber = taken.isEmpty() ? firstNumber : taken.last();
    	currNumber = currNumber == null ? firstNumber : (currNumber + 1);
    	return currNumber.intValue();
    }
    
	@Override
	protected void setConnectionStatusText(int newState) {
		// not sent by FileWriteService
	}
	
	private boolean requiresData(int[] shareFor) {
		return mTempGamesDataForGames == null || mTempGamesDataForGames.size() != shareFor.length || !containsAllGames(shareFor);
	}
	
	private void initData(int[] shareFor) {
		if (mTempGamesData.exists() && !mTempGamesData.delete()) {
			onFailure(1);
    		return;
		}
		mTempGamesDataForGames = new ArrayList<Integer>(shareFor.length);
		for (int gameKey : shareFor) {
			mTempGamesDataForGames.add(Integer.valueOf(gameKey));
		}
		try {
			mService = new FileWriteService(mHandler, getContentResolver(), mTempGamesData);
		} catch (IOException e) {
			onFailure(2);
			return;
		}
	}
	
	private void startShareOrSave(boolean share) {
		mStartShare.setEnabled(false);  	
    	mSaveToSD.setEnabled(false);  
		int[] shareFor = mManager.getSelectedGames();
		if (requiresData(shareFor)) {
		    mIsShareConnection = share;
			initData(shareFor);
		} else {
			if (share) {
				shareGamesData();
			} else {
				try {
					saveGamesData();
				} catch (IOException e) {
					mSaveToSD.setText(getResources().getString(R.string.save_to_sd_path_failed));
				}
			}
			mStartShare.setEnabled(true);  	
	    	mSaveToSD.setEnabled(true);  
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
