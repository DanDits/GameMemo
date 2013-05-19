package dan.dit.gameMemo.util.compaction;

public class CompactedDataCorruptException extends Exception {
	private static final long serialVersionUID = -5192264960343340894L;
	private String corruptData;
	
	public CompactedDataCorruptException() {
		super();
	}
	
	public CompactedDataCorruptException(String detailMessage) {
		super(detailMessage);
	}
	
	public CompactedDataCorruptException(Throwable cause) {
		super (cause);
	}

	public CompactedDataCorruptException(String message,
			Throwable cause) {
		super(message, cause);
	}

	public CompactedDataCorruptException setCorruptData(String corruptData) {
		this.corruptData = corruptData;
		return this;
	}
	
	public String getCorruptData() {
		return corruptData == null ? "" : corruptData;
	}

	public CompactedDataCorruptException setCorruptData(Compacter dataCompressor) {
		return setCorruptData(dataCompressor.compact());
	}
	
	
}
