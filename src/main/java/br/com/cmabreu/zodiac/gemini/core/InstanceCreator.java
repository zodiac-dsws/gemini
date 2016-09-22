package br.com.cmabreu.zodiac.gemini.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import br.com.cmabreu.zodiac.gemini.entity.ActivationExecutor;
import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Domain;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.entity.Instance;
import br.com.cmabreu.zodiac.gemini.entity.Relation;
import br.com.cmabreu.zodiac.gemini.exceptions.DatabaseConnectException;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.services.ExecutorService;

public class InstanceCreator {
	private int order;
	private Fragment fragment;
	private List<String> nativeAttributes = new ArrayList<String>();
	private StringBuilder fileXMLEntries = new StringBuilder();
	private ExecutorService cs;
	
	public InstanceCreator() throws DatabaseConnectException {
		nativeAttributes.add("id_activity");
		nativeAttributes.add("index_id");
		nativeAttributes.add("id_experiment");
		nativeAttributes.add("id_instance");
		cs = new ExecutorService();
	}
	
	private boolean isNative( String attribute ) {
		return nativeAttributes.contains( attribute.toLowerCase() );
	}

	// Cria um instance para uma atividade (ou coloca varias atividades em uma mesma instancia
	// para serem executadas em sequencia pelo mesmo nó, caso activity.getNextActivities()
	// possua atividades a serem executadas em sequencia.
	public Instance createInstance( Activity activity, Fragment fragment, String parameter) throws Exception {
		this.fragment = fragment;
		order = 0;
		Instance pipe = new Instance();
		pipe.setIdFragment( fragment.getIdFragment() );
		pipe.setContent( getXMLContent(activity, parameter, pipe.getSerial() )  );
		pipe.setType( activity.getType() );
		pipe.setQtdActivations( order );
		return pipe;
	}
	
	// Gera um CSV no formato: 1a linha = colunas // 2a linha = dados
	public String generateInputData( UserTableEntity parameter ) {
		String retorno = "";
		String firstLine = "";
		String secondLine = "";
		
		// Generate the columns
		for( String column : parameter.getColumnNames() ) {
			if ( !isNative( column ) ) {
				firstLine = firstLine + column + ",";
			}
		}
		
		// Generate the data
		for( String column : parameter.getColumnNames() ) {
			if ( !isNative( column ) ) {
				String value = parameter.getData( column );
				secondLine = secondLine + value + ",";
			}
		}
		
		// Mount header columns + data line
		if ( ( firstLine.length() > 0 ) && ( secondLine.length() > 0 ) ) {
			firstLine = firstLine.substring(0, firstLine.length() - 1);
			secondLine = secondLine.substring(0, secondLine.length() - 1);
			retorno = firstLine + "\n" + secondLine;
		}
		return retorno;
	}	

	// Apos generateInputData criar uma linha com os nomes das colunas CSV e uma linha com dados,
	// pode ser necessario adicionar mais linhas de dados.
	public String appendInputData( UserTableEntity parameter, String sourceInputData ) {
		String retorno = "";
		String secondLine = "";
		for( String column : parameter.getColumnNames() ) {
			if ( !isNative( column ) ) {
				String value = parameter.getData( column );
				secondLine = secondLine + value + ",";
			}
		}
		if ( ( sourceInputData.length() > 0 ) && ( secondLine.length() > 0 ) ) {
			secondLine = secondLine.substring(0, secondLine.length() - 1);
			retorno = sourceInputData + "\n" + secondLine;
		}
		return retorno;
	}	

	/**
	 * Criar no instance:
	 * 	<file table='input_data' name='data1.zip' attribute='columnx' />
	 * 	<file table='input_data' name='data3.zip' attribute='columnx' />
	 * 
	 */
	private String getFilesAndConvertData( Activity activity, String parameter ) throws Exception {
		if ( parameter == null ) {
			debug("no parameter do convert data");
			return "";
		}
		// Resetar o buffer de entradas xml de arquivos.
		fileXMLEntries.setLength(0);
		boolean changed = false;
		
		String lines[] = parameter.split("\n");
		if ( lines.length == 0 ) {
			debug("no data to convert");
			return ""; 
		}

		String columnList = lines[0];
		debug("processing columns " + columnList );
		
		// Preparar para refazer o CSV de dados com os novos valores em "result"...
		String columns[] = columnList.split(",");
		int columnIndex = 0;
		// Para cada coluna
		for ( String column : columns ) {
			
			debug("checking domain for column " + column + "...");
			if ( activity.getInputRelations().size() == 0 ) {
				debug("no input relations found for activity " + activity.getSerial() );
			}
			
			for ( Relation inputRelation : activity.getInputRelations() ) {
				String sourceTable = inputRelation.getName();
				String domainName = sourceTable + "." + column;
				
				debug("checking domain " + domainName + "..." );
				Domain domain = DomainStorage.getInstance().getDomain( domainName );
				if ( domain != null ) {
					changed = true;
					debug( domainName + " is a File type field. scanning data lines..." );
					

					
					for ( int lineNumber = 1; lineNumber < lines.length; lineNumber++ ) {
						String newLine = "";
						String prefix = "";
						// Desmembra cada item de dados da linha CSV.
						String dataValues[] = lines[lineNumber].split(",");
						// Pega o valor do indice do arquivo, que estah na coluna columnIndex
						String fileNameAndPath = dataValues[columnIndex] ;
						debug( "using file " + fileNameAndPath + "..." );
						// Com o valor da coluna columnIndex 
						// tenta pegar o arquivo correspondente (este valor eh o indice do arquivo).
						
						File file = new File( fileNameAndPath );
						
						debug("found file " + file.getName() );
						// Troca o indice pelo nome na lista de dados da linha em questao.
						dataValues[columnIndex] = file.getName();
						
						fileXMLEntries.append("<file name='" + fileNameAndPath + "' table='" + domain.getTable().getName() + 
								"' attribute='" + column + "' />");
						
						// Refazer a linha, mas com o valor do arquivo trocado pelo nome.
						for ( String dataValue : dataValues ) {
							newLine = newLine + prefix + dataValue;
							prefix = ",";
						}

						// Adicionar a nova linha na nova lista CSV...
						lines[lineNumber] = newLine;
						
					}
					
				} else {
					debug(domainName + " is not a file domain.");
				}
			}
			columnIndex++;
		}
		
		if ( !changed ) {
			// Não foi encontrado nenhum campo do tipo "File".
			// Retornar exatamente o que foi entregue.
			return parameter;
		} else {
			// Houve mudançaas nos dados. Retornar o que foi modificado.
			StringBuilder result = new StringBuilder(); 
			for ( String line : lines ) {
				result.append( line +  "\n");
			}
			return result.toString();
		}
	}
	
