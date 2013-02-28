package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;


public class PlayerAdapter extends ArrayAdapter<Player> {
	private Collection<Player> allPlayers = CombinedPool.ALL_POOLS.getAll();
	public PlayerAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}
	
	@Override
	public Filter getFilter() {
		return nameFilter;
	}

	private Filter nameFilter = new Filter() {
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			if (constraint != null) {
				String trimmedConstraint = constraint.toString().trim().toLowerCase(Locale.getDefault());
				if (trimmedConstraint.length() > 0) {
					ArrayList<Player> suggestions = new ArrayList<Player>(5); // there will never be many suggested players
					final int MIN_LENGTH_TO_FILTER_IN_WORD = 3;
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
