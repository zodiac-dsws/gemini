package br.com.cmabreu.zodiac.gemini.exceptions;

public class UpdateException extends PersistenceException {
	private static final long serialVersionUID = 1L;

	public UpdateException( String message ) {
		super(message);
	}
	
}
