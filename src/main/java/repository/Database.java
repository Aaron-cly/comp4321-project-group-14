package repository;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Database {
    // will only be appended to DB_dir if the classes are not running in JSP context
    public static String prefix = "./apache-tomcat-10.0.20/bin";
    public static String DB_dir = "./rocksdb/";
    private RocksDB DB;     // singleton
    private String fileName;

    static{
        var path = System.getProperty("user.dir");
        if(!path.contains("apache"))
            DB_dir = prefix + DB_dir.substring(1);
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
