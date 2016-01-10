package dan.dit.gameMemo.appCore.numberInput;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.util.ActivityUtil;
import dan.dit.gameMemo.util.compaction.Compacter;

public class NumberInputController implements OperationCallback {
    private static final boolean DEFAULT_SAVE_HISTORY = true;
    private static final String PREF_FILE = "dan.dit.gameMemo.OPERATIONS_FOR_GAMES";
    private static final String OPERATIONS_DATA_PREFIX = "ops_";
    private ViewGroup mContainer;
    private View mRoot;
    private View mRedo;
    private View mUndo;
    private View mUnredoContainer;
    private Context mContext;
    private GridView mGrid;
    private int mGameKey;
    private List<Operation> mOps;
    private List<OperationListener> mListener;
    private List<OnOperationClickedListener> mOnClickListener;
    private OperationAdapter mAdapter;
    
    private boolean mSaveHistory;
    private Stack<Operation> mDoneOperations;
    private List<Operation> mUndoneOperations;
    private String mStorageKey;
    
    public NumberInputController(int gameKey, Context context, ViewGroup container, boolean operationsRemovable, String storageKey) {
        mContainer = container;
        mContext = context;
        mGameKey = gameKey;
        mSaveHistory = DEFAULT_SAVE_HISTORY;
        mStorageKey = storageKey == null ? "" : storageKey;
        mListener = new LinkedList<OperationListener>();
        mOnClickListener = new LinkedList<OnOperationClickedListener>();
        mRoot = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.number_input, mContainer);
        mGrid = (GridView) mRoot.findViewById(R.id.numberGrid);
        mUnredoContainer = mRoot.findViewById(R.id.unredo_container);
        mDoneOperations = new Stack<Operation>();
        mUndoneOperations = new LinkedList<Operation>();
        mGrid.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                    long id) {
                Operation op = mAdapter.getItem(position);
                if (op instanceof AbsoluteOperation || op instanceof ClearOperation) {
                    // since these operations do not really have a general inverse but rather a situational, we need to copy the op and set inverse
                    if (op instanceof AbsoluteOperation) {
                        op = new AbsoluteOperation(((AbsoluteOperation) op).getValue());
                    } else if (op instanceof ClearOperation) {
                        op = new ClearOperation();
                    }
                    Operation inverse = new AbsoluteOperation(getActiveListener().getNumber());
                    op.setInverse(inverse);
                    inverse.setInverse(op);
                }
                notifyClickedListeners(op, false);
                executeOperationOnActive(op, true, true);
                notifyClickedListeners(op, true);
            }
        });
        if (operationsRemovable) {
            mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
    
                @Override
                public boolean onItemLongClick(AdapterView<?> adapter, View view,
                        int position, long id) {
                    Operation toRemove = mAdapter.getItem(position);
                    if (!(toRemove instanceof NewOperation)) {
                        undoableRemoveOperation(toRemove);
                    } // never remove a New operation
                    return true;
                }
            });
        }
        mUndo = mRoot.findViewById(R.id.undo);
        mUndo.setEnabled(false);
        mUndo.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                undoLastOperation();
            }
        });
        GameKey.applyTheme(gameKey, context.getResources(), mUndo);
        mRedo = mRoot.findViewById(R.id.redo);
        mRedo.setEnabled(false);
        mRedo.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                redoLastOperation();
            }
        });
        GameKey.applyTheme(gameKey, context.getResources(), mRedo);
        loadOperations();
    }
    
    public void setStorageKey(String newKey) {
        if (!mStorageKey.equals(newKey)) {
            mStorageKey = newKey;
            saveOperations();
        }
    }
    
    public void setSaveHistory(boolean save) {
        mSaveHistory = save;
        mUnredoContainer.setVisibility(mSaveHistory ? View.VISIBLE : View.GONE);
    }
    
    private void undoLastOperation() {
        if (!mDoneOperations.isEmpty()) {
            Operation done = mDoneOperations.pop();
            mUndoneOperations.add(0, done);
            executeOperationOnActive(done.getInverseOperation(), false, false);
            mRedo.setEnabled(true);
            onDoneOperationsChange();
        } else {
            // not supposed to happen as button is disaled if mDoneOperations is empty
            Toast.makeText(mContext, "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void redoLastOperation() {
        if (!mUndoneOperations.isEmpty()) {
            Operation toRedo = mUndoneOperations.remove(0);
            executeOperationOnActive(toRedo, false, true);
            mRedo.setEnabled(!mUndoneOperations.isEmpty());
        }else {
            // not supposed to happen as button is diabled if mUndoneOperation is empty
            Toast.makeText(mContext, "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void notifyClickedListeners(Operation op, boolean operationDone) {
        for (OnOperationClickedListener l : mOnClickListener) {
            if (operationDone) {
                l.onOperationClickedAfterExecute(op);
            } else {
                l.onOperationClickedBeforeExecute(op);
            }
        }
    }
    
    public void executeCustomOperation(CustomOperation op) {
        boolean hasInverse = op.getInverseOperation() != null;
        notifyClickedListeners(op, false);
        executeOperationOnActive(op, true, hasInverse);
        notifyClickedListeners(op, true);
        if (!hasInverse) {
            // as we cannot make this undone but still did it, we have to clear the history
            clearHistory();         
        }
    }
    
    private void onDoneOperationsChange() {
        mUndo.setEnabled(!mDoneOperations.isEmpty());
    }
    
    private void executeOperationOnActive(Operation toExecute, boolean clearRedoables, boolean pAddToDone) {
        boolean addToDone = pAddToDone && mSaveHistory;
        if (addToDone) {
            if (mDoneOperations.size() > 0 ) {
                Operation lastDone = mDoneOperations.get(mDoneOperations.size() - 1);
                if (lastDone instanceof SelectListenerOperation && toExecute instanceof SelectListenerOperation
                        && ((SelectListenerOperation) lastDone).getListener() == ((SelectListenerOperation) toExecute).getListener()) {
                    // do not save operation as done, as this would be double (can occur when redoing operations that lead directly to selecting a new listener
                    addToDone = false;
                }
            }
            if (addToDone) {
                mDoneOperations.add(toExecute);
            }
            onDoneOperationsChange();
        }
        if (toExecute instanceof RemoveOperation) {
            toExecute.execute(Double.NaN);
        } else if (toExecute instanceof NewOperation) {
            if (((NewOperation) toExecute).getOperationToAdd() == null && addToDone && mDoneOperations.size() > 0) {
                mDoneOperations.remove(mDoneOperations.size() - 1); // not sure that we really created an operation
                onDoneOperationsChange();
            }
            toExecute.execute(Double.NaN);
        } else {
            List<OperationListener> activeListeners = new ArrayList<OperationListener>(mListener.size());
            for (OperationListener listener : mListener) {
                if (listener.isActive()) {
                    activeListeners.add(listener);
                }
            }
            for (OperationListener listener : activeListeners) {
                double result = toExecute.execute(listener.getNumber());
                listener.operationExecuted(result, toExecute);
            }
        }

        if (clearRedoables) {
            mRedo.setEnabled(false);
            mUndoneOperations.clear();
        }
    }
    
    private String getPrefKey() {
        return OPERATIONS_DATA_PREFIX + mGameKey + mStorageKey;
    }
    
    private void loadOperations() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        String data = sharedPref.getString(getPrefKey(),null);
        mOps = new LinkedList<Operation>();
        if (data != null) {
            Compacter cmp = new Compacter(data);
            Operation operation = null;
            for (String op : cmp) {
                // first letter determines operation type
                char firstLetter = op.charAt(0);
                String lastLetters = op.substring(1);
                try {
                    switch (firstLetter) {
                    case '+':
                        operation = new PlusOperation(Double.parseDouble(lastLetters)); break;
                    case '-':
                        operation = new MinusOperation(Double.parseDouble(lastLetters)); break;
                    case '*':
                        operation = new MultiOperation(Double.parseDouble(lastLetters)); break;
                    case '/':
                        operation = new DivideOperation(Double.parseDouble(lastLetters)); break;
                    case 'a':
                        operation = new AbsoluteOperation(Double.parseDouble(lastLetters)); break;
                    case 'n':
                        operation = new NewOperation(this, null, -1); break;
                    case 's':
                        operation = new SignOperation(); break;
                    case 'c':
                        operation = new ClearOperation(); break;
                    default:
                        operation = null;
                    }
                    
                } catch (NumberFormatException nfe) {
                    operation = null;
                }
                if (operation != null) {
                    mOps.add(operation);
                }
            }
        }
        mAdapter = new OperationAdapter(mContext, mGameKey, mOps);
        mGrid.setAdapter(mAdapter);
    }
    
    private void saveOperations() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Compacter cmp = new Compacter();
        if (mOps != null && mOps.size() > 0) {
            for (Operation op : mOps) {
                // first letter determines operation type
                char firstLetter = '\0';
                String lastLetters = "";
                if (op instanceof AlgebraicOperation) {
                    if (op instanceof PlusOperation) {
                        firstLetter = '+';
                    } else if (op instanceof MinusOperation) {
                        firstLetter = '-';
                    } else if (op instanceof MultiOperation) {
                        firstLetter = '*';
                    } else if (op instanceof DivideOperation) {
                        firstLetter = '/';
                    }
                    lastLetters = Double.toString(((AlgebraicOperation) op).getOperand());
                } else if (op instanceof AbsoluteOperation) {
                    firstLetter = 'a';
                    lastLetters = Double.toString(((AbsoluteOperation) op).getValue());
                } else if (op instanceof NewOperation) {
                    firstLetter = 'n';
                    lastLetters = "";
                } else if (op instanceof SignOperation) {
                    firstLetter = 's';
                    lastLetters = "";
                } else if (op instanceof ClearOperation) {
                    firstLetter = 'c';
                    lastLetters = "";
                } else {
                    firstLetter = '\0';
                    lastLetters = "";
                }
                if (firstLetter != '\0') {
                    cmp.appendData(firstLetter + lastLetters);
                }
            }
        }
        editor.putString(getPrefKey(), cmp.compact());
        ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
    }

    @Override
    public boolean removeOperation(Operation toRemove) {
        if (mOps.remove(toRemove)) {
            saveOperations();
            mAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    private void undoableRemoveOperation(Operation toRemove) {
        executeOperationOnActive(new RemoveOperation(this, toRemove, mOps.indexOf(toRemove)), true, true);
    }
    
    @Override
    public void newOperation(Operation toAdd, int position) {
        if (toAdd == null) {
            showNewOperationDialog(position);
        } else if (position >= 0 && position < mOps.size()){
            mOps.add(position, toAdd);
        } else if (mOps.size() > 0 && mOps.get(mOps.size() - 1) instanceof NewOperation) {
            mOps.add(mOps.size() - 1, toAdd); // if the last operation is a NewOperation, we add it in front of this one
        } else {
            mOps.add(toAdd);
        }
        saveOperations();
        mAdapter.notifyDataSetChanged();
    }

    private void showNewOperationDialog(int operationPosition) {
        View baseView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.new_operation_dialog, null);
        final RadioGroup group = (RadioGroup) baseView.findViewById(R.id.op_type);
        final EditText number = (EditText) baseView.findViewById(R.id.op_number);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(R.drawable.ic_menu_add)
        .setTitle(mContext.getResources().getString(R.string.number_input_new_operation_heading))
                .setView(baseView)
               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       onConfirmation(number, group);
                   }
               })
               .setNegativeButton(android.R.string.no, null);
        final AlertDialog dialog = builder.create();
        group.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.op_clear || checkedId == R.id.op_sign) {
                    number.setVisibility(View.GONE);
                } else {
                    number.setVisibility(View.VISIBLE);
                }
            }
        });
        number.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    onConfirmation(number, group);
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }
    
    private void onConfirmation(final EditText number, final RadioGroup group) {
        double num = Double.NaN;
        if (number.getText().length() > 0) {
            try {
                num = Double.parseDouble(number.getText().toString());
            } catch(NumberFormatException nfe) {
            }
        }
        Operation op = null;
        int checkedId = group.getCheckedRadioButtonId();
        switch (checkedId) {
        case R.id.op_absolute:
            op = new AbsoluteOperation(num); break;
        case R.id.op_clear:
            op = new ClearOperation(); break;
        case R.id.op_sign:
            op = new SignOperation(); break;                           
        }
        if (checkedId == R.id.op_plus || checkedId == R.id.op_minus || checkedId == R.id.op_multi || checkedId == R.id.op_divide) {
            if (!Double.isNaN(num) && !Double.isInfinite(num)) {
             switch (checkedId) {
             case R.id.op_plus:
                 op = new PlusOperation(num); break;
             case R.id.op_minus:
                 op = new MinusOperation(num); break;
             case R.id.op_multi:
                 if (!AlgebraicOperation.isZero(num)) {
                     op = new MultiOperation(num);
                 } else {
                     op = new AbsoluteOperation(0.0);
                 }
                 break;
             case R.id.op_divide:
                 if (!AlgebraicOperation.isZero(num)) {
                     op = new DivideOperation(num);
                 }
                 break;
             default:
                 break;
             }
            }
        }
        if (op != null) {
            executeOperationOnActive(new NewOperation(NumberInputController.this, op, -1), false, true);
        } else {
            Toast.makeText(mContext, mContext.getResources().getString(R.string.new_illegal_operation), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public OperationListener getActiveListener() {
        for (OperationListener list : mListener) {
            if (list.isActive()) {
                return list;
            }
        }
        return null;
     }

    public int getOperationsCount() {
        return mOps.size();
    }

    public void removeAllListener() {
        mListener.clear();
        mOnClickListener.clear();
    }
    
    public void addListener(OperationListener l) {
        mListener.add(l);
    }
    
    public void addListener(OnOperationClickedListener l) {
        mOnClickListener.add(l);
    }

    public void setListenerActive(int listenerPos) {
        if (!mListener.get(listenerPos).isActive()) {
            executeOperationOnActive(new SelectListenerOperation(this, mListener.get(listenerPos)), true, true);
        }
    }
    
    public void clearHistory() {
        mDoneOperations.clear();
        mUndoneOperations.clear();
        mRedo.setEnabled(false);
        onDoneOperationsChange();
    }

    public void clearOperations() {
        mOps.clear();
        saveOperations();
    }
}
