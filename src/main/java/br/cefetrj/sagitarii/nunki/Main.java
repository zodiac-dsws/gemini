package br.cefetrj.sagitarii.nunki;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import br.cefetrj.sagitarii.nunki.comm.Communicator;


public class Main {
	private static Logger logger = LogManager.getLogger( "br.cefetrj.sagitarii.nunki.Main" ); 
	private static long totalInstancesProcessed = 0;
	private static boolean paused = false;
	private static List<TaskRunner> runners = new ArrayList<TaskRunner>();
	private static boolean restarting = false;
	private static boolean quiting = false;
	private static Communicator communicator;
	private static Configurator configurator;
	private static Watchdog watchdog;
	
	public static void pause() {
		paused = true;
	}
	
	public static long getTotalInstancesProcessed() {
		return totalInstancesProcessed;
	}
	
	public static Communicator getCommunicator() {
		return communicator;
	}
	
	public static Configurator getConfigurator() {
		return configurator;
	}

	public static void resume() {
		paused = false;
	}

	private static List<String> decodeResponse( String encodedResponse ) {
		logger.debug("decoding response ...");
		String[] responses = encodedResponse.replace("[", "").replace("]", "").replace(" ", "").split(",");
		List<String> resp = new ArrayList<String>();
		logger.debug("response package contains " + resp.size() + " instances" );
		resp = new ArrayList<String>( Arrays.asList( responses ) ); 
		logger.debug("done");
		return resp;
	}

	
	/**
	 * TaskManager entry point
	 * 
	 * EX UNITATE VIRES !

	 */
	public static void main( String[] args ) {
		
		try {
			System.out.println("");
	    	System.out.println("Sagitarii Nunki Node v1.0		      26/03/2016");
	    	System.out.println("Carlos Magno Abreu        magno.mabreu@gmail.com");
			System.out.println("------------------------------------------------");
			System.out.println("");
			
			
			configurator = new Configurator("config.xml");
			configurator.loadMainConfig();

			if ( configurator.useProxy() ) {
				logger.debug("Proxy: " + configurator.getProxyInfo().getHost() );
			}
			if ( !configurator.getShowConsole() ) {
				logger.debug("No activations console.");
			}

			logger.debug("Staring communicator...");
			communicator = new Communicator( configurator );
			watchdog = new Watchdog(communicator, configurator);
			
			
			logger.debug("Database: " + configurator.getDbUser() + "@" + configurator.getDbUrl() + 
					":" + configurator.getDbPort() + "/" + configurator.getDbDatabase() );
			
			while (true) {
				clearRunners();

				logger.debug( "init new cycle: " + runners.size() + " of " + configurator.getActivationsMaxLimit() + " tasks running:" );
				for ( TaskRunner tr : getRunners() ) {
					if ( tr.getCurrentTask() != null ) {
						String time = tr.getStartTime() + " (" + tr.getTime() + ")";
						logger.debug( " > " + tr.getCurrentTask().getTaskId() + " (" + tr.getCurrentActivation().getExecutor() + ") : " + time);
					}
				}
				
				SpeedEqualizer.equalize( configurator, runners.size() );
				
				if ( !paused ) {
					
					watchdog.protect( getRunners(), configurator.getSystemProperties().getCpuLoad() );
					
					String response = "NO_DATA";
					try {
						if ( runners.size() < configurator.getActivationsMaxLimit() ) {

							if ( !havePendentCommand() ) {
								logger.debug( "asking Sagitarii for tasks to process...");
								
								int packageSize = configurator.getActivationsMaxLimit() - runners.size();
								if ( packageSize < 1 ) {
									packageSize = 1;
								}
								
								response = getTasksFromSagitarii(packageSize);
								
								if ( response.length() > 0 ) {
									if ( response.equals("COMM_ERROR") ) {
										logger.error("Sagitarii is offline");
									} else {
										if ( !specialCommand( response ) ) {
											List<String> responses = decodeResponse( response );
											for ( String decodedResponse : responses ) {
												startTask( decodedResponse);
											}
										}
									}
								} else {
									logger.debug("nothing to do for now");
								}
								
							} else {
								logger.debug("cannot request new tasks: flushing buffers...");
							}
							
							// If this number is > 0 then atfer I started new tasks, 
							// some of old ones have finished, so I'm a bit slow 
							logger.debug("Lazy Rate: " + (configurator.getActivationsMaxLimit() - runners.size() ) );
							
						} else {
							//
						}

						DynamicLoadBalancer.equalize( configurator, runners.size() );

					} catch ( Exception e ) {
						logger.error( "process error: " + e.getMessage() );
						logger.error( " > " + response );
					}
				}
				sendRunners();
				
				logger.debug("will sleep " + configurator.getPoolIntervalMilliSeconds() + "ms");
				try {
				    Thread.sleep( configurator.getPoolIntervalMilliSeconds() );
				} catch( InterruptedException ex ) {
				
				}
						
			}
			
			
		} catch (Exception e) {
			logger.debug("Critical error. Cannot start TaskManager Node.");
			logger.debug("Error details:");
			e.printStackTrace();
		}

	}

