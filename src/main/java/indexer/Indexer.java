package indexer;

import model.MetaData;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Indexer {
    public static RocksDB invertedIndexDb;
    public static RocksDB metaInfoDb;
    private static String INVERTED_INDEX_PATH = "InvertedIndexDb";
    private static String META_INTO_PATH = "MetaInfoDb";
    private static String DELIMITER = ";";
    private static String SEPARATOR = "::";

    {
        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);

        try {
            // drop all data database first to ensure fresh run
            RocksDB.destroyDB(INVERTED_INDEX_PATH, options);
            RocksDB.destroyDB(META_INTO_PATH, options);
            invertedIndexDb = RocksDB.open(options, INVERTED_INDEX_PATH);
            metaInfoDb = RocksDB.open(options, META_INTO_PATH);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void updateIndex(int docId, HashMap<String, Integer> frequencies){
        try {
            for(Map.Entry<String, Integer> entry : frequencies.entrySet()){
                byte[] value = invertedIndexDb.get(entry.getKey().getBytes());
                String newPosting = String.valueOf(docId) + SEPARATOR + entry.getValue() + DELIMITER;
                // case where term has not been indexed before
                if(value == null){
                    invertedIndexDb.put(entry.getKey().getBytes(), newPosting.getBytes());
                    continue;
                }
                // term has already been indexed
                // append to the posting list
                var currentPostList = new String(value);
                currentPostList += newPosting;

                invertedIndexDb.put(entry.getKey().getBytes(), currentPostList.getBytes());
            }

        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void printInvertedIndex(){
        var iter = invertedIndexDb.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next() ){
            System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
        }
    }

    public void printMetaInfo(){
        String directory = Paths.get("").toAbsolutePath().toString();
        try {
            var writer = new BufferedWriter(new FileWriter("spider_result.txt"));
            writer.write("");
            var iter = metaInfoDb.newIterator();
            for(iter.seekToFirst(); iter.isValid(); iter.next() ){
                var currentMetaData = MetaData.deserialize(iter.value());
                System.out.println(new String(iter.key()) + ": \n" + currentMetaData );
                writer.append(currentMetaData.pgTitle + "\n");
                writer.append(currentMetaData.url + "\n");
                writer.append(currentMetaData.lastModifiedDate.toString() + "\n");
                for(Map.Entry<String, Integer> e : currentMetaData.frequencies.entrySet()){
                    writer.append(e.getKey() + " " + e.getValue() + ";");
                }
                writer.append("\n");
                for(String s: currentMetaData.childLinks){
                    writer.append(s + "\n");
                }
                writer.append("=====================================================\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public void addMetaInformation(int docId, MetaData data){
        try {
            metaInfoDb.put(String.valueOf(docId).getBytes(), MetaData.convertToByteArray(data));
        } catch (RocksDBException e) {
            System.out.println("Could not save meta information of page");
            e.printStackTrace();
        }
    }

//    public void retrieveKeywords(int docId){
//        var iter = invertedIndexDb.newIterator();
//        var wordMap = new HashMap<String, Integer>();
//        for(iter.seekToFirst(); iter.isValid(); iter.next()){
//            var currentPosting = new String(iter.value());
//            var individualPostings = currentPosting.split(DELIMITER);
//            for(String s: individualPostings){
//                var element = s.split(SEPARATOR);
//                wordMap.put(new String(iter.key()), element)
//            }
//
//        }
//    }




}
