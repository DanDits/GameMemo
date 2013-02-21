package dan.dit.gameMemo.util.compression;

public class CompressedDataCorruptException extends Exception {
	private static final long serialVersionUID = -5192264960343340894L;
	private String corruptData;
	
	public CompressedDataCorruptException() {
		super();
	}
	
	public CompressedDataCorruptException(String detailMessage) {
		super(detailMessage);
	}
	
	public CompressedDataCorruptException(Throwable cause) {
		super (cause);
	}

	public CompressedDataCorruptException(String message,
			Throwable cause) {
		super(message, cause);
	}

	public CompressedDataCorruptException setCorruptData(String corruptData) {
		this.corruptData = corruptData;
		return this;
	}
	
	public String getCorruptData() {
		return corruptData == null ? "" : corruptData;
	}

	public CompressedDataCorruptException setCorruptData(Compressor dataCompressor) {
		return setCorruptData(dataCompressor.compress());
	}
	
	
}
