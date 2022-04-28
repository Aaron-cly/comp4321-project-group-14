package repository;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Database {
    public static String DB_dir = "./apache-tomcat-10.0.20/bin/rocksdb/";
    private RocksDB DB;     // singleton
    private String fileName;

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
