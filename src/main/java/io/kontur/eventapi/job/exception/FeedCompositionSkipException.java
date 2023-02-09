package io.kontur.eventapi.job.exception;

public class FeedCompositionSkipException extends RuntimeException {

	public FeedCompositionSkipException() {
		super();
	}

	public FeedCompositionSkipException(String message) {
		super(message);
	}

	public FeedCompositionSkipException(String message, Throwable cause) {
		super(message, cause);
	}

	public FeedCompositionSkipException(Throwable cause) {
		super(cause);
	}
}