	private static String getTasksFromSagitarii(int packageSize) {
		String response;
		response = communicator.announceAndRequestTask( configurator.getSystemProperties().getCpuLoad(), 
				configurator.getSystemProperties().getFreeMemory(), configurator.getSystemProperties().getTotalMemory(),
				packageSize, configurator.getSystemProperties().getMemoryPercent(), configurator.getActivationsMaxLimit() );
		return response;
	}
	
	private static void startTask( String decodedResponse ) {
		logger.debug("starting new task");
		TaskRunner tr = new TaskRunner( decodedResponse, communicator, configurator);
		runners.add(tr);
		tr.start();
		totalInstancesProcessed++;
		logger.debug("new task started");
	}

	private static String generateJsonPair(String paramName, String paramValue) {
		return "\"" + paramName + "\":\"" + paramValue + "\""; 
	}

	private static String addArray(String paramName, String arrayValue) {
		return "\"" + paramName + "\":" + arrayValue ; 
	}

	private static void sendRunners() {
		logger.debug("sending " + getRunners().size() + " Task Runners to Sagitarii ");
		StringBuilder sb = new StringBuilder();
		String dataPrefix = "";
		sb.append("[");
		for ( TaskRunner tr : getRunners() ) {
			if ( tr.getCurrentActivation() != null ) {
				logger.debug( " > " + tr.getCurrentActivation().getTaskId() + " sent" );
				sb.append( dataPrefix + "{");
				sb.append( generateJsonPair( "workflow" , tr.getCurrentActivation().getWorkflow() ) + "," );
				sb.append( generateJsonPair( "experiment" , tr.getCurrentActivation().getExperiment() ) + "," );
				sb.append( generateJsonPair( "taskId" , tr.getCurrentActivation().getTaskId() ) + "," );
				sb.append( generateJsonPair( "executor" , tr.getCurrentActivation().getExecutor() ) + "," ); 
				sb.append( generateJsonPair( "startTime" , tr.getStartTime() ) + "," );
				sb.append( generateJsonPair( "elapsedTime" , tr.getTime() ) );
				dataPrefix = ",";
				sb.append("}");
			} else {
				sb.append( dataPrefix + "{");
				sb.append( generateJsonPair( "workflow" , "UNKNOWN" ) + "," );
				sb.append( generateJsonPair( "experiment" , "UNKNOWN" ) + "," );
				sb.append( generateJsonPair( "taskId" , "UNKNOWN" ) + "," );
				sb.append( generateJsonPair( "executor" , "UNKNOWN" ) + "," ); 
				sb.append( generateJsonPair( "startTime" , "00:00:00" ) + "," );
				sb.append( generateJsonPair( "elapsedTime" , "00:00:00" ) );
				dataPrefix = ",";
				sb.append("}");
			}
		}
		sb.append("]");
		StringBuilder data = new StringBuilder();
		data.append("{");
		data.append( generateJsonPair( "nodeId" , configurator.getSystemProperties().getMacAddress() ) + "," );
		data.append( generateJsonPair( "cpuLoad" , String.valueOf( configurator.getSystemProperties().getCpuLoad() ) ) + "," );
		data.append( generateJsonPair( "freeMemory" , String.valueOf( configurator.getSystemProperties().getFreeMemory() ) ) + "," );
		data.append( generateJsonPair( "totalMemory" , String.valueOf( configurator.getSystemProperties().getTotalMemory() ) ) + "," );
		data.append( generateJsonPair( "memoryPercent" , String.valueOf( configurator.getSystemProperties().getMemoryPercent() ) ) + "," );
		data.append( generateJsonPair( "totalDiskSpace" , String.valueOf( configurator.getSystemProperties().getTotalDiskSpace() ) ) + "," );
		data.append( generateJsonPair( "freeDiskSpace" , String.valueOf( configurator.getSystemProperties().getFreeDiskSpace() ) ) + "," );
		data.append( generateJsonPair( "maximunLimit" , String.valueOf( configurator.getActivationsMaxLimit() ) ) + "," );
		data.append( addArray("data", sb.toString() ) ); 
		data.append("}");			
		
		logger.debug(" done sending Task Runners: " + data.toString() );
		
		communicator.doPost("receiveNodeTasks", "tasks", data.toString() );
		
	}
	
