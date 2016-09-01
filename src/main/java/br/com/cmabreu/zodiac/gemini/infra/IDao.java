package br.com.cmabreu.zodiac.gemini.infra;

import java.util.List;

import br.com.cmabreu.zodiac.gemini.exceptions.DeleteException;
import br.com.cmabreu.zodiac.gemini.exceptions.InsertException;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.exceptions.UpdateException;

public interface IDao<T> {
	 int insertDO(T objeto) throws InsertException;
	 void deleteDO(T objeto) throws DeleteException;
	 void updateDO(T objeto) throws UpdateException;
	 List<T> getList(String criteria) throws NotFoundException;
	 T getDO(int id) throws NotFoundException;
	 void executeQuery(String criteria, boolean withCommit) throws Exception;
	 public List<?> genericAccess(String hql) throws Exception;
	 int getCount(String tableName, String criteria) throws Exception;
}
