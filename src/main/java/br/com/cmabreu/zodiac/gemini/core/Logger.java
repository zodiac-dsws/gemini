package br.com.cmabreu.zodiac.gemini.core;

import java.util.Calendar;

public class Logger {
	private static Logger instance;
	private boolean enabled;
	private boolean toFile = true;
	private String fileName;
	
	public void enable() {
		enabled = true;
	}
	
	public void canOutputToFile( boolean toFile ) {
		this.toFile = toFile;
	}
	
	public void disable() {
		enabled = false;
	}
	
	public static Logger getInstance() {
		if ( instance == null ) {
			instance = new Logger();
		}
		return instance;
	}
	
	private String getClassName( String className ) {
		String[] temp = className.split("\\.");
		int pos = temp.length -1 ;
		return temp[ pos ] ;
	}
	
	private void print( String s ) {
		System.out.println( s );
		
	}
	
	public void debug( String className, String message ) {
		if ( !enabled ) { return; }
		String s = "[DEBUG] " + getClassName(className) + " " + message;
		print( s );
	}
	
	public void error( String className, String message ) {
		if ( !enabled ) { return; }
		String s = "[ERROR] " + getClassName(className) + " " + message;
		print( s );
	}
	
	public void warn( String className, String message ) {
		if ( !enabled ) { return; }
		String s = "[WARN] " + getClassName(className) + " " + message;
		print( s );
	}

	

}
