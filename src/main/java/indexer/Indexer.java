package indexer;

import model.MetaData;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Map;

public class Indexer {
    public static RocksDB invertedIndexDb;
    public static RocksDB metaInfoDb;
    private static String INVERTED_INDEX_PATH = "InvertedIndexDb";
    private static String META_INTO_PATH = "MetaInfoDb";

    {
        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);

        try {
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
                String newPosting = String.valueOf(docId) + " " + entry.getValue() + ";  ";
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
        var iter = metaInfoDb.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next() ){
            System.out.println(new String(iter.key()) + ": \n" + MetaData.deserialize(iter.value()) );
        }
    }

    public void addMetaInformation(int docId, MetaData data){
        try {
            metaInfoDb.put(String.valueOf(docId).getBytes(), MetaData.convertToByteArray(data));
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }




}
