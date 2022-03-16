import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import repository.Repository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static indexer.Indexer.metaInfoDb;

public class ResultWriter {
    private static String dbPath_Forwad_Frequency = "Forward_Frequency";
    private static String dbPath_Page = "Page";
    private static String dbPath_Word = "Word";
    private static String dbPath_Page_Info = "Page_Info";
    private static RocksDB forward_frequency_table;
    private static RocksDB page;
    private static RocksDB word;
    private static RocksDB page_info;

    private static String DELIMITER = ";";
    private static String SEPARATOR = "::";

    static {
        RocksDB.loadLibrary();
        Options options = new Options();
        try {
            forward_frequency_table = RocksDB.open(options, dbPath_Forwad_Frequency);
            page = RocksDB.open(options, dbPath_Page);
            word = RocksDB.open(options, dbPath_Word);
        } catch (RocksDBException e) {
        }
    }

    public static void write_spider_result() {
//        String directory = Paths.get("").toAbsolutePath().toString();
//        try (var writer = new BufferedWriter(new FileWriter("spider_result.txt"))) {
//            writer.write("");
//            var iter = page_info.newIterator();
//            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
////                var currentMetaData = MetaData.deserialize(iter.value());
////                var currentPageInfo = Repository.PageInfo.
//                System.out.println(new String(iter.key()) + ": \n" + currentMetaData);
//                writer.append(currentMetaData.pgTitle + "\n");
//                writer.append(currentMetaData.url + "\n");
//                writer.append(currentMetaData.lastModifiedDate.toString() + "\n");
//                for (Map.Entry<String, Integer> e : currentMetaData.frequencies.entrySet()) {
//                    writer.append(e.getKey() + " " + e.getValue() + ";");
//                }
//                writer.append("\n");
//                for (String s : currentMetaData.childLinks) {
//                    writer.append(s + "\n");
//                }
//                writer.append("=====================================================\n\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

    }
}
