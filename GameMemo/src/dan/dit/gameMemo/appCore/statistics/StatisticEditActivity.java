package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;
import dan.dit.gameMemo.gameData.statistics.StatisticsDbHelper;

public class StatisticEditActivity extends Activity {
    public static final String EXTRA_SELECTED_ATTRIBUTE = "dan.dit.gameMemo.SELECTED_ATTRIBUTE"; // String, identifier of attribute to select
    private int mGameKey;
    private GameStatisticAttributeManager mManager;
    private List<StatisticAttribute> mAllAttributes;
    private Spinner mAttributeSelect;
    private StatisticAttribute mSelected;
    private boolean mStateSelectionChange;
    private ImageButton mDelete;
    private Button mExtend;
    private EditText mName;
    private EditText mDescription;
    private EditText mCustomValue;
    private EditText mPriority;
    private ViewGroup mStatisticDataContainer;
    private Button mPresType;
    private Spinner mReferenceSelect;
    private List<StatisticAttribute> mAllReferenceStatistics;
    private ListView mSubAttributes;
    private ListView mSubAttributesAll;
    private AdvancedStatisticsAdapter mSubAttributesAdapter;
    private AdvancedStatisticsAdapter mSubAttributesAllAdapter;
    private TextView mBasedOnHint;
    
