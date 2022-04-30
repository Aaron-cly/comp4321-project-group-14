package repository;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/** An abstraction class for the rocksdb database instances */
public class Database {
    // will only be appended to DB_dir if the classes are not running in JSP context
    public static String projectFolder = "comp4321-project-group-14";
    public static String rootTomcatDirectory = "/apache-tomcat-10.0.20/webapps/ROOT/";
    public static String DB_dir;
    private RocksDB DB;     // singleton
    private String fileName;

    static{
        var pwd = System.getProperty("user.dir");
        var absoluteProjectRootDir = pwd.substring(0, pwd.indexOf(projectFolder)) + projectFolder;
        DB_dir = absoluteProjectRootDir + rootTomcatDirectory + "rocksdb/";
    }

    /** Sole constructor.
     *  Constructs a Database instance.
     * @param fileName  The fileName of the database.
     */
    public Database(String fileName) {
        this.fileName = fileName;
    }

    /** Gets the path of the database instance
     *
     * @return  The path of the database instance
     */
    public String getPath() {
        return DB_dir + fileName;
    }

    /** Getter Method for the singleton database instance
     *
     * @return The singleton database instance
     */
    public RocksDB getDB() {
        return this.DB;
    }

    /** Opens rocksdb connection of the database
     *
     * @throws RocksDBException Occurs when something goes wrong in the connection of rocksdb
     */
    public void openConnection() throws RocksDBException {
        Options options = new Options();
        options.setCreateIfMissing(true);
        DB = RocksDB.open(getPath());
    }

    /** Closes the rocksdb connection of the database */
    public void closeConnection() {
        if (DB != null) {
            DB.close();
        }
    }
}
