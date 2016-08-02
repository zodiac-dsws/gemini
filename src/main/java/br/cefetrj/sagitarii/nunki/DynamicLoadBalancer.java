package br.cefetrj.sagitarii.nunki;

public class DynamicLoadBalancer {
	private static Logger logger = LogManager.getLogger( "br.cefetrj.sagitarii.nunki.DynamicLoadBalancer" ); 
	private static int MAXIMUN_CPU_LIMIT = 95;
	private static int MINIMUN_CPU_LIMIT = 90;
	private static int INITIAL_TASK_LIMIT = 0;
	private static int DLB_FREQUENCY = 10;
	private static int defaultMaxLimit = 0;
	private static int testCount = 0;
	private static int noTaskCheckCount = 0;
	private static int MAXIMUN_RAM_TO_USE = 80;
	
	public static boolean tooHigh( double load, double ramLoad ) {
		boolean ramAboveLimit = ( ramLoad >= MAXIMUN_RAM_TO_USE );
		return ( load >= MAXIMUN_CPU_LIMIT ) || ( ramAboveLimit );
	}

	public static boolean tooLow( double load, double ramLoad ) {
		boolean ramBelowLimit = ( ramLoad < MAXIMUN_RAM_TO_USE );
		return ( load < MINIMUN_CPU_LIMIT ) && ramBelowLimit;
	}
	

	public synchronized static void equalize( Configurator configurator, int totalTasksRunning ) {
		boolean enforceTaskLimitToCores = configurator.enforceTaskLimitToCores();
		int maxLimitToGrow = configurator.getSystemProperties().getAvailableProcessors() +
				( configurator.getSystemProperties().getAvailableProcessors() / 2 ) ;
		
		if ( enforceTaskLimitToCores ) return;
		
		double load = configurator.getSystemProperties().getCpuLoad();
		double ramLoad = 100 - configurator.getSystemProperties().getMemoryPercent();
		int activationsMaxLimit = configurator.getActivationsMaxLimit();
		
		if ( ramLoad < 0 ) {
			ramLoad = 0;
		}
		
		DLB_FREQUENCY = configurator.getDLBFrequency();
		MAXIMUN_RAM_TO_USE = configurator.getMaximunRamToUse();
		MAXIMUN_CPU_LIMIT = configurator.getMaximunCPULimit();
		MINIMUN_CPU_LIMIT = configurator.getMinimunCPULimit();
		
		if ( activationsMaxLimit > maxLimitToGrow ) {
			activationsMaxLimit = maxLimitToGrow;
			configurator.setActivationsMaxLimit( activationsMaxLimit );
		}

		if ( INITIAL_TASK_LIMIT == 0 ) {
			INITIAL_TASK_LIMIT = activationsMaxLimit;
		}
		
		boolean tooLow = false;
		boolean tooHigh = false;
		
		
		if ( defaultMaxLimit == 0 ) {
			defaultMaxLimit = activationsMaxLimit;
			logger.debug("AML default value is " + INITIAL_TASK_LIMIT );
		}
		

		if ( totalTasksRunning == 0) {
			noTaskCheckCount++;
			if ( noTaskCheckCount > DLB_FREQUENCY ) {
				logger.debug("AML set to default value: " + INITIAL_TASK_LIMIT );
				activationsMaxLimit = INITIAL_TASK_LIMIT;
				configurator.setActivationsMaxLimit( activationsMaxLimit );
				noTaskCheckCount = 0;
			}
			return;
		}

		boolean acceptable = false;
		
		if ( tooHigh(load,ramLoad) ) {
			tooHigh = true;
		} else 
		
		if ( tooLow(load,ramLoad) ) {
			tooLow = true;
		} else {
			acceptable = true;
			testCount = 0;
		}

		testCount++;
		if ( !acceptable && ( testCount >= DLB_FREQUENCY ) ) {
			
			String where = "too low ";
			if ( tooLow && ( activationsMaxLimit < maxLimitToGrow ) ) {
				activationsMaxLimit++;
			}
			if ( tooHigh && ( activationsMaxLimit > 1 ) ) {
				where = "too high";
				activationsMaxLimit--;
			}
			
			testCount = 0;
			
			configurator.setActivationsMaxLimit( activationsMaxLimit );
	
			logger.debug( "[" + totalTasksRunning + "] RAM Load: " + ramLoad + "% | CPU Load: " + load + "% (" + where + ") : AML is now " + activationsMaxLimit);
			
		} else {
			//logger.debug(" Nothing was changed: " + testCount );
		}
		
	}
}
