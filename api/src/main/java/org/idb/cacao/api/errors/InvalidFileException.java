package org.idb.cacao.api.errors;

public class InvalidFileException extends RuntimeException {
    private static final long serialVersionUID = 27688450636526L;

    public InvalidFileException() {
        super();
    }

    public InvalidFileException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidFileException(final String message) {
        super(message);
    }

    public InvalidFileException(final Throwable cause) {
        super(cause);
    }
}

