package crawler;

import model.PageInfo;
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
            page_info = RocksDB.open(options, dbPath_Page_Info);
        } catch (RocksDBException e) {
//            e.printStackTrace();
        }
    }

    public static void write_spider_result() {
        try (var writer = new BufferedWriter(new FileWriter("spider_result.txt"))) {
            writer.write("");
            var map = Repository.Page.getMap_PageId_Url();
            for(Map.Entry<String, String> entry : map.entrySet()){
                var pageInfo = Repository.PageInfo.getPageInfo(entry.getKey());

                writer.append(pageInfo.pgTitle).append(String.valueOf('\n'));
                writer.append(new String(entry.getValue())).append(String.valueOf('\n'));
                writer.append(pageInfo.lastModifiedDate).append(", ")
                        .append(pageInfo.pageSize).append('\n');

                var wordPosting = Repository.ForwardFrequency
                        .getMap_WordId_Freq(entry.getKey());

//                System.out.println(wordPosting);

                for(var e : wordPosting.entrySet()){
//                    System.out.println(e);
                    String termId = e.getKey();
                    writer.append(Repository.Word.getWord(termId) + " " + e.getValue() + ";");
                }
                writer.append('\n');

                for(var link: pageInfo.childLinks){
                    writer.append(link).append('\n');
                }

                writer.append("---------------------------------------------").append("\n\n");

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RocksDBException e) {
            e.printStackTrace();
        }

    }
}
