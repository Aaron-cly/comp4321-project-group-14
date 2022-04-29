package repository;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

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

    public Database(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return DB_dir + fileName;
    }

    public RocksDB getDB() {
        return this.DB;
    }

    public void openConnection() throws RocksDBException {
        Options options = new Options();
        options.setCreateIfMissing(true);
        DB = RocksDB.open(getPath());
    }

    public void closeConnection() {
        if (DB != null) {
            DB.close();
        }
    }
}
