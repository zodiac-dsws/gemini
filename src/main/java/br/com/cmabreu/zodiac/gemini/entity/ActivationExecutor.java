package br.com.cmabreu.zodiac.gemini.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import br.com.cmabreu.zodiac.gemini.types.ExecutorType;

@Entity
@Table(name="executors", indexes = {
        @Index(columnList = "id_activation_executor", name = "executor_id_hndx")
})    
public class ActivationExecutor {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id_activation_executor")
	private int idActivationExecutor;	

	@Column(length=150)
	private String executorAlias;

	@Column(length=50)
	private String hash;

	@Column(length=15)
	@Enumerated(EnumType.STRING)
	private ExecutorType type;
	
	@Column(length=150)
	private String activationWrapper;

	@Column(columnDefinition = "TEXT", name="sql")
	private String selectStatement;

	public String getExecutorAlias() {
		return executorAlias;
	}

	public void setExecutorAlias(String executorAlias) {
		this.executorAlias = executorAlias;
	}

	public String getSelectStatement() {
		return selectStatement;
	}

	public void setSelectStatement(String selectStatement) {
		this.selectStatement = selectStatement;
	}

	public String getActivationWrapper() {
		return activationWrapper;
	}

	public void setActivationWrapper(String activationWrapper) {
		this.activationWrapper = activationWrapper;
	}

	public int getIdActivationExecutor() {
		return idActivationExecutor;
	}

	public void setIdActivationExecutor(int idActivationExecutor) {
		this.idActivationExecutor = idActivationExecutor;
	}

	public ExecutorType getType() {
		return type;
	}

	public void setType(ExecutorType type) {
		this.type = type;
	}
	
	public void setHash(String hash) {
		this.hash = hash;
	}
	
	public String getHash() {
		return hash;
	}

}
