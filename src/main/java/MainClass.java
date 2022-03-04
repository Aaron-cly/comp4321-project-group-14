import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.IOException;
import java.util.Arrays;

public class MainClass {

    public static void main(String[] args) {
        System.out.println("Message from MainClass");
        runParser();
    }

    public static void runParser() {
        final String url = "http://www.cse.ust.hk";
        Connection conn = Jsoup.connect(url);
        try {
            Response res = conn.execute();
            Boolean redirectFlag = res.hasHeader("location");
            String redirectAddr = res.header("location");
            String lastModified = res.header("last-modified");
            System.out.println(redirectFlag);
            System.out.println(redirectAddr);
            System.out.println(lastModified);

            Options options = new Options();
//            RocksDB db = RocksDB.open(options, "rocksdb");
//
//            RocksIterator iter = db.newIterator();
//            byte[] key1 = "key 1".getBytes();
//            byte[] value1 = "value 1".getBytes();
//            byte[] key2 = "key 2".getBytes();
//            byte[] value2 = "value 2".getBytes();
//            db.put(key1,value1);
//            db.put(key2,value2);
//            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
//                System.out.println(Arrays.toString(iter.key()));
//                System.out.println(Arrays.toString(iter.value()));
//            }
        } catch (HttpStatusException e) {
        } catch (IOException e) {
        }
    }

}