	/**
	 * Check if there is a command waiting for task buffer is flushed 
	 */
	private static boolean havePendentCommand() {
		if ( quiting ) {
			logger.debug("TaskManager is quiting... do not process tasks anymore");
			quit();
			return true;
		}
		if ( restarting ) {
			logger.debug("TaskManager is restarting... do not process tasks anymore");
			restart();
			return true;
		}

		
		// No command is in process. Can free TaskManager now...
		return false;
	}
	
	/**
	 * Will check if Sagitarii sent a special command to this node
	 * Returning TRUE will deny to run tasks ( may be a command or in flushing process )
	 * 
	 * WARNING: By returning FALSE ensure this "response" is a valid XML instance
	 * 	or the XML parser will throw an error.
	 * 
	 */
	private static boolean specialCommand( String response ) {
		logger.debug("checking preprocess");
		
		// RESULT:
		// FALSE = A valid XML instance. Will run a new task.
		// TRUE  = A Sagitarii command or we don't want to run new tasks 
		// 			even "response" is a valid XML instance
		
		if ( ( !response.equals( "NO_ANSWER" ) ) && ( !response.equals( "COMM_ERROR" ) ) && ( !response.equals( "" ) ) ) {
			
			if ( response.equals( "COMM_RESTART" ) ) {
				logger.warn("get restart command from Sagitarii");
				restart();
				return true;
			} 
				
			if ( response.equals( "COMM_QUIT" ) ) {
				logger.warn("get quit command from Sagitarii");
				quit();
				return true;
			} 
				
			if ( response.contains( "INFORM" ) ) {
				String[] data = response.split("#");
				logger.warn("Sagitarii is asking for Instance " + data[1] );
				inform( data[1] );
				// No need to stop TaskManager or flush buffers... just an information request
				// Avoid consider this response as a valid XML instance by returning "TRUE"
				return true;
			} 
				
		} else {
			logger.debug("invalid response from Sagitarii: " + response);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Will restart TaskManager
	 * It is a Sagitarii command
	 */
	public static void restartApplication() {
		try {
		  final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		  final File currentJar = new File ( TaskManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	
		  /* is it a jar file? */
		  if( !currentJar.getName().endsWith(".jar") ) {
		    return;
		  }
	
		  /* Build command: java -jar application.jar */
		  final ArrayList<String> command = new ArrayList<String>();
		  command.add( javaBin );
		  command.add( "-jar" );
		  command.add( currentJar.getPath() );
	
		  final ProcessBuilder builder = new ProcessBuilder(command);
		  builder.start();
		  System.exit(0);
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Restart TaskManager
	 */
	private static void restart() {
		restarting = true;
		if ( getRunners().size() > 0 ) {
			logger.debug("cannot restart now. " + getRunners().size() + " tasks still runnig");
		} else {
			logger.debug("restart now.");
			restartApplication();
		}
	}
	
	private static void inform( String instanceSerial ) {
		boolean found = false;
		for ( TaskRunner tr : getRunners() ) {
			if ( tr.getCurrentTask() != null ) {
				if ( tr.getCurrentActivation().getInstanceSerial().equals( instanceSerial ) ) {
					found = true;
					break;
				}
			}
		}
		String status = "";
		if ( found ) {
			status = "RUNNING";
			logger.debug("Instance "+instanceSerial+" is running");
		} else {
			status = "NOT_FOUND";
			logger.debug("Instance "+instanceSerial+" not found");
		}
		String parameters = "macAddress=" + configurator.getSystemProperties().getMacAddress() + 
				"&instance=" + instanceSerial + "&status=" + status;
		communicator.send("taskStatusReport", parameters);

	}
	
	/**
	 * Close TaskManager
	 */
	private static void quit() {
		quiting = true;
		if ( getRunners().size() > 0 ) {
			logger.debug("cannot quit now. " + getRunners().size() + " tasks still runnig");
		} else {
			logger.debug("quit now.");
			System.exit(0);
		}
	}
	
	public static List<TaskRunner> getRunners() {
		return new ArrayList<TaskRunner>( runners );
	}
	
	/**
	 * Remove all finished task threads from buffer
	 *  
	 */
	private static void clearRunners() {
		logger.debug("cleaning task runners...");
		int total = 0;
		Iterator<TaskRunner> i = runners.iterator();
		while ( i.hasNext() ) {
			TaskRunner req = i.next(); 
			if ( !req.isActive() ) {
				try {
					logger.debug(" > killing task runner " + req.getCurrentActivation().getExecutor() + " " + req.getSerial() + " (" + req.getCurrentTask().getTaskId() + ")" );
				} catch ( Exception e ) { 
					logger.debug(" > killing null task runner");
				}
				i.remove();
				total++;
			}
		}		
		logger.debug( total + " task runners deleted" );
	}

}
