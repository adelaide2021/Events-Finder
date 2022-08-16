package db;

import db.mysql.MySQLConnection;

//获得对后台支持的数据库的一个handle
//具体实现用db.mysql里面的方法
public class DBConnectionFactory {
	// This should change based on the pipeline.
	private static final String DEFAULT_DB = "mysql";
	
	// 用factory函数确定具体用哪个数据库实现
	// 工厂模式简化的例子
	public static DBConnection getConnection(String db) {
		switch (db) {
		case "mysql":
			return new MySQLConnection();
		case "mongodb":
			// return new MongoDBConnection();
			return null;
		default:
			throw new IllegalArgumentException("Invalid db: " + db);
		}
	}
	
	// 实现通过mysql连接database
	public static DBConnection getConnection() {
		return getConnection(DEFAULT_DB);
	}


}
