package br.com.cmabreu.zodiac.gemini.exceptions;

public class DeleteException extends PersistenceException {
	private static final long serialVersionUID = 1L;
	
	public DeleteException( String message ) {
		super(message);
	}	

}
