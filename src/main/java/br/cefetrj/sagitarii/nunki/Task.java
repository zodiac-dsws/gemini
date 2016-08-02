package br.cefetrj.sagitarii.nunki;

/**
 * Copyright 2015 Carlos Magno Abreu
 * magno.mabreu@gmail.com 
 *
 * Licensed under the Apache  License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required  by  applicable law or agreed to in  writing,  software
 * distributed   under the  License is  distributed  on  an  "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the  specific language  governing  permissions  and
 * limitations under the License.
 * 
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Task {
	private List<String> sourceData;
	private List<String> console;
	private List<String> execLog;
	private TaskStatus status;
	private int exitCode;
	private Activation activation;
	private Date realStartTime;
	private Date realFinishTime;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private int PID;
	private TaskManager tm;

	public int getPID() {
		return PID;
	}

	public Date getRealFinishTime() {
		return realFinishTime;
	}

	public Date getRealStartTime() {
		return realStartTime;
	}

	public void setRealFinishTime(Date realFinishTime) {
		this.realFinishTime = realFinishTime;
	}

	public void setRealStartTime(Date realStartTime) {
		this.realStartTime = realStartTime;
	}

	private void error( String s ) {
		execLog.add( s );
		logger.error( s );
		tm.notifySagitarii("[ERROR] " + s);
	}

	private void debug( String s ) {
		execLog.add( s );
		logger.debug( s );
		tm.notifySagitarii(s);
	}

	public Activation getActivation() {
		return activation;
	}

	public List<String> getSourceData() {
		return sourceData;
	}

	public List<String> getConsole() {
		return console;
	}

	public List<String> getExecLog() {
		return execLog;
	}

	public void setSourceData(List<String> sourceData) {
		this.sourceData = sourceData;
	}

	public TaskStatus getTaskStatus() {
		return this.status;
	}

	public String getApplicationName() {
		return activation.getCommand();
	}

	public String getTaskId() {
		return this.activation.getTaskId();
	}	

	public Task( Activation activation, List<String> execLog, TaskManager tm ) {
		this.activation = activation;
		this.status = TaskStatus.STOPPED;
		this.activation = activation;
		this.console = new ArrayList<String>();
		this.execLog = execLog;
		this.tm = tm;
	}

	public Connection connect(String url, String user, String port, String password, String database) {
	      debug("Try to connect at " + user+"@"+url+":"+port+"/"+database);	
	      Connection connection = null;
	      try {
	         Class.forName("org.postgresql.Driver");
	         connection= DriverManager.getConnection("jdbc:postgresql://"+url+":"+port+"/"+database, user, password);
	      } catch (Exception e) {
	         error(e.getClass().getName()+": "+e.getMessage());
	      }
	      debug("Database Connected.");	
	      return connection;
	}
	
	public boolean executeSql( Connection connection, String sql ) {
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
			connection.close();
			return true;
		} catch ( Exception e ) {
			error( e.getClass().getName()+": "+ e.getMessage() );
			return false;
		}
	}
	
	
	public void run( Configurator conf ) {
		debug("running external wrapper ");
		
		Connection connection = connect( conf.getDbUrl(), conf.getDbUser(),
				conf.getDbPort(), conf.getDbPassword(), conf.getDbDatabase() );
		if( connection != null ) {
			debug( activation.getCommand() );
			if( executeSql( connection, activation.getCommand() ) ) {
				debug("Statement executed.");
			} else {
				error("Cannot run statement.");
			}
			
			
		} else {
			error("No connection to database. Nothing to do.");
		}
		
		debug("external wrapper finished.");
	}

	public int getExitCode() {
		return this.exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

}