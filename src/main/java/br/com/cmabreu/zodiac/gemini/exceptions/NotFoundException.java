package br.com.cmabreu.zodiac.gemini.exceptions;

public class NotFoundException extends PersistenceException {
	private static final long serialVersionUID = 1L;

	public NotFoundException(String message){
		super(message);
	}
	
}
