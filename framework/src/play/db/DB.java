package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPAContext;
import play.exceptions.DatabaseException;

public class DB {

    public static DataSource datasource = null;

    public static void init() {
        if (changed()) {
            try {
                System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
                System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
                Properties p = Play.configuration;
                ComboPooledDataSource ds = new ComboPooledDataSource();
                ds.setDriverClass(p.getProperty("db.driver"));
                ds.setJdbcUrl(p.getProperty("db.url"));
                ds.setUser(p.getProperty("db.user"));
                ds.setPassword(p.getProperty("db.pass"));
                ds.setAcquireRetryAttempts(1);
                ds.setAcquireRetryDelay(0);
                ds.setCheckoutTimeout(1000);
                ds.setBreakAfterAcquireFailure(true);
                ds.setMaxPoolSize(30);
                ds.setMinPoolSize(10);
                datasource = ds;
                Logger.info("The database is ready");
            } catch (Exception e) {
                Logger.debug(e);
            }
        }
    }
        
    public static void close() {  
        if(localConnection.get() != null) {
            try {
                Connection connection = localConnection.get();
                localConnection.set(null);
                connection.close();
            } catch(Exception e) {
                throw new DatabaseException("It's possible than the connection was not propertly closed !", e);
            }
        }
    }
    
    static ThreadLocal<Connection> localConnection = new ThreadLocal<Connection>();
    
    public static Connection getConnection() {
        try {
            if(JPA.isEnabled()) {
                return ((org.hibernate.ejb.EntityManagerImpl) JPAContext.getEntityManager()).getSession().connection();
            }
            if(localConnection.get() != null) {
                return localConnection.get();
            }
            Connection connection = datasource.getConnection();
            localConnection.set(connection);
            return connection;
        } catch (SQLException ex) {
            throw new DatabaseException("Cannot obtain a new connection ("+ex.getMessage()+")", ex);
        }
    }
    
    public static boolean execute(String SQL) {
        try {
            return getConnection().createStatement().execute(SQL);
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        } 
    }
    
    public static ResultSet executeQuery(String SQL) {
        try {
            return getConnection().createStatement().executeQuery(SQL);
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        }
    }

    private static boolean changed() {
        Properties p = Play.configuration;
        
        if("mem".equals(p.getProperty("db"))) {
            p.put("db.driver", "org.hsqldb.jdbcDriver");
            p.put("db.url", "jdbc:hsqldb:mem:playembed");
            p.put("db.user", "sa");
            p.put("db.pass", "");
        }
        
        if("fs".equals(p.getProperty("db"))) {
            p.put("db.driver", "org.hsqldb.jdbcDriver");
            p.put("db.url", "jdbc:hsqldb:file:"+(new File(Play.applicationPath, "db/db").getAbsolutePath()));
            p.put("db.user", "sa");
            p.put("db.pass", "");
        }

        if ((p.getProperty("db.driver") == null) || (p.getProperty("db.url") == null) || (p.getProperty("db.user") == null) || (p.getProperty("db.pass") == null)) {
            return false;
        }
        if (datasource == null) {
            return true;
        } else {
            ComboPooledDataSource ds = (ComboPooledDataSource) datasource;
            if (!p.getProperty("db.driver").equals(ds.getDriverClass())) {
                return true;
            }
            if (!p.getProperty("db.url").equals(ds.getJdbcUrl())) {
                return true;
            }
            if (!p.getProperty("db.user").equals(ds.getUser())) {
                return true;
            }
            if (!p.getProperty("db.pass").equals(ds.getPassword())) {
                return true;
            }
        }
        return false;
    }
}