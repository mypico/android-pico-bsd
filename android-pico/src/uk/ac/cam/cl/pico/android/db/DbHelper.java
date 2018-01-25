/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.db;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.data.pairing.KeyPairingAccessor;
import uk.ac.cam.cl.pico.data.pairing.LensPairingAccessor;
import uk.ac.cam.cl.pico.db.DbKeyPairingAccessor;
import uk.ac.cam.cl.pico.db.DbKeyPairingImp;
import uk.ac.cam.cl.pico.db.DbLensPairingAccessor;
import uk.ac.cam.cl.pico.db.DbLensPairingImp;
import uk.ac.cam.cl.pico.db.DbPairingImp;
import uk.ac.cam.cl.pico.db.DbServiceImp;
import uk.ac.cam.cl.pico.db.DbSessionImp;
import uk.ac.cam.cl.pico.db.DbTerminalImp;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also
 * usually provides the DAOs used by the other classes.
 */
final public class DbHelper extends OrmLiteSqliteOpenHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DbHelper.class.getSimpleName());

    // name of the database file for your application -- change to something appropriate for your
    // app
    public static final String DATABASE_NAME = "pico.db";

    // any time you make changes to your database objects, you may have to increase the database
    // version
    private static final int DATABASE_VERSION = 21;
    
    private static DbHelper instance;
    
    public static DbHelper getInstance(final Context context) {
    	if (instance == null) {
    		instance = new DbHelper(context.getApplicationContext());
    	}
    	return instance;
    }
    
    private Dao<DbServiceImp, Integer> serviceDao;
    private Dao<DbPairingImp, Integer> pairingDao;
    private Dao<DbKeyPairingImp, Integer> keyPairingDao;
    private Dao<DbLensPairingImp, Integer> lensPairingDao;
    private Dao<DbSessionImp, Integer> sessionDao;
    private Dao<DbTerminalImp, Integer> terminalDao;
    
    private DbKeyPairingAccessor keyPairingAccessor;
    private DbLensPairingAccessor lensPairingAccessor;

    /**
	 * @deprecated use {@link #getInstance} instead.
     */
    @Deprecated
    public DbHelper(final Context context) {

        // This is an optimisation which reduces the time taken to creates the
        // DAOs, but the config file must be re-generated everytime the database
        // schema changes, so leave it turned off for the time being.
    	
        // super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        LOGGER.debug("DatabaseHelper constructed");
    }

    /**
     * This is called when the database is first created. Creates the required database tables.
     */
    @Override
    public void onCreate(final SQLiteDatabase db, final ConnectionSource connectionSource) {
        try {
            LOGGER.debug("Creating database tables...");
            TableUtils.createTable(connectionSource, DbServiceImp.class);
            TableUtils.createTable(connectionSource, DbPairingImp.class);
            TableUtils.createTable(connectionSource, DbKeyPairingImp.class);
            TableUtils.createTable(connectionSource, DbLensPairingImp.class);
            TableUtils.createTable(connectionSource, DbSessionImp.class);
            TableUtils.createTable(connectionSource, DbTerminalImp.class);
            LOGGER.info("All database tables created");
        } catch (SQLException e) {
            LOGGER.error("Database creation failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Enable foreign key constraints:
     * {@link http://http://stackoverflow.com/questions/6789075/deleting-using-ormlite-on-android}
     */
    @Override
    public void onOpen(final SQLiteDatabase db){
        super.onOpen(db);
        if (!db.isReadOnly()){
            db.setForeignKeyConstraintsEnabled(true);
        }
    }
    
    /**
     * This is called when your application is upgraded and it has a higher version number (see
     * {@link #DATABASE_VERSION DATABASE_VERSION}. Updates the required database tables.
     */
    @Override
    public void onUpgrade(final SQLiteDatabase db, final ConnectionSource connectionSource,
            final int oldVersion, final int newVersion) {
        LOGGER.info("Upgrading database from version " + oldVersion + " to " + newVersion);
        try {
            LOGGER.debug("Dropping database tables...");
            // true arguments means any SQLExceptions which occur will be caught
            // and suppressed. This flag is set, because otherwise the
            // addition of new tables (or possibly changes to table which affect
            // their indices) causes runtime exceptions ("Pico has stopped 
            // working.")
            TableUtils.dropTable(connectionSource, DbServiceImp.class, true);
            TableUtils.dropTable(connectionSource, DbPairingImp.class, true);
            TableUtils.dropTable(connectionSource, DbKeyPairingImp.class, true);
            TableUtils.dropTable(connectionSource, DbLensPairingImp.class, true);
            TableUtils.dropTable(connectionSource, DbSessionImp.class, true);
            TableUtils.dropTable(connectionSource, DbTerminalImp.class, true);
            LOGGER.debug("All tables dropped");
            
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            LOGGER.error("Database upgrade failed", e);
            throw new RuntimeException(e);
        }
    }
    
    Dao<DbServiceImp, Integer> getServiceDao() throws SQLException {
    	if (serviceDao == null) {
    		serviceDao = getDao(DbServiceImp.class);
    	}
    	return serviceDao;
    }
    
    Dao<DbPairingImp, Integer> getPairingDao() throws SQLException {
    	if (pairingDao == null) {
    		pairingDao = getDao(DbPairingImp.class);
    	}
    	return pairingDao;
    }
    
    Dao<DbKeyPairingImp, Integer> getKeyPairingDao() throws SQLException {
    	if (keyPairingDao == null) {
    		keyPairingDao = getDao(DbKeyPairingImp.class);
    	}
    	return keyPairingDao;
    }
    
    Dao<DbLensPairingImp, Integer> getLensPairingDao() throws SQLException {
    	if (lensPairingDao == null) {
    		lensPairingDao = getDao(DbLensPairingImp.class);
    	}
    	return lensPairingDao;
    }
    
    Dao<DbSessionImp, Integer> getSessionDao() throws SQLException {
    	if (sessionDao == null) {
    		sessionDao = getDao(DbSessionImp.class);
    	}
    	return sessionDao;
    }
    
    Dao<DbTerminalImp, Integer> getTerminalDao() throws SQLException {
    	if (terminalDao == null) {
    		terminalDao = getDao(DbTerminalImp.class);
    	}
    	return terminalDao;
    }
    
    public KeyPairingAccessor getKeyPairingAccessor() throws SQLException {
    	if (keyPairingAccessor == null) {
    		keyPairingAccessor = new DbKeyPairingAccessor(
    				getKeyPairingDao(), getPairingDao(), getServiceDao());
    	}
    	return keyPairingAccessor;
    }
    
    public LensPairingAccessor getLensPairingAccessor() throws SQLException {
    	if (lensPairingAccessor == null) {
    		lensPairingAccessor = new DbLensPairingAccessor(
    				getLensPairingDao(), getPairingDao(), getServiceDao());
    	}
    	return lensPairingAccessor;
    }
}