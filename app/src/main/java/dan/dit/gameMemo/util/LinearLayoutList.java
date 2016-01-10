package dan.dit.gameMemo.util;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
/**
 * LinearLayout with an adapter. 
 * @author Daniel
 *
 */
public class LinearLayoutList extends LinearLayout {	
	public LinearLayoutList(Context context) {
		super(context);
	}

	public LinearLayoutList(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@SuppressLint("NewApi")
	public LinearLayoutList(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private BaseAdapter mAdapter;
	private DataSetObserver mObserver = new DataSetObserver() {
		public void onChanged() {
			dataSetChanged();
		}
		public void onInvalidated() { 
			dataSetInvalidated();
		}
	};

	/*public LinearLayoutList(Context context, LinearLayout layout, BaseAdapter adapter) {
		mContext = context;
		mLayout = layout;
		adapter_ = adapter;

		adapter_.registerDataSetObserver(observer_);
		dataSetChanged();
	}*/


	public void setAdapter(BaseAdapter adapter) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mObserver);
		}
		mAdapter = adapter;
		if (mAdapter != null) {
			mAdapter.registerDataSetObserver(mObserver);
		}
		dataSetChanged();
	}
	
	public BaseAdapter getAdapter() {
		return mAdapter;
	}
	
	private void dataSetInvalidated() {
		// no-op
	}

	private void dataSetChanged() {
		View current;
		Vector<View> changedViews = new Vector<View>();
		if (mAdapter != null) {
			for(int itemNr = 0 ; itemNr < mAdapter.getCount() ; itemNr++){
				current = getChildAt(itemNr);
				changedViews.add(mAdapter.getView(itemNr, current, this));
			}
		}
		removeAllViews();
		for(View v : changedViews) {
			addView(v);
		}
	}

}