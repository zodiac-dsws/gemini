package br.com.cmabreu.zodiac.gemini.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.cmabreu.zodiac.gemini.entity.ActivationExecutor;
import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Consumption;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.entity.Instance;
import br.com.cmabreu.zodiac.gemini.services.ExecutorService;
import br.com.cmabreu.zodiac.gemini.services.RelationService;


public class ReduceInstanceGenerator implements IInstanceGenerator {
		
	@Override
	public List<Instance> generateInstances(Activity activity, Fragment frag) throws Exception {
		String relation = activity.getInputRelation().getName();
		debug( "Activity '" + activity.getTag() + "' allowed to run. Fetching data from source table " + relation );
		debug("generating instances...");		

		InstanceCreator pc = new InstanceCreator();
		List<Instance> pipes = new ArrayList<Instance>();
		RelationService ts = new RelationService();

		ExecutorService cs = new ExecutorService();
		ActivationExecutor executor = cs.getExecutor( activity.getCommand() );

		// fieldsDef : campos de agrupamento. Ex: UF,SEXO
		// Precisa ser separado por vírgula.
		String fieldsDef = executor.getSelectStatement();

		if ( (fieldsDef != null) && ( !fieldsDef.equals("") ) ) {

			// Separa os registros de acordo com o criterio de agrupamento

			String correctSql = "select u.* "
					+ " from experiments exp left join " + relation + " u on u.id_experiment = exp.id_experiment"  
					+ " left join instances p on p.id_instance = u.id_instance"
					+ " where exp.id_experiment = " + frag.getExperiment().getIdExperiment();
			
			String groupingSql = "select distinct "+ fieldsDef + " from (" + correctSql + ") r1 order by " + fieldsDef;
			
			debug( groupingSql );
			
			Set<UserTableEntity> groupedFields = ts.genericFetchList( groupingSql );
			String[] fields = fieldsDef.split(",");
			String selectionSql = "";
			String prefix = "";
			if ( (fields != null) && ( fields.length > 0 ) && ( groupedFields.size() > 0 ) ) {
				debug("'REDUCE' type detected: " + groupedFields.size() + " instances will be created for activity " + activity.getTag());
				StringBuilder sb = new StringBuilder();
				for ( UserTableEntity ute : groupedFields ) {
					sb.setLength(0);
					prefix = "";
					for ( String field : fields  ) {
						String value = ute.getData( field );
						sb.append( prefix + field + " = '" + value + "'" );
						prefix = " and ";
					}
					String queryDef = sb.toString();
					// Para cada registro do agrupado, repete enquando os campos do agrupamento forem iguais:
					// Ex: UF,SEXO
					// Repete: 
					// SP,MASCULINO : 8 instances gerados
					// SP,FEMININO  : 12 instances gerados
					// RJ,MASCULINO : 3 instances gerados
					selectionSql = "select * from (" + correctSql + ") r1 where " + queryDef;
					
					debug( selectionSql );
					
					if ( !selectionSql.trim().equals("") ) {
						Set<UserTableEntity> utes = ts.genericFetchList(selectionSql);
						debug(utes.size() + " lines of data was selected for this query");
						String parameter = "";
						
						Set<Consumption> consumptions = new HashSet<Consumption>();
						
						for ( UserTableEntity uteInternal : utes ) {
							if ( parameter.equals("") ) {
								parameter = pc.generateInputData( uteInternal );
							} else {
								parameter = pc.appendInputData(uteInternal, parameter);
							}
							
							// CONSUMPTION REGISTER
							int idRow = Integer.valueOf( uteInternal.getData("index_id") );
							int idTable = activity.getInputRelation().getIdTable();
							Consumption con = new Consumption();
							con.setIdRow(idRow);
							con.setIdTable(idTable);
							con.setIdActivity( activity.getIdActivity() );
							consumptions.add(con);
							// ===============================
							
						}
						Instance pipe = pc.createInstance(activity, frag, parameter);
						
						pipe.setConsumptions(consumptions);
						
						debug(" > Instance serial : " + pipe.getSerial() );
						
						pipes.add(pipe);
					} 
					
				}
				debug("done. " + pipes.size() + " instance generated.");
				
			} else {
				error("Empty grouping fields descriptor for " + executor.getExecutorAlias() );
			}
			
		}
		
		return pipes;
	}

	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}		
	
	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}		
}
