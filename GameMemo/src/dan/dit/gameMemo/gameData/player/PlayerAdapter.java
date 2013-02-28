package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;
import android.widget.Filter;


public class PlayerAdapter extends ArrayAdapter<Player> {
	private Set<Player> allPlayer = new HashSet<Player>();
	
	public PlayerAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}
	
	@Override
	public Filter getFilter() {
		return nameFilter;
	}
	
	@Override
	public void add(Player toAdd) {
		super.add(toAdd);
		allPlayer.add(toAdd);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void addAll(Collection<? extends Player> collection) {
		super.addAll(collection);
		allPlayer.addAll(collection);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void addAll(Player...items) {
		super.addAll(items);
		for (Player p : items) {
			allPlayer.add(p);
		}
	}
	
	@Override
	public void remove(Player p) {
		super.remove(p);
		allPlayer.remove(p);
	}
	
	@Override
	public void insert(Player obj, int index) {
		super.insert(obj, index);
		allPlayer.add(obj);
	}

	private Filter nameFilter = new Filter() {
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			if (constraint != null) {
				String trimmedConstraint = constraint.toString().trim().toLowerCase(Locale.getDefault());
				if (trimmedConstraint.length() > 0) {
					ArrayList<Player> suggestions = new ArrayList<Player>(5); // there will never be many suggested players
					final int MIN_LENGTH_TO_FILTER_IN_WORD = 3;
					for (Player curr : allPlayer) {
						if (curr != null) {
							String currName = curr.getName().toLowerCase(Locale.getDefault());
							if (trimmedConstraint.length() >= MIN_LENGTH_TO_FILTER_IN_WORD 
									&& currName.contains(trimmedConstraint)) {
								suggestions.add(curr);
							} else if (trimmedConstraint.length() < MIN_LENGTH_TO_FILTER_IN_WORD 
									&& currName.startsWith(trimmedConstraint)) {
								suggestions.add(curr);
							}
						}
					}
					FilterResults res = new FilterResults();
					res.values = suggestions;
					res.count = suggestions.size();
					return res;
				}
			}
			return new FilterResults();
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			if (results != null && results.count > 0) {
				@SuppressWarnings("unchecked")
				ArrayList<Player> suggestions = (ArrayList<Player>) results.values;
				clear();
				for (Player p : suggestions) {
					add(p);
				}
				notifyDataSetChanged();
			}
		}
	};
}
