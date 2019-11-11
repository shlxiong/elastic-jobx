package com.openxsl.elasticjob.jpa;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataRetrievalFailureException;

import com.dangdang.ddframe.job.event.rdb.JobEventRdbSearch.Result;
import com.openxsl.config.util.BeanUtils;
import com.openxsl.config.util.StringUtils;
import com.openxsl.elasticjob.eventlog.JobExecutionBean;

/**
 * JDBC操作类
 * @author xiongsl
 */
public class JdbcUtils {
	private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);
	private static final ConcurrentHashMap<Class<?>, Field[]> JPA_FIELDS
					= new ConcurrentHashMap<Class<?>, Field[]>();
	private static final ConcurrentHashMap<Class<?>, String> TABLE_MAP
					= new ConcurrentHashMap<Class<?>, String>();
	
	public static void createTableIfNecessary(Class<?> beanClass, DataSource dataSource)
				throws SQLException{
		String tableName = mappingTableName(beanClass);
//		conn.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"});
		String sql = String.format("SELECT * FROM %s WHERE 1<>1", tableName);
		try ( Connection conn = dataSource.getConnection();
			  PreparedStatement preStmt = conn.prepareStatement(sql) ){
			preStmt.executeQuery().close();
		}catch(SQLException e) {
			sql = getCreateTableSql(beanClass);
			try {
				//除了CREATE TABLE，还有创建索引语句
				for (String str : StringUtils.split(sql,";")) {
					if (!"".equals(str)) {
						executeUpdate(str, null, dataSource);
					}
				}
			}catch(SQLException ex) {
				logger.warn("create table error: ", ex);
			}
		}
	}
	
	public static String getCreateTableSql(Class<?> beanClass) {
		StringBuilder sql = new StringBuilder("CREATE TABLE ");
		Table tableAnno = beanClass.getAnnotation(Table.class);
		sql.append(tableAnno.name()).append("(\n\t");
		String primaryKey = null, column;
		for (Field field : mappingJpaFields(beanClass)){
			Column anno = field.getAnnotation(Column.class);
			column = getColumn(field);
			sql.append(column).append("  ");
			final Class<?> ftype = field.getType();
			if (ftype == String.class) {
				sql.append("VARCHAR(").append(anno.length()).append(")");
			} else if (Date.class.isAssignableFrom(ftype)){
				sql.append((java.sql.Date.class==ftype) ? "DATE" : "TIMESTAMP");
			} else if (boolean.class==ftype || Boolean.class.isAssignableFrom(ftype)) {
				sql.append("CHAR(1)");
			} else if (int.class==ftype || Integer.class.isAssignableFrom(ftype)) {
				sql.append("INT");
			} else {
				sql.append("NUMERIC(").append(anno.precision())
					.append(",").append(anno.scale()).append(")");
			}
			boolean nilable = anno.nullable();
			if (field.isAnnotationPresent(Id.class)) {
				nilable = false;
				primaryKey = column;
			}
			sql.append(nilable ? " NULL" : " NOT NULL").append(",\n\t");
		}
		sql.append("PRIMARY KEY (").append(primaryKey).append(")\n);\n");
		
		for (Index index : tableAnno.indexes()) {
			sql.append("CREATE INDEX ").append(index.name()).append(" ON ")
				.append(tableAnno.name()).append("(")
				.append(StringUtils.camelToSplitName(index.columnList(),"_"))
				.append(");\n");
		}
		return sql.toString();
	}
	
	/**
	 * 插入新对象
	 */
	public static boolean insert(Object bean, DataSource dataSource) throws SQLException{
		Class<?> beanClass = bean.getClass();
		StringBuilder insertSql = new StringBuilder("INSERT INTO ");
		StringBuilder argsSql = new StringBuilder(") VALUES (");
		List<Object> values = new ArrayList<Object>();
		insertSql.append(mappingTableName(beanClass)).append("(");
		for (Field field : mappingJpaFields(beanClass)) {
			Object value = BeanUtils.getPrivateField(bean, field.getName());
			if (value == null) {
				continue;
			}
			insertSql.append(getColumn(field)).append(",");
			argsSql.append("?,");
			values.add(value);
		}
		argsSql.deleteCharAt(argsSql.length()-1).append(")");
		insertSql.deleteCharAt(insertSql.length()-1).append(argsSql.toString());
		String sql = insertSql.toString();
		
		return executeUpdate(sql, values, dataSource) > 0;
	}
	/**
	 * 修改记录
	 */
	public static int update(Object bean, QueryMap<Object> wheres, boolean skipNull,
							DataSource dataSource) throws SQLException {
		Class<?> beanClass = bean.getClass();
		StringBuilder buffer = new StringBuilder("UPDATE ");
		buffer.append(mappingTableName(beanClass)).append(" SET ");
		List<Object> values = new ArrayList<Object>();
		for (Field field : mappingJpaFields(beanClass)) {
			if (wheres.containsKey(field.getName())) {
				continue;
			}
			Object value = BeanUtils.getPrivateField(bean, field.getName());
			if (value==null && skipNull) {
				continue;
			}
			buffer.append(getColumn(field)).append("=?,");
			values.add(value);
		}
		buffer.deleteCharAt(buffer.length()-1);
		values.addAll(extractWhereSql(wheres, buffer));
		String sql = buffer.toString();
		
		return executeUpdate(sql, values, dataSource);
	}
	
	public static <T> List<T> query(QueryMap<?> wheres, Class<T> clazz, String orders,
							DataSource dataSource, String... fields) throws SQLException {
		
		List<Object> values = new ArrayList<Object>(0);
		String sql = getSql(wheres, clazz, orders, values, fields);
		return executeQuery(sql, values, clazz, dataSource);
	}
	
	public static <T> Result<T> query(QueryMap<?> wheres, Class<T> clazz, String orders,
							int pageNo, int pageSize,   //分页参数
							DataSource dataSource, String... fields) throws SQLException {
		List<Object> values = new ArrayList<Object>(0);
		String sql = getSql(wheres, clazz, orders, values, fields);
		int total = getCount(sql, values, dataSource);
		sql = getPageSql(sql, pageNo, pageSize);
		return new Result<T>(total, executeQuery(sql, values, clazz, dataSource));
	}
	
	public static final int executeUpdate(String sql, Collection<Object> values,
							DataSource dataSource) throws SQLException {
		logger.debug("execute sql: {}", sql);
		try ( Connection conn = dataSource.getConnection();
			  PreparedStatement preStmt = conn.prepareStatement(sql) ){
			if (values != null) {
				preparedArguments(preStmt, values);
			}
			return preStmt.executeUpdate();
		}
	}
	
	private static <T> List<T> executeQuery(String sql, Collection<Object> values,
							Class<T> entityClass, DataSource dataSource) throws SQLException {
		logger.debug("querySql: {}", sql);
		List<T> lstResult = new ArrayList<T>();
		try ( Connection conn = dataSource.getConnection();
			  PreparedStatement preStmt = conn.prepareStatement(sql) ){
			if (values != null) {
				preparedArguments(preStmt, values);
			}
			
			ResultSet rs = preStmt.executeQuery();
			while (rs.next()) {
				T mappedObject = org.springframework.beans.BeanUtils.instantiate(entityClass);
				lstResult.add(mappedObject);
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);

				ResultSetMetaData rsmd = rs.getMetaData();
				//多出Oracle分頁字段'RN'，所以限制 min
				final int columnCount = Math.min(rsmd.getColumnCount(),
									entityClass.getDeclaredFields().length);
				String column, field, errorMsg;
				for (int index = 1; index <= columnCount; index++) {
					column = rsmd.getColumnName(index);
					field = StringUtils.splitToCamel(column.toLowerCase(), "_");  //oracle大寫;
					errorMsg = String.format("Unable to map column '%s' to property '%s'", column, field);
					PropertyDescriptor pd = org.springframework.beans.BeanUtils
									.getPropertyDescriptor(entityClass, field);
					if (pd == null){
						throw new DataRetrievalFailureException(errorMsg+", no writable method");
					}
					try {
						Object value = org.springframework.jdbc.support.JdbcUtils.
											getResultSetValue(rs, index, pd.getPropertyType());
						if (value != null){
							bw.setPropertyValue(field, value);
						}
					}catch (NotWritablePropertyException ex) {
						throw new DataRetrievalFailureException(errorMsg, ex);
					}
				}
			}
			rs.close();
		}
		return lstResult;
	}
	private static int getCount(String sql, Collection<Object> values,
							DataSource dataSource) throws SQLException {
		sql = String.format("select COUNT(*) from (%s) a", sql);
		logger.debug("countSql: {}", sql);
		int count = 0;
		try ( Connection conn = dataSource.getConnection();
			  PreparedStatement preStmt = conn.prepareStatement(sql) ){
			if (values != null) {
				int idx = 1;
				for (Object value : values) {
					preStmt.setObject(idx++, value);
				}
			}
			ResultSet rs = preStmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
		}
		return count;
	}
	private static String getSql(QueryMap<?> wheres, Class<?> clazz, String orders,
								List<Object> arguments, String... fields){
		StringBuilder buffer = new StringBuilder("SELECT ");
		if (fields.length < 1) {
			buffer.append("*");
		}else {
			for (String field : fields) {
				buffer.append(getColumn(field,clazz)).append(",");
			}
			buffer.deleteCharAt(buffer.length()-1);
		}
		buffer.append(" FROM ").append(mappingTableName(clazz));
		if (wheres.size() > 0) {
			arguments.addAll(extractWhereSql(wheres, buffer)) ;
		}
		if (orders!=null && orders.length()>0) {
			buffer.append(" ORDER BY ");
			for (String orderby : StringUtils.split(orders, ",")) {
				String field = orderby.split(" ")[0];
				orders = orders.replace(field, getColumn(field,clazz));
			}
			buffer.append(orders);
		}
		
		logger.debug("querySql: {}", buffer.toString());
		return buffer.toString();
	}
	private static String getPageSql(String sql, int pageNo, int pageSize) {
		//mysql
//		String limiter = String.format(" limit %d, %d", pageNo*pageSize, pageSize);
//		return sql + limiter;
		//oracle
		String frmtPage = "select * from (\nSELECT A.*,ROWNUM RN FROM (%s)A WHERE ROWNUM<=%d\n) B where RN >= %d";
		return String.format(frmtPage, sql, (pageNo+1)*pageSize, pageNo*pageSize);
	}
	private static final Pattern EXPR_PATTERN = Pattern.compile("(\\w+)(\\s)*(.*)");
	private static List<Object> extractWhereSql(QueryMap<?> wheres, StringBuilder bufferSql) {
		List<Object> values = new ArrayList<Object>(wheres.size());
		bufferSql.append(" WHERE ");
		Matcher matcher;
		for (Map.Entry<String, ?> entry : wheres.entrySet()) {
			matcher = EXPR_PATTERN.matcher(entry.getKey());
			matcher.matches();
			String operator = matcher.group(3);
			String column = StringUtils.camelToSplitName(matcher.group(1), "_");
			if (entry.getValue() == null) {
				bufferSql.append(column).append(" IS NULL AND ");
			} else {
				operator = "".equals(operator) ? "=" : String.format(" %s ", operator);
				bufferSql.append(column).append(operator).append("? AND ");
				values.add(entry.getValue());
			}
		}
		int len = bufferSql.length();
		bufferSql.delete(len-5, len);
		return values;
	}
	
	public static void main(String[] args) {
		String str = "f1 like";
		str = "f1";
//		str = "f2 != ";
		Matcher matcher = EXPR_PATTERN.matcher(str);
		if (matcher.matches()) {
			System.out.println(matcher.group(1));
			System.out.println(matcher.group(2));
			System.out.println(matcher.group(3));
		}
		System.out.println(getCreateTableSql(JobExecutionBean.class));
	}
	
	private static Field[] mappingJpaFields(Class<?> beanClass) {
		if (!JPA_FIELDS.containsKey(beanClass)) {
			List<Field> lstField = new ArrayList<Field>();
			for (Field field : beanClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(Column.class)) {
					lstField.add(field);
				}
			}
			Field[] fields = new Field[lstField.size()];
			lstField.toArray(fields);
			JPA_FIELDS.put(beanClass, fields);
			lstField.clear();
		}
		return JPA_FIELDS.get(beanClass);
	}
	private static String mappingTableName(Class<?> beanClass) {
		if (!TABLE_MAP.containsKey(beanClass)) {
			TABLE_MAP.put(beanClass, beanClass.getAnnotation(Table.class).name());
		}
		return TABLE_MAP.get(beanClass);
	}
	private static final String getColumn(Field field) {
		String column = field.getAnnotation(Column.class).name();
		if ("".equals(column)) {
			column = StringUtils.camelToSplitName(field.getName(),"_");
		}
		return column;
	}
	private static final String getColumn(String fieldName, Class<?> beanClass) {
		try {
			Field field = beanClass.getDeclaredField(fieldName);
			String column = field.getAnnotation(Column.class).name();
			if ("".equals(column)) {
				column = StringUtils.camelToSplitName(field.getName(),"_");
			}
			return column;
		} catch (Exception e) {
			return StringUtils.camelToSplitName(fieldName,"_");
		} 
	}
	private static void preparedArguments(PreparedStatement preStmt, Collection<Object> values)
					throws SQLException{
		final boolean isOracle = isOracle(preStmt.getConnection());
		int idx = 1;
		for (Object value : values) {
			if (isOracle && value instanceof Date) {
				long milsecs = ((Date)value).getTime();
				preStmt.setTimestamp(idx++, new Timestamp(milsecs));
			} else {
				preStmt.setObject(idx++, value);
			}
		}
	}
	private static boolean isOracle(Connection conn) throws SQLException {
		return "Oracle".equals(conn.getMetaData().getDatabaseProductName());
	}
	
}
