package dan.dit.gameMemo.util.compression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * A Compressor is a very simple object that allows 'serialization' of
 * primitive data types by compressing data into a single string as CSV (see http://en.wikipedia.org/wiki/Comma-separated_values).
 * This data
 * that is appended to the compressor can later be read by a new compressor initialized
 * with the compressed string in the same order. Strings must not be null, but can contain any data! So there is no special character excluded.
 * <br> Nesting of
 * compressors is easily possible, so strings compressed by a compressor can be appended and later
 * read by another compressor.<br><br>
 * Example:<br>
 * <code>Compressor cmp = new Compressor().appendData("a").appendData("b").appendData(2);<br>
 * String compressed = cmp.compress();<br>
 * Compressor decompressor = new Compressor(compressed);<br>
 * // decompressor.getData(0) equals "a", getData(1) equals "b" and getData(2) equals String.valueOf(2) </code>
 * @author Daniel
 *
 */
public class Compressor implements Iterable<String>, Compressible {
	private static final String SEPARATION_SYMBOL = ";"; // any length one string which is no blank symbol that gets trimmed
	private static final String BLANK = " "; // any string of length >= 1 unequal to and does not contain SEPARATION_SYMBOL
	private static final String SEPARATOR = BLANK + SEPARATION_SYMBOL + BLANK;
	private static final char[] SEPERATOR_CHARS = SEPARATOR.toCharArray();
	private List<String> data;
	
	/**
	 * Creates a new (De)Compressor which decompresses the given compressed data.
	 * @param compressedData Compressed data that can then be read. 
	 * @throws NullPointerException If given data string is <code>null</code>.
	 */
	public Compressor(String compressedData) {
		data = new ArrayList<String>();
		// search for SEPERATORS and extract data in between
		int startIndex = 0;
		final int dataLength = compressedData.length();
		final int seperatorLength = SEPERATOR_CHARS.length;
		for (int curIndex = 0; curIndex < dataLength - seperatorLength + 1; ) {
			boolean foundSeparator = true;
			for (int s = 0; s < seperatorLength; s++) {
				if (SEPERATOR_CHARS[s] != compressedData.charAt(curIndex + s)) {
					foundSeparator = false;
					break;
				}
			}
			if (foundSeparator) {
				data.add(compressedData.substring(startIndex, curIndex).replace(SEPARATION_SYMBOL + SEPARATION_SYMBOL, SEPARATION_SYMBOL));
				curIndex += seperatorLength;
				startIndex = curIndex;
			} else {
				curIndex++;
			}
		}
		if (startIndex < dataLength) {
			data.add(compressedData.substring(startIndex).replace(SEPARATION_SYMBOL + SEPARATION_SYMBOL, SEPARATION_SYMBOL));
		}
	}
	
	/**
	 * Creates a new empty compressor with default capacity.
	 */
	public Compressor() {
		data = new ArrayList<String>();
	}
	
	/**
	 * Creates a new empty compressor with the given capacity.
	 * @param capacity The capacity for the Compressor, must be positive.
	 */
	public Compressor(int capacity) {
		data = new ArrayList<String>(capacity);
	}

	/**
	 * Appends data to the compressor.
	 * @param dataString The data string to append.
	 * @throws NullPointerException If dataString is <code>null</code>.
	 * @return this
	 */
	public Compressor appendData(String dataString) {
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
	public Compressor appendData(int dataInt) {
		data.add(String.valueOf(dataInt));
		return this;
	}
	
	/**
	 * Appends the given char. Equal to appendData(String.valueOf(dataChar)).
	 * @param dataChar The char to append.
	 * @return this
	 */
	public Compressor appendData(char dataChar) {
		data.add(String.valueOf(dataChar));
		return this;
	}
	
	/**
	 * Appends the given long. Equal to appendData(String.valueOf(dataLong)).
	 * @param dataLong The long to append.
	 * @return this
	 */
	public Compressor appendData(long dataLong) {
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
	
	@Override
	public Iterator<String> iterator() {
		return data.iterator();
	}
	
	/**
	 * Returns the size of this compressor, which is the amount
	 * of data packages stored with it.
	 * @return The size of this compressor.
	 */
	public int getSize() {
		return data.size();
	}

	@Override
	public String compress() { // compressing is always possible
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
	
	
}