	private String getActivityAsXML( Activity activity, String parameter ) throws Exception {
		StringBuilder sbu = new StringBuilder();
		String command = "";
		
		parameter = getFilesAndConvertData( activity, parameter );

		cs.newTransaction();
		debug("generating xml entry for activity " + activity.getSerial() + " " + activity.getTag() + " (" + activity.getType() + ")");
		debug("will use executor " + activity.getCommand() );
		
		
		ActivationExecutor executor;
		try {
			executor = cs.getExecutor( activity.getCommand() );
		} catch ( NotFoundException e ) {
			throw new Exception( e.getMessage() );
		}
		
		if ( activity.getType().isJoin() ) {
			debug("this activity will run a SQL script");
			
			String sql = executor.getSelectStatement().replace("%ID_EXP%",  String.valueOf( this.fragment.getExperiment().getIdExperiment() ) );
			sql = sql.replace("%ID_WFL%", String.valueOf( this.fragment.getExperiment().getWorkflow().getIdWorkflow() ));
			sql = sql.replace("%ID_ACT%", String.valueOf( activity.getIdActivity() ) );
			
			command = sql;
		} else {
			command = executor.getActivationWrapper();
			debug("this activity will run " + activity.getCommand() + " node application ");
		}

		
		sbu.append( "<activity>" );
		sbu.append( "<order>" );
		sbu.append( order );
		sbu.append( "</order>" );
		
		sbu.append( "<serial>" );
		sbu.append( activity.getSerial() );
		sbu.append( "</serial>" );

		sbu.append( "<executorType>" );
		sbu.append( executor.getType() );
		sbu.append( "</executorType>" );
		
		sbu.append( "<executor>" );
		sbu.append( activity.getExecutorAlias() );
		sbu.append( "</executor>" );
		
		sbu.append( "<type>" );
		sbu.append( activity.getType() );
		sbu.append( "</type>" );
		
		sbu.append( "<inputData>" );
			if ( parameter != null ) {
				sbu.append( parameter );
			}
		sbu.append( "</inputData>" );

		// Se getFilesAndConvertData produziu algum arquivo, as tags estar�o em fileXMLEntries
		sbu.append( "<files>" );
			sbu.append( fileXMLEntries.toString() );
		sbu.append( "</files>" );
		
		sbu.append( "<targetTable>" );
		sbu.append( activity.getOutputRelation().getName() );
		sbu.append( "</targetTable>" );

		
		sbu.append( "<command><![CDATA[" );
		sbu.append( command );
		sbu.append( "]]>"
				+ "</command>" );
		
		sbu.append( "</activity>" );
		order++;
		
		return sbu.toString();
	}
	
	/**
	 * Checa se uma atividade est� contida no fragmento sendo processado.
	 * 
	 */
	private boolean contains( Activity act ) {
		for ( Activity activity : fragment.getActivities() ) {
			if ( activity.equals( act ) ) {
				return true;
			}
		}
		return false;
	}

	private String goDeep( Activity act ) throws Exception {
		StringBuilder sbu = new StringBuilder();
		if ( contains(act) ) {
			sbu.append( getActivityAsXML(act, null) ); 

			debug("processing " + act.getSerial() + " next activities");
			for ( Activity next : act.getNextActivities() ) {
				sbu.append( goDeep(next) );
			}
		} 
		return sbu.toString();
	}
	
	private String getXMLContent( Activity act, String parameter, String instanceSerial ) throws Exception {
		debug("will generate xml data for instance " + instanceSerial + " with activity entry point " + act.getSerial() + " (" + act.getTag() + ")");
		StringBuilder sb = new StringBuilder();
		sb.append( "<?xml version='1.0' encoding='UTF-8'?>" );
		sb.append("<instance workflow='" +
				act.getFragment().getExperiment().getWorkflow().getTag() + "' experiment='" + 
				act.getFragment().getExperiment().getTagExec() + "' serial='" + instanceSerial + "' id='##TAG_ID_INSTANCE##'" +  
				" fragment='" + fragment.getSerial() + "'>" );
		sb.append( getActivityAsXML(act, parameter) );
		
		debug("processing " + act.getSerial() + " next activities");
		
		for( Activity next : act.getNextActivities() ) {
			sb.append( goDeep( next ) );
		}
		
		debug("instance " + instanceSerial + " xml data generation is done");
		sb.append( "</instance>" );
		return sb.toString();
	}

	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}		
	
}
