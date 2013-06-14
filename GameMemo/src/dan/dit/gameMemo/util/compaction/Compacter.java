package dan.dit.gameMemo.util.compaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * A Compacter is a very simple object that allows 'serialization' of
 * primitive data types by compacting data into a single string as CSV (see http://en.wikipedia.org/wiki/Comma-separated_values).
 * This data
 * that is appended to the Compacter can later be read by a new Compacter initialized
 * with the compacted string in the same order. Strings must not be null, but can contain any data! So there is no special character excluded.
 * <br> Nesting of
 * Compacters is easily possible, so strings compacted by a Compacter can be appended and later
 * read by another Compacter.<br><br>
 * Example:<br>
 * <code>Compacter cmp = new Compacter().appendData("a").appendData("b").appendData(2);<br>
 * String compacted = cmp.compact();<br>
 * Compacter decompacter = new Compacter(compacted);<br>
 * // decompacter.getData(0) equals "a", getData(1) equals "b" and getData(2) equals String.valueOf(2) </code>
 * @author Daniel
 *
 */
public class Compacter implements Iterable<String> {
	private static final String SEPARATION_SYMBOL = ";"; // any length one string which is no blank symbol that gets trimmed
	private static final String BLANK = " "; // any string of length >= 1 unequal to and does not contain SEPARATION_SYMBOL
	public static final String SEPARATOR = BLANK + SEPARATION_SYMBOL + BLANK;
	private static final char[] SEPERATOR_CHARS = SEPARATOR.toCharArray();
	private List<String> data;
	
	/**
	 * Creates a new (De)Compacter which decompacts the given compacted data.
	 * @param compactedData Compacted data that can then be read. 
	 * @throws NullPointerException If given data string is <code>null</code>.
	 */
	public Compacter(String compactedData) {
		data = new ArrayList<String>();
		// search for SEPERATORS and extract data in between
		int startIndex = 0;
		final int dataLength = compactedData.length();
		final int seperatorLength = SEPERATOR_CHARS.length;
		for (int curIndex = 0; curIndex < dataLength - seperatorLength + 1; ) {
			boolean foundSeparator = true;
			for (int s = 0; s < seperatorLength; s++) {
				if (SEPERATOR_CHARS[s] != compactedData.charAt(curIndex + s)) {
					foundSeparator = false;
					break;
				}
			}
			if (foundSeparator) {
				data.add(compactedData.substring(startIndex, curIndex).replace(SEPARATION_SYMBOL + SEPARATION_SYMBOL, SEPARATION_SYMBOL));
				curIndex += seperatorLength;
				startIndex = curIndex;
			} else {
				curIndex++;
			}
		}
		if (startIndex < dataLength) {
			data.add(compactedData.substring(startIndex).replace(SEPARATION_SYMBOL + SEPARATION_SYMBOL, SEPARATION_SYMBOL));
		}
	}
	
	/**
	 * Creates a new empty Compacter with default capacity.
	 */
	public Compacter() {
		data = new ArrayList<String>();
	}
	
	/**
	 * Creates a new empty Compacter with the given capacity.
	 * @param capacity The capacity for the Compacter, must be positive.
	 */
	public Compacter(int capacity) {
		data = new ArrayList<String>(capacity);
	}

	/**
	 * Appends data to the Compacter.
	 * @param dataString The data string to append.
	 * @throws NullPointerException If dataString is <code>null</code>.
	 * @return this
	 */
	public Compacter appendData(String dataString) {
		if (dataString == null) {
			throw new NullPointerException("Given String is null.");
		}
		data.add(dataString);
		return this;
	}
	
	/**
	 * Appends the given int. Equal to appendData(String.valueOf(dataInt)).
	 * @param dataInt The int to append.
	 * @return this
	 */
	public Compacter appendData(int dataInt) {
		data.add(String.valueOf(dataInt));
		return this;
	}
	
	/**
	 * Appends the given char. Equal to appendData(String.valueOf(dataChar)).
	 * @param dataChar The char to append.
	 * @return this
	 */
	public Compacter appendData(char dataChar) {
		data.add(String.valueOf(dataChar));
		return this;
	}
	
	/**
	 * Appends the given long. Equal to appendData(String.valueOf(dataLong)).
	 * @param dataLong The long to append.
	 * @return this
	 */
	public Compacter appendData(long dataLong) {
		data.add(String.valueOf(dataLong));
		return this;
	}
	
	/**
	 * Returns the data at the given index. The data order is kept consistent:
	 * The first data appended is at index 0, the second at index 1,...
	 * @param index The index of the desired data. Must be greater than or equal zero
	 * and lower than getSize().
	 * @return The data stored at the given index.
	 */
	public String getData(int index) {
		return data.get(index);
	}
	
	/**
	 * Returns the data at the given index, converting it to an integer.
	 * @param index The index of the desired data. Must be greater than or equal zero and lower than getSize().
	 * @return The data stored ath the given index as an integer.
	 * @throws CompactedDataCorruptException When a NumberFormatException occurs.
	 */
	public int getInt(int index) throws CompactedDataCorruptException {
		String dataString = data.get(index);
		int dataInt;
		try {
			dataInt = Integer.parseInt(dataString);
		} catch (NumberFormatException nfe) {
			throw new CompactedDataCorruptException("Could not parse int from: " + dataString).setCorruptData(this);
		}
		return dataInt;
	}
	
	/**
	 * Returns the data at the given index, converting it to a long.
	 * @param index The index of the desired data. Must be greater than or equal zero and lower than getSize().
	 * @return The data stored ath the given index as a long.
	 * @throws CompactedDataCorruptException When a NumberFormatException occurs.
	 */
	public long getLong(int index) throws CompactedDataCorruptException {
		String dataString = data.get(index);
		long dataLong;
		try {
			dataLong = Long.parseLong(dataString);
		} catch (NumberFormatException nfe) {
			throw new CompactedDataCorruptException("Could not parse long from: " + dataString).setCorruptData(this);
		}
		return dataLong;
	}
	
	@Override
	public Iterator<String> iterator() {
		return data.iterator();
	}
	
	/**
	 * Returns the size of this Compacter, which is the amount
	 * of data packages stored with it.
	 * @return The size of this Compacter.
	 */
	public int getSize() {
		return data.size();
	}

	public String compact() { // compacting is always possible
		if (data.size() == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < data.size(); i++) {
			String s = data.get(i);			
			builder.append(s.replace(SEPARATION_SYMBOL, SEPARATION_SYMBOL + SEPARATION_SYMBOL));
			if (i < data.size() - 1 || s.length() == 0) {
				builder.append(SEPARATOR);
			}
		}
		return builder.toString();
	}	
	
	@Override
	public String toString() {
		return data.toString();
	}
	
	@Override
	public int hashCode() {
		return data.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Compacter) {
			return data.equals(((Compacter) other).data);
		} else {
			return super.equals(other);
		}
	}
}
