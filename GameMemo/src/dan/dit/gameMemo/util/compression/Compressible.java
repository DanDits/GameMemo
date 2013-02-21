package dan.dit.gameMemo.util.compression;

/**
 * An interface for an arbitrary object that is lightweight enough
 * to simply compress it to a string and later load it from a string.
 * Implementing classes should also provide a constructor which takes a single
 * string value (the compressed data) to rebuilt an object or better: take a single Compressor 
 * object.
 * @author Daniel
 *
 */
public interface Compressible {
	/**
	 * Compresses all important data ('important' as defined by the compressible object)
	 * to a single String. The object should be able to rebuilt itself (completely) from this string.
	 * @return A string that contains the data of this object.
	 */
	String compress();
}
