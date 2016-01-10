package dan.dit.gameMemo.util;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public final class ActivityUtil {
    private ActivityUtil() {} // not instantiable
    
    public static final void applyUncaughtExceptionHandler(Context context) {
        // remove for release build
        Thread.setDefaultUncaughtExceptionHandler(new ShowStacktraceUncaughtExceptionHandler(context));
    }
    
    /**
     * If there is an action bar, this method hides or shows it, else does nothing.
     * @param act The activity which's ActionBar (if available) is to be shown or hidden.
     * @param hide If <code>true</code> the bar will be hidden, else shown if available.
     */
    @SuppressLint("NewApi")
	public static final void hideActionBar(Activity act, boolean hide) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar bar = act.getActionBar();
            if (bar != null) {
                if (hide) {
                    bar.hide();
                } else {
                    bar.show();
                }
            }
        }
    }
    
    /**
     * Applies the editor changes if running on a build version where this method is available, else
     * commits the changes.
     * @param editor The editor which's changes are to be commited or applied.
     */
    public static final void commitOrApplySharedPreferencesEditor(SharedPreferences.Editor editor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }
}
