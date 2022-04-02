package repository;

import java.util.*;

import model.SerializeUtil;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class Repository {
    public static class Word {
        private static String dbPath = "./rocksdb/Word";
        protected static RocksDB word_table;
        // word -> wordId

        static {
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                // drop all data database first to ensure fresh run
                RocksDB.destroyDB(dbPath, options);
                word_table = RocksDB.open(options, dbPath);
            } catch (RocksDBException e) {
            }
        }

        public static String getWord(String wordId) throws RocksDBException {
            RocksIterator iter = word_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String entry_wordId = new String(iter.value());
                if (entry_wordId.equals(wordId)) {
                    // return the word of the given wordId
                    return new String(iter.key());
                }
            }
            return null;
        }

        public static String insertWord(String word) throws RocksDBException {
            // TODO
            String existingId = getWordId(word);

            if (existingId != null) {
                return existingId;
            }

            String wordId = String.valueOf(word.hashCode());
            // insert new word to db
            word_table.put(word.getBytes(), wordId.getBytes());
            // return the wordId of the inserted word
            return wordId;
        }

        public static String getWordId(String word) {
            byte[] value = null;
            try {
                value = word_table.get(word.getBytes());
            } catch (RocksDBException e) {
            }

            // return word of given wordId
            return value == null ? null : new String(value);
        }

    }

    public static class Page {
        private static String dbPath = "./rocksdb/Page";
        protected static RocksDB page_table;
        // url -> pageId

        static {
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                // drop all data database first to ensure fresh run
                RocksDB.destroyDB(dbPath, options);
                page_table = RocksDB.open(options, dbPath);
            } catch (RocksDBException e) {
            }
        }

        public static String getPageUrl(String pageId) throws RocksDBException {
            RocksIterator iter = page_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String entry_word = new String(iter.value());
                if (entry_word.equals(pageId)) {
                    // return the wordId of the given word
                    return new String(iter.key());
                }
            }
            return null;
        }

        public static String insertPage(String url) throws RocksDBException {
            String existingId = getPageId(url);

            if (existingId != null) {
                return existingId;
            }

            String pageId = String.valueOf(url.hashCode());
            page_table.put(url.getBytes(), pageId.getBytes());

            // return the pageId of the inserted page
            return pageId;
        }

        public static String getPageId(String url) {
            //return pageId of the given url
            byte[] value = null;
            try {
                value = page_table.get(url.getBytes());
            } catch (RocksDBException e) {
            }

            // return pageId of given url
            return value == null ? null : new String(value);
        }

        public static HashMap<String, String> getMap_url_pageId() {
            var map = new HashMap<String, String>();

            RocksIterator iter = page_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                map.put(new String(iter.key()), new String(iter.value()));
            }
            return map;
        }

    }

    public static class ForwardIndex {
        private static String dbPath = "./rocksdb/Forward_Index";
        private static RocksDB forward_index_table;
        // pageId -> HashMap(wordId -> freq)

        static {
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                // drop all data database first to ensure fresh run
                RocksDB.destroyDB(dbPath, options);
                forward_index_table = RocksDB.open(options, dbPath);
            } catch (RocksDBException e) {
            }
        }

        // check if a page is in Forward frequency as a key
        private static boolean pageIn_ForwardIndex(String pageId) {
            boolean isIn;
            try {
                forward_index_table.get(pageId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        public static void updateUrl_wordPositions(String url, HashMap<String, List<Integer>> wordPositions) throws RocksDBException {
            String pageId = null;
            try {
                pageId = Page.insertPage(url);
            } catch (RocksDBException e) {
            }
            if (pageId == null) {
                return;
            }

            updatePage_wordPositions(pageId, wordPositions);
        }

        // input wordPositions: wordId -> List(positions)
        public static void updatePage_wordPositions(String pageId, HashMap<String, List<Integer>> wordPositions) throws RocksDBException {
            byte[] dataBytes = SerializeUtil.serialize(wordPositions);
            forward_index_table.put(pageId.getBytes(), dataBytes);
        }

        // get the HashMap of word positions for a given page
        public static HashMap<String, List<Integer>> getMap_WordId_Positions(String pageId) throws RocksDBException {
            if (!pageIn_ForwardIndex(pageId)) {
                return new HashMap<>();
            }

            HashMap<String, List<Integer>> map_wordId_positions = null;
            try {
                map_wordId_positions = SerializeUtil.deserialize(forward_index_table.get(pageId.getBytes()));
            } catch (RocksDBException e) {
                throw new RocksDBException(e.getMessage());
            }
            return map_wordId_positions;
        }

        public static HashMap<String, HashMap<String, List<Integer>>> getAll_ForwardIndex() {
            var forward = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = forward_index_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_wordId_positions = SerializeUtil.deserialize(iter.value());

                forward.put(wordId, map_wordId_positions);
            }
            return forward;
        }

        public static void print() {
            RocksIterator iter = forward_index_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
            }
        }
    }

    public static class InvertedIndex {
        private static String dbPath = "./rocksdb/Inverted_Index";
        private static RocksDB inverted_index_table;

        static {
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                // drop all data database first to ensure fresh run
                RocksDB.destroyDB(dbPath, options);
                inverted_index_table = RocksDB.open(options, dbPath);
            } catch (RocksDBException e) {
            }
        }

        // check if a word is in Inverted Frequency as a key
        public static boolean wordIn_InvertedFrequency(String wordId) {
            boolean isIn;
            try {
                inverted_index_table.get(wordId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        // wordId :String -> HashMap(pageId -> List(position :Integer))
        public static void insert_InvertedIndexFile(HashMap<String, HashMap<String, List<Integer>>> inverted) {
            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : inverted.entrySet()) {
                var wordId = entry.getKey();
                var map_pageId_posList = entry.getValue();
                var dataBytes = SerializeUtil.serialize(map_pageId_posList);

                try {
                    inverted_index_table.put(wordId.getBytes(), dataBytes);
                } catch (RocksDBException e) {
                    System.out.println("Error putting inverted index file to db");
                    e.printStackTrace();
                }
            }
        }

        public static HashMap<String, HashMap<String, List<Integer>>> getAll_InvertedIndex() {
            var inverted = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = inverted_index_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_pageId_posList = SerializeUtil.deserialize(iter.value());

                inverted.put(wordId, map_pageId_posList);
            }
            return inverted;
        }

    }

    public static class PageInfo {
        private static String dbPath = "./rocksdb/Page_Info";
        private static RocksDB pageInfoDb;

        static {
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                // drop all data database first to ensure fresh run
                RocksDB.destroyDB(dbPath, options);
                pageInfoDb = RocksDB.open(options, dbPath);
            } catch (RocksDBException e) {
            }
        }

        public static void addPageInfo(String pageId, model.PageInfo data) {
            try {
                pageInfoDb.put(pageId.getBytes(), model.PageInfo.convertToByteArray(data));
            } catch (RocksDBException e) {
                System.out.println("Could not save meta information of page");
                e.printStackTrace();
            }
        }

        public static model.PageInfo getPageInfo(String pageId) throws RocksDBException {
            model.PageInfo pageInfo = null;
            try {
                var bytes = pageInfoDb.get(pageId.getBytes());
                pageInfo = model.PageInfo.deserialize(bytes);
            } catch (Exception e) {
            }

            return pageInfo;
        }
    }
}