    public static Intent getIntent(Context context, int gameKey, StatisticAttribute selectedAttribute) {
        if (selectedAttribute == null) {
            throw new IllegalArgumentException("Must select an attribute to start.");
        }
        Intent i = new Intent(context, StatisticEditActivity.class);
        i.putExtra(GameKey.EXTRA_GAMEKEY, gameKey);
        i.putExtra(EXTRA_SELECTED_ATTRIBUTE, selectedAttribute.getIdentifier());
        return i;
    }
    
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGameKey = getIntent().getExtras().getInt(GameKey.EXTRA_GAMEKEY);
        mManager = GameKey.getGameStatisticAttributeManager(mGameKey);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getActionBar() != null) {
                getActionBar().setTitle(getResources().getString(R.string.statistics_edit_activity_header));
                getActionBar().setDisplayShowTitleEnabled(true);
                getActionBar().setDisplayHomeAsUpEnabled(true);
                getActionBar().setIcon(GameKey.getGameIconId(mGameKey));
            }
        }
        setContentView(R.layout.statistics_edit);
        mAttributeSelect = (Spinner) findViewById(R.id.attribute_select);
        mDelete = (ImageButton) findViewById(R.id.attribute_delete);
        mExtend = (Button) findViewById(R.id.attribute_extend);
        mName = (EditText) findViewById(R.id.attribute_name);
        mDescription = (EditText) findViewById(R.id.attribute_descr);
        mCustomValue = (EditText) findViewById(R.id.attribute_custom_value);
        mPriority = (EditText) findViewById(R.id.priority);
        mStatisticDataContainer = (ViewGroup) findViewById(R.id.statistic_data_container);
        mPresType = (Button) findViewById(R.id.pres_type);
        mReferenceSelect = (Spinner) findViewById(R.id.reference_select);
        mSubAttributes = (ListView) findViewById(R.id.sub_attrs);
        mSubAttributesAll = (ListView) findViewById(R.id.sub_attrs_all);
        mBasedOnHint = (TextView) findViewById(R.id.based_on_hint);
        init();
    }
    
    private void init() {
        GameKey.applyTheme(mGameKey, getResources(), mExtend);
        GameKey.applyTheme(mGameKey, getResources(), mPresType);
        GameKey.applyTheme(mGameKey, getResources(), mDelete);
        // attribute selection
        mAttributeSelect.requestFocus();
        mAttributeSelect.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                selectAttribute((StatisticAttribute) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
            
        });
        onRefreshAllAttributes();
        selectAttribute(mManager.getAttribute(getIntent().getExtras().getString(EXTRA_SELECTED_ATTRIBUTE)));
        // attribute deletion
        mDelete.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mManager.delete(mSelected.getIdentifier(), StatisticsDbHelper.getInstance().getWritableDatabase())) { 
                    onRefreshAllAttributes();
                    selectAttribute(mManager.getAllAttributes(true).get(0));
                }
            }
        });
        // attribute extension
        mExtend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onExtend();
            }
            
        });
        // attribute name, description and custom value
        mName.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {}

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onNameChange();
            }
            
        });
        mDescription.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {}

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onDescriptionChange();
            }
            
        });
        mCustomValue.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {}

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onCustomValueChange();
            }
            
        });
        mPriority.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onPriorityChange();
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
}
            
            @Override
            public void afterTextChanged(Editable s) {
 }
        });
        mReferenceSelect.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                GameStatistic stat = (GameStatistic) mSelected;
                if (stat != null) {
                    stat.setReferenceStatistic((GameStatistic) mReferenceSelect.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                GameStatistic stat = (GameStatistic) mSelected;
                if (stat != null) {
                    stat.setReferenceStatistic(null);
                }
            }
        });
        mSubAttributes.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                StatisticAttribute toDelete = (StatisticAttribute) mSubAttributes.getItemAtPosition(position);
                if (!mSelected.removeAttribute(toDelete)) {
                    Toast.makeText(StatisticEditActivity.this, getResources().getString(R.string.statistics_edit_sub_attribute_cannot_remove), Toast.LENGTH_SHORT).show();
                } else {
                    onSubAttributesChange();
                }
            }
        });
        mSubAttributesAll.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                if (mSelected.addAttribute((StatisticAttribute) mSubAttributesAll.getItemAtPosition(position))) {
                    onSubAttributesChange();
                }
            }
        });
        mPresType.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                GameStatistic stat = (GameStatistic) mSelected;
                if (stat != null) {
                    stat.setPresentationType((stat.getPresentationType() + 1) % GameStatistic.PRESENTATION_TYPES_COUNT);
                    applyReferenceSelection();
                }
            }
        });
    }
    
    @Override
    public void onDestroy() {
        onDeselect();
        super.onDestroy();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void selectAttribute(StatisticAttribute attr) {
        if (attr == null) {
            throw new IllegalArgumentException("Cannot select null attribute " + attr);
        }
        if (mStateSelectionChange || attr.equals(mSelected)) {
            return;
        }
        mStateSelectionChange = true;
        onDeselect();
        mSelected = attr;
        mAttributeSelect.setSelection(mAllAttributes.indexOf(attr));
        if (mSelected.getBaseAttribute() != null) {
            mBasedOnHint.setVisibility(View.VISIBLE);
            mBasedOnHint.setText(getResources().getString(R.string.statistic_based_on) + "\n" + mSelected.getBaseAttribute().getName(getResources()));
        } else {
            mBasedOnHint.setVisibility(View.GONE);
        }
        mDelete.setEnabled(mSelected.isUserAttribute());
        // StatisticAttribute data
        mName.setText(mSelected.getName(getResources()));
        mDescription.setText(mSelected.getDescription(getResources()));
        if (mSelected.requiresCustomValue()) {
            mCustomValue.setVisibility(View.VISIBLE);
            mCustomValue.setText(mSelected.getCustomValue());
        } else {
            mCustomValue.setVisibility(View.GONE);
        }
        mPriority.setText(String.valueOf(mSelected.getPriority()));
        mPriority.setEnabled(mSelected.isUserAttribute());
        // GameStatistic data
        if (mSelected instanceof GameStatistic) {
            mStatisticDataContainer.setVisibility(View.VISIBLE);
            applyReferenceSelection(); 
        } else {
            mStatisticDataContainer.setVisibility(View.GONE);
        }
        // subattributets
        onSubAttributesChange();
        // make uneditable if predefined attribute
        boolean editable = mSelected.isUserAttribute();
        mName.setEnabled(editable);
        mDescription.setEnabled(editable);
        mCustomValue.setEnabled(editable);
        mSubAttributesAdapter.setAllItemsDisabled(!editable);
        mSubAttributesAllAdapter.setAllItemsDisabled(!editable);
        mStateSelectionChange = false; 
    }
    
    private void onSubAttributesChange() {
        List<StatisticAttribute> subAttrs = new ArrayList<StatisticAttribute>(mSelected.getAttributes());
        mSubAttributesAdapter = new AdvancedStatisticsAdapter(this, subAttrs);
        mSubAttributesAdapter.setAttribute(mSelected);
        mSubAttributes.setAdapter(mSubAttributesAdapter);
        List<StatisticAttribute> allAttrs = mManager.getAllAttributes(true);
        allAttrs.removeAll(subAttrs);
        Iterator<StatisticAttribute> it = allAttrs.iterator();
        while (it.hasNext()) {
            if (!mSelected.canBeAdded(it.next())) {
                it.remove();
            }
        }
        mSubAttributesAllAdapter = new AdvancedStatisticsAdapter(this, allAttrs);
        mSubAttributesAll.setAdapter(mSubAttributesAllAdapter);
    }
    
    private void applyReferenceSelection() {
        if (mSelected != null && mSelected instanceof GameStatistic) {
            mPresType.setText(GameStatistic.getPresTypeTextResId(((GameStatistic) mSelected).getPresentationType()));
            if (!TextUtils.isEmpty(((GameStatistic) mSelected).getReference())) {
                mReferenceSelect.setSelection(mAllReferenceStatistics.indexOf(mManager.getStatistic(((GameStatistic) mSelected).getReference())));
            } else {
                mReferenceSelect.setSelection(0);
            }
            if (!mSelected.isUserAttribute() || ((GameStatistic) mSelected).getPresentationType() == GameStatistic.PRESENTATION_TYPE_ABSOLUTE) {
                mReferenceSelect.setEnabled(false);
            } else {
                mReferenceSelect.setEnabled(true);
            }
        }
    }
    
    /*
     * Called on initialization and whenever an attribute is added or deleted to make it appear in the list
     */
    private void onRefreshAllAttributes() {
        mAllAttributes = mManager.getAllAttributes(true);
        mAttributeSelect.setAdapter(new SimpleStatisticsAdapter(this, mAllAttributes));
        mAttributeSelect.setSelection(mAllAttributes.indexOf(mSelected));
        
        List<GameStatistic> stats = mManager.getStatistics(true);
        mAllReferenceStatistics = new ArrayList<StatisticAttribute>(stats.size() + 1);
        mAllReferenceStatistics.add(null);
        mAllReferenceStatistics.addAll(stats);
        mReferenceSelect.setAdapter(new SimpleStatisticsAdapter(this, mAllReferenceStatistics));
        applyReferenceSelection();
    }
    
    private void onDeselect() {
        if (mSelected != null && mSelected.isUserAttribute()) {
            mSelected.save(StatisticsDbHelper.getInstance().getWritableDatabase());
        }
    }
    
    private void onExtend() {
        StatisticAttribute.Builder builder = null;
        if (mSelected instanceof GameStatistic) {
            GameStatistic.Builder statBuilder = new GameStatistic.Builder(null, (GameStatistic) mSelected);
            builder = statBuilder;
        } else {
            builder = new StatisticAttribute.Builder(null, mSelected);
        }
        builder.setUserCreated();
        StatisticAttribute newAttr = builder.getAttribute();
        mManager.putAttributeInMap(newAttr);
        mSelected = null;
        onRefreshAllAttributes();
        selectAttribute(newAttr);
    }
    
    private void onNameChange() {
        if (mStateSelectionChange) {
            return;
        }
        mSelected.setName(mName.getText().toString());
        ((SimpleStatisticsAdapter) mAttributeSelect.getAdapter()).notifyDataSetChanged();
    }
    
    private void onDescriptionChange() {
        if (mStateSelectionChange) {
            return;
        }
        mSelected.setDescription(mDescription.getText().toString());
    }
    
    private void onCustomValueChange() {
        if (mStateSelectionChange) {
            return;
        }
        mSelected.setCustomValue(mCustomValue.getText().toString());
    }
    
    private void onPriorityChange() {
        if (mStateSelectionChange) {
            return;
        }
        try {
            mSelected.setPriority(Integer.parseInt(mPriority.getText().toString()));
        } catch (NumberFormatException nfe) {} // ignore
    }
}
