package indexer;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Map;

public class Indexer {
    public static RocksDB index;

    {
        RocksDB.loadLibrary();
        Options options = new Options();
        String dbPath = "rocksdb";
        options.setCreateIfMissing(true);

        try {
            index = RocksDB.open(options, dbPath);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void updateIndex(int docId, HashMap<String, Integer> frequencies){
        try {
            for(Map.Entry<String, Integer> entry : frequencies.entrySet()){
                byte[] value = index.get(entry.getKey().getBytes());
                String newPosting = String.valueOf(docId) + " " + entry.getValue() + ";  ";
                // case where term has not been indexed before
                if(value == null){
                    index.put(entry.getKey().getBytes(), newPosting.getBytes());
                    continue;
                }
                // term has already been indexed
                // append to the posting list
                var currentPostList = new String(value);
                currentPostList += newPosting;

                index.put(entry.getKey().getBytes(), currentPostList.getBytes());
            }

        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void printIndex(){
        var iter = index.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next() ){
            System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
        }
    }


}
