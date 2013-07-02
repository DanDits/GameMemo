package dan.dit.gameMemo.gameData.player;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import dan.dit.gameMemo.R;


public class PlayerAdapter extends ArrayAdapter<Player> {
	private static final int MIN_LENGTH_TO_FILTER_IN_WORD = 2;
	private Set<Player> allPlayers;
	
	public PlayerAdapter(boolean big, Context context, Set<Player> all) {
		super(context, big ? R.layout.dropdown_item_big : android.R.layout.simple_dropdown_item_1line, 
				android.R.id.text1);
		allPlayers = all;
	}
	
	@Override
	public Filter getFilter() {
		return nameFilter;
	}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView text = (TextView)view.findViewById(android.R.id.text1);
        if (text != null) {
            text.setTextColor(Color.BLACK);
        }
        return view;

    }
	
	@Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = getView(position, convertView, parent);
        return view;

    }
	  
	private Filter nameFilter = new Filter() {
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			if (constraint != null) {
				String trimmedConstraint = constraint.toString().trim().toLowerCase(Locale.getDefault());
				if (trimmedConstraint.length() > 0) {
					Set<Player> suggestions = new HashSet<Player>(); // there will never be many suggested players
					for (Player curr : allPlayers) {
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
				Set<Player> suggestions = (Set<Player>) results.values;
				clear();
				for (Player p : suggestions) {
					add(p);
				}
				notifyDataSetChanged();
			}
		}
	};
	public void addFilterPlayers(List<Player> toFilter) {
		if (toFilter != null) {
			for (Player p : toFilter) {
				if (p != null) {
					remove(p);
					allPlayers.remove(p);
				}
			}
		}
	}
}
