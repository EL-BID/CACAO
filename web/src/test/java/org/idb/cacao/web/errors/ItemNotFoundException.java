package org.idb.cacao.web.errors;

public class ItemNotFoundException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public ItemNotFoundException(String message) {
        super(message);
    }

    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemNotFoundException(Throwable cause) {
        super(cause);
    }
}
