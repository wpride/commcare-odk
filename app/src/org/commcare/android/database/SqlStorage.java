/**
 * 
 */
package org.commcare.android.database;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import net.sqlcipher.DatabaseUtils;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import org.commcare.android.db.legacy.LegacyInstallUtils.CopyMapper;
import org.commcare.android.javarosa.AndroidLogger;
import org.commcare.android.tasks.ExceptionReportTask;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;

import android.database.Cursor;
import android.util.Pair;

/**
 * @author ctsims
 *
 */
public class SqlStorage<T extends Persistable> implements IStorageUtilityIndexed, Iterable<T> {
	
	private static final boolean DEBUG_MODE = false;  
	
	String table;
	Class<? extends T> ctype;
	EncryptedModel em;
	T t;
	DbHelper helper;
	
	protected SqlStorage() {}
	
	public SqlStorage(String table, Class<? extends T> ctype, DbHelper helper) {
		this.table = table;
		this.ctype = ctype;
		this.helper = helper;
		
		try {
			T e = (T)ctype.newInstance();
			if(e instanceof EncryptedModel) {
				em = (EncryptedModel)e;
			}
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getIDsForValue(java.lang.String, java.lang.Object)
	 */
	public Vector<Integer> getIDsForValue(String fieldName, Object value) {
		return getIDsForValues(new String[] {fieldName}, new Object[] { value} );
	}
	
	public Vector<Integer> getIDsForValues(String[] fieldNames, Object[] values) {
		Pair<String, String[]> whereClause = helper.createWhere(fieldNames, values, em, t);
		
		if(DEBUG_MODE) {
			String sql = SQLiteQueryBuilder.buildQueryString(false, table, new String[] {DbUtil.ID_COL} , whereClause.first,null, null, null,null);
			Cursor explain = helper.getHandle().rawQuery("EXPLAIN QUERY PLAN " + sql, whereClause.second);
			System.out.println("SQL: " + sql);
			DatabaseUtils.dumpCursor(explain);
			explain.close();
		}
		
		Cursor c = helper.getHandle().query(table, new String[] {DbUtil.ID_COL} , whereClause.first, whereClause.second,null, null, null);
		if(c.getCount() == 0) {
			c.close();
			return new Vector<Integer>();
		} else {
			c.moveToFirst();
			Vector<Integer> indices = new Vector<Integer>();
			int index = c.getColumnIndexOrThrow(DbUtil.ID_COL);
			while(!c.isAfterLast()) {		
				int id = c.getInt(index);
				indices.add(Integer.valueOf(id));
				c.moveToNext();
			}
			c.close();
			return indices;
		}
	}
	

	public Vector<T> getRecordsForValue(String fieldName, Object value) {
		return getRecordsForValues(new String[] {fieldName}, new Object[] {value});
	}
	
	public Vector<T> getRecordsForValues(String[] fieldNames, Object[] values) {
		Pair<String, String[]> whereClause = helper.createWhere(fieldNames, values, em, t);

		Cursor c = helper.getHandle().query(table, new String[] {DbUtil.DATA_COL} , whereClause.first, whereClause.second,null, null, null);
		if(c.getCount() == 0) {
			c.close();
			return new Vector<T>();
		} else {
			c.moveToFirst();
			Vector<T> indices = new Vector<T>();
			int index = c.getColumnIndexOrThrow(DbUtil.DATA_COL);
			while(!c.isAfterLast()) {		
				byte[] data = c.getBlob(index);
				indices.add(newObject(data));
				c.moveToNext();
			}
			c.close();
			return indices;
		}
	}
	
	public String getMetaDataFieldForRecord(int recordId, String rawFieldName) {
		String rid = String.valueOf(recordId);
		String scrubbedName = TableBuilder.scrubName(rawFieldName);
		Cursor c = helper.getHandle().query(table, new String[] {scrubbedName} , DbUtil.ID_COL + "=?", new String[] {rid}, null, null, null);
		if(c.getCount() == 0) {
			c.close();
			throw new NoSuchElementException("No record in table " + table + " for ID " + recordId);
		}
		c.moveToFirst();
		String result = c.getString(c.getColumnIndexOrThrow(scrubbedName));
		c.close();
		return result;

	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getRecordForValue(java.lang.String, java.lang.Object)
	 */
	public T getRecordForValues(String[] rawFieldNames, Object[] values) throws NoSuchElementException, InvalidIndexException {
		Pair<String, String[]> whereClause = helper.createWhere(rawFieldNames, values, em, t);
		Cursor c = helper.getHandle().query(table, new String[] {DbUtil.ID_COL, DbUtil.DATA_COL} , whereClause.first, whereClause.second,null, null, null);
		if(c.getCount() == 0) {
			throw new NoSuchElementException("No element in table " + table + " with names " + rawFieldNames +" and values " + values.toString());
		}
		if(c.getCount() > 1) {
			 throw new InvalidIndexException("Invalid unique column set" + rawFieldNames + ". Multiple records found with value " + values.toString(), rawFieldNames.toString());
		}
		c.moveToFirst();
		byte[] data = c.getBlob(c.getColumnIndexOrThrow(DbUtil.DATA_COL));
		c.close();
		return newObject(data);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getRecordForValue(java.lang.String, java.lang.Object)
	 */
	public T getRecordForValue(String rawFieldName, Object value) throws NoSuchElementException, InvalidIndexException {
		Pair<String, String[]> whereClause = helper.createWhere(new String[] {rawFieldName}, new Object[] {value}, em, t);
		
		if(DEBUG_MODE) {
			String sql = SQLiteQueryBuilder.buildQueryString(false, table, new String[] {DbUtil.ID_COL} , whereClause.first,null, null, null,null);
			Cursor explain = helper.getHandle().rawQuery("EXPLAIN QUERY PLAN " + sql, whereClause.second);
			DatabaseUtils.dumpCursor(explain);
			explain.close();
		}
		
		String scrubbedName = TableBuilder.scrubName(rawFieldName);
		Cursor c = helper.getHandle().query(table, new String[] {DbUtil.DATA_COL} ,whereClause.first, whereClause.second, null, null, null);
		if(c.getCount() == 0) {
			c.close();
			throw new NoSuchElementException("No element in table " + table + " with name " + scrubbedName +" and value " + value.toString());
		}
		if(c.getCount() > 1) {
			c.close();
			throw new InvalidIndexException("Invalid unique column " + scrubbedName + ". Multiple records found with value " + value.toString(), scrubbedName);
		}
		c.moveToFirst();
		byte[] data = c.getBlob(c.getColumnIndexOrThrow(DbUtil.DATA_COL));
		c.close();
		return newObject(data);
	}
	
	public T newObject(byte[] data) {
		try {
			T e = (T)ctype.newInstance();
			e.readExternal(new DataInputStream(new ByteArrayInputStream(data)), helper.getPrototypeFactory());
			
			return e;
		} catch (IllegalAccessException e) {
			throw logAndWrap(e, "Illegal Access Exception");
		} catch (InstantiationException e) {
			throw logAndWrap(e, "Instantiation Exception");
		} catch (IOException e) {
			throw logAndWrap(e, "Totally non-sensical IO Exception");
		} catch (DeserializationException e) {
			throw logAndWrap(e, "CommCare ran into an issue deserializing data");
		}
	}
	
	private RuntimeException logAndWrap(Exception e, String message) {
		RuntimeException re = new RuntimeException(message + " while inflating type " + ctype.getName());
		re.initCause(e);
		Logger.log(AndroidLogger.TYPE_ERROR_STORAGE, ExceptionReportTask.getStackTrace(re, true));
		return re;
	}
	
	
	
	

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#add(org.javarosa.core.util.externalizable.Externalizable)
	 */
	public int add(Externalizable e) throws StorageFullException {
		SQLiteDatabase db = helper.getHandle();
		int i = -1;
		try{
			db.beginTransaction();
			long ret = db.insertOrThrow(table, DbUtil.DATA_COL, helper.getContentValues(e));
			
			if(ret > Integer.MAX_VALUE) {
				throw new RuntimeException("Waaaaaaaaaay too many values");
			}
		
			i =(int)ret;
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		return i;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#close()
	 */
	public void close() {
		helper.getHandle().close();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#destroy()
	 */
	public void destroy() {
		//nothing;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#exists(int)
	 */
	public boolean exists(int id) {
		Cursor c = helper.getHandle().query(table, new String[] {DbUtil.ID_COL} , DbUtil.ID_COL +"= ? ", new String[] {String.valueOf(id)}, null, null, null);
		if(c.getCount() == 0) {
			c.close();
			return false;
		}
		if(c.getCount() > 1) {
			c.close();
			throw new InvalidIndexException("Invalid ID column. Multiple records found with value " + id, "ID");
		}
		c.close();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#getAccessLock()
	 */
	public SQLiteDatabase getAccessLock() {
		return helper.getHandle();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#getNumRecords()
	 */
	public int getNumRecords() {
		Cursor c = helper.getHandle().query(table, new String[] {DbUtil.ID_COL} , null, null, null, null, null);
		int records = c.getCount();
		c.close();
		return records;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#getRecordSize(int)
	 */
	public int getRecordSize(int id) {
		//serialize and test blah blah.
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#getTotalSize()
	 */
	public int getTotalSize() {
		//serialize and test blah blah.
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#isEmpty()
	 */
	public boolean isEmpty() {
		if(getNumRecords() == 0) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#iterate()
	 */
	public SqlStorageIterator<T> iterate() {
		return iterate(true);
	}
	
	/**
	 * Creates a custom iterator for this storage which can either include or exclude the actual data.
	 * 
	 * Useful for getting an overview of data for querying into without wasting the bits to transfer over
	 * the huge full records. 
	 * 
	 * @param includeData True to return an iterator with all records. False to return only the index.
	 */
	public SqlStorageIterator<T> iterate(boolean includeData) {
		String[] projection = includeData ? new String[] {DbUtil.ID_COL, DbUtil.DATA_COL} : new String[] {DbUtil.ID_COL};
		Cursor c = helper.getHandle().query(table,  projection, null, null, null, null, DbUtil.ID_COL);
		return new SqlStorageIterator<T>(c, this);
	}
	
	
	public Iterator<T> iterator() {
		return iterate();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#read(int)
	 */
	public T read(int id) {
		return newObject(readBytes(id));
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#readBytes(int)
	 */
	public byte[] readBytes(int id) {
		Cursor c = helper.getHandle().query(table, new String[] {DbUtil.ID_COL, DbUtil.DATA_COL} , DbUtil.ID_COL +"=?", new String[] {String.valueOf(id)}, null, null, null);
		
		c.moveToFirst();
		byte[] blob = c.getBlob(c.getColumnIndexOrThrow(DbUtil.DATA_COL));
		c.close();
		return blob;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#remove(int)
	 */
	public void remove(int id) {
		SQLiteDatabase db = helper.getHandle();
		db.beginTransaction();
		try {
			db.delete(table, DbUtil.ID_COL +"=?",new String[] {String.valueOf(id)});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#remove(int)
	 */
	public void remove(List<Integer> ids) {
		if(ids.size() == 0 ) { return; }
		SQLiteDatabase db = helper.getHandle();
		db.beginTransaction();
		try {
			List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(ids);
			for(Pair<String, String[]> whereParams : whereParamList) {
				int rowsRemoved = db.delete(table, DbUtil.ID_COL +" IN " + whereParams.first, whereParams.second);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}	
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#remove(org.javarosa.core.services.storage.Persistable)
	 */
	public void remove(Persistable p) {
		this.remove(p.getID());
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#removeAll()
	 */
	public void removeAll() {
		SQLiteDatabase db = helper.getHandle();
		db.beginTransaction();
		try {
			db.delete(table, null,null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#removeAll(org.javarosa.core.services.storage.EntityFilter)
	 */
	public Vector<Integer> removeAll(EntityFilter ef) {
		Vector<Integer> removed = new Vector<Integer>();
		for(IStorageIterator iterator = this.iterate() ; iterator.hasMore() ;) {
			int id = iterator.nextID();
			switch(ef.preFilter(id, null)){
			case EntityFilter.PREFILTER_INCLUDE:
				removed.add(id);
				continue;
			case EntityFilter.PREFILTER_EXCLUDE:
				continue;
			}
			if(ef.matches(read(id))) {
				removed.add(id);
			}
		}
		
		if(removed.size() == 0) { return removed; }
		
		List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(removed);

		
		SQLiteDatabase db = helper.getHandle();
		db.beginTransaction();
		try {
			for(Pair<String, String[]> whereParams : whereParamList) {
				db.delete(table, DbUtil.ID_COL +" IN " + whereParams.first, whereParams.second);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}	
		
		return removed;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#repack()
	 */
	public void repack() {
		//Unecessary!
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#repair()
	 */
	public void repair() { 
		//Unecessary!
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#update(int, org.javarosa.core.util.externalizable.Externalizable)
	 */
	public void update(int id, Externalizable e) throws StorageFullException {
		SQLiteDatabase db = helper.getHandle();
		db.beginTransaction();
		try {
			db.update(table, helper.getContentValues(e), DbUtil.ID_COL +"=?", new String[] {String.valueOf(id)});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageUtility#write(org.javarosa.core.services.storage.Persistable)
	 */
	public void write(Persistable p) throws StorageFullException {
		if(p.getID() != -1) {
			update(p.getID(), p);
			return;
		}
		SQLiteDatabase db = helper.getHandle();
		try {
		
		db.beginTransaction();
		long ret = db.insertOrThrow(table, DbUtil.DATA_COL, helper.getContentValues(p));
		
		if(ret > Integer.MAX_VALUE) {
			throw new RuntimeException("Waaaaaaaaaay too many values");
		}
		
		int id = (int)ret;
		//Now we need to put the id into the record
		
		p.setID(id);
		db.update(table, helper.getContentValues(p), DbUtil.ID_COL +"=?" , new String[] {String.valueOf(id)});
		
		db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void setReadOnly() {
		//TODO: Implement (although I doubt there's much useful stuff to do)
	}

	public void registerIndex(String filterIndex) {
		// TODO Auto-generated method stub
	}
	
	public static <T extends Persistable> Map<Integer, Integer> cleanCopy(SqlStorage<T> from, SqlStorage<T> to) throws StorageFullException {
		return cleanCopy(from, to, null);
	}
	
	public static <T extends Persistable> Map<Integer, Integer> cleanCopy(SqlStorage<T> from, SqlStorage<T> to, CopyMapper<T> mapper) throws StorageFullException {
		to.removeAll();
		SQLiteDatabase toDb = to.helper.getHandle();
		try{
			Hashtable<Integer, Integer> idMapping = new Hashtable<Integer, Integer>();
			toDb.beginTransaction();
			
			for(T t : from) {
				int key = t.getID();
				//Clear the ID, we don't wanna guarantee it
				t.setID(-1);
				if(mapper != null){
					t = mapper.transform(t);
				}
				to.write(t);
				idMapping.put(key, t.getID());
			}
			toDb.setTransactionSuccessful();
			return idMapping;
		} finally {
			toDb.endTransaction();
		}
	}
}
