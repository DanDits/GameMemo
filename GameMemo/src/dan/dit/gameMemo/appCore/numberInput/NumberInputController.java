package dan.dit.gameMemo.appCore.numberInput;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.util.ActivityUtil;
import dan.dit.gameMemo.util.compaction.Compacter;

public class NumberInputController implements OperationCallback {
    private static final String PREF_FILE = "dan.dit.gameMemo.OPERATIONS_FOR_GAMES";
    private static final String OPERATIONS_DATA_PREFIX = "ops_";
    private ViewGroup mContainer;
    private View mRoot;
    private View mRedo;
    private View mUndo;
    private Context mContext;
    private GridView mGrid;
    private int mGameKey;
    private List<Operation> mOps;
    private List<OperationListener> mListener;
    private OperationAdapter mAdapter;
    
    private Stack<Operation> mDoneOperations;
    private List<Operation> mUndoneOperations;
    
    public NumberInputController(int gameKey, Context context, ViewGroup container, boolean operationsRemovable) {
        mContainer = container;
        mContext = context;
        mGameKey = gameKey;
        mListener = new LinkedList<OperationListener>();
        mRoot = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.number_input, mContainer);
        mGrid = (GridView) mRoot.findViewById(R.id.numberGrid);
        mDoneOperations = new Stack<Operation>();
        mUndoneOperations = new LinkedList<Operation>();
        mGrid.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                    long id) {
                executeOperationOnActive(mAdapter.getItem(position), true, true);
            }
        });
        if (operationsRemovable) {
            mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
    
                @Override
                public boolean onItemLongClick(AdapterView<?> adapter, View view,
                        int position, long id) {
                    undoableRemoveOperation(mAdapter.getItem(position));
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
    
    private void undoLastOperation() {
        if (!mDoneOperations.isEmpty()) {
            Operation done = mDoneOperations.pop();
            mUndoneOperations.add(0, done);
            executeOperationOnActive(done.getInverseOperation(), false, false);
            mRedo.setEnabled(true);
            mUndo.setEnabled(!mDoneOperations.isEmpty());
            Log.d("Minigolf", "Undoing: " + done.getName() + " by doing " + done.getInverseOperation().getName());
        } else {
            Toast.makeText(mContext, "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void redoLastOperation() {
        if (!mUndoneOperations.isEmpty()) {
            Operation toRedo = mUndoneOperations.remove(0);
            executeOperationOnActive(toRedo, false, true);
            mRedo.setEnabled(!mUndoneOperations.isEmpty());
            Log.d("Minigolf", "Redoing: " + toRedo.getName());
        }else {
            Toast.makeText(mContext, "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void executeOperationOnActive(Operation toExecute, boolean clearRedoables, boolean addToDone) {
        if (addToDone) {
            Operation inverse = toExecute.getInverseOperation();
            if (inverse == null) {
                if (toExecute instanceof AbsoluteOperation || toExecute instanceof ClearOperation) {
                    inverse = new AbsoluteOperation(getActiveListener().getNumber());
                    toExecute.setInverse(inverse);
                    inverse.setInverse(toExecute);
                }
                //TODO missing other special oeprations like NewOperation/RemoveOperation
            }
            boolean flagDoAdd = true;
            if (mDoneOperations.size() > 0 ) {
                Operation lastDone = mDoneOperations.get(mDoneOperations.size() - 1);
                if (lastDone instanceof SelectListenerOperation && toExecute instanceof SelectListenerOperation
                        && ((SelectListenerOperation) lastDone).getListener() == ((SelectListenerOperation) toExecute).getListener()) {
                    // do not save operation as done, as this would be double (can occur when redoing operations that lead directly to selecting a new listener
                    flagDoAdd = false;
                }
            }
            if (flagDoAdd) {
                mDoneOperations.add(toExecute);
            }
            mUndo.setEnabled(true);
            Log.d("Minigolf",   "  doing: " + toExecute.getName());
        }
        if (toExecute instanceof RemoveOperation) {
            toExecute.execute(Double.NaN);
        } else if (toExecute instanceof NewOperation) {
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
        return OPERATIONS_DATA_PREFIX + mGameKey;
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
                        operation = new NewOperation(this, null); break;
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
        mAdapter = new OperationAdapter(mContext, mOps);
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
        executeOperationOnActive(new RemoveOperation(this, toRemove), true, true);
    }
    
    @Override
    public void newOperation(Operation toAdd) {
        if (toAdd == null) {
            // TODO create new operation dialogs
        } else {
            mOps.add(toAdd);
        }
        saveOperations();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public Operation getLastCreatedOperation() {
        if (mOps.isEmpty()) {
            return null;
        } else {
            return mOps.get(mOps.size() - 1);
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
    }
    
    public void addListener(OperationListener l) {
        mListener.add(l);
    }

    public void setListenerActive(int listenerPos) {
        if (!mListener.get(listenerPos).isActive()) {
            executeOperationOnActive(new SelectListenerOperation(this, mListener.get(listenerPos)), false, true);
        }
    }
    
    public void clearHistory() {
        mDoneOperations.clear();
        mUndoneOperations.clear();
        mRedo.setEnabled(false);
        mUndo.setEnabled(false);
    }

    public void clearOperations() {
        mOps.clear();
        saveOperations();
    }
}
