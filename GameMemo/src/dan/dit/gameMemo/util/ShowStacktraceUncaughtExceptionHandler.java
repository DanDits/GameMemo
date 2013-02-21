package dan.dit.gameMemo.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;


import android.content.Context;
import android.content.Intent;

public class ShowStacktraceUncaughtExceptionHandler implements UncaughtExceptionHandler {
	private final Context context;
	
	public ShowStacktraceUncaughtExceptionHandler(Context context) {
		this.context = context;
		if (context == null) {
			throw new NullPointerException();
		}
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable t) {        
		StringWriter stackTrace = new StringWriter();
		t.printStackTrace(new PrintWriter(stackTrace));
		String stacktraceString = stackTrace.toString();
		Intent i = new Intent(context, DebugShowStacktraceActivity.class);
		i.putExtra(DebugShowStacktraceActivity.EXTRAS_STACKTRACE, stacktraceString);
		context.startActivity(i);
		android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
	}
	
}
