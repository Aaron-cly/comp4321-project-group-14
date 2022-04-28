package repository;

import model.SerializeUtil;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.*;

public class Repository {
    protected static RocksDB WordToWordId;
    protected static RocksDB WordIdToWord;
    protected static RocksDB PageToPageId;
    protected static RocksDB PageIdToPage;
    protected static RocksDB forwardDB;
    protected static RocksDB invertedDB;
    protected static RocksDB pageInfoDB;
    protected static RocksDB forwardTitleDB;
    protected static RocksDB invertedTitleDB;

    static List<String> dbPathList = List.of(Word.dbPath_1, Word.dbPath_2, Page.dbPath_1, Page.dbPath_2, ForwardIndex.dbPath, InvertedIndex.dbPath, ForwardIndex_Title.dbPath, InvertedIndex_Title.dbPath, PageInfo.dbPath);

    static {
        RocksDB.loadLibrary();
    }

    public static void destroyAll() {
        Options options = new Options();
        try {
            // drop all data database first to ensure fresh run
            for (String dbPath : dbPathList) {
                RocksDB.destroyDB(dbPath, options);
            }
        } catch (RocksDBException e) {
        }
    }

    public static void openConnections() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            WordToWordId = RocksDB.open(options, Word.dbPath_1);
            WordIdToWord = RocksDB.open(options, Word.dbPath_2);
            PageToPageId = RocksDB.open(options, Page.dbPath_1);
            PageIdToPage = RocksDB.open(options, Page.dbPath_2);
            forwardDB = RocksDB.open(options, ForwardIndex.dbPath);
            invertedDB = RocksDB.open(options, InvertedIndex.dbPath);
            pageInfoDB = RocksDB.open(options, PageInfo.dbPath);
            forwardTitleDB = RocksDB.open(options, ForwardIndex_Title.dbPath);
            invertedTitleDB = RocksDB.open(options, InvertedIndex_Title.dbPath);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeAllConnections() {
        WordToWordId.close();
        PageToPageId.close();
        forwardDB.close();
        invertedDB.close();
        pageInfoDB.close();
        forwardTitleDB.close();
        invertedTitleDB.close();
    }

    public static class Word {
        private static String dbPath_1 = "./rocksdb/WordToWordId";
        private static String dbPath_2 = "./rocksdb/WordIdToWord";

        public static String getWord(String wordId) throws RocksDBException {
            byte[] value = null;
            try {
                value = WordIdToWord.get(wordId.getBytes());
            } catch (RocksDBException e) {
            }

            // return word of given wordId
            return value == null ? null : new String(value);
        }

        public static String insertWord(String word) throws RocksDBException {
            // TODO
            String existingId = getWordId(word);

            if (existingId != null) {
                return existingId;
            }

            String wordId = String.valueOf(word.hashCode());
            // insert new word to db
            WordToWordId.put(word.getBytes(), wordId.getBytes());
            WordIdToWord.put(wordId.getBytes(), word.getBytes());
            // return the wordId of the inserted word
            return wordId;
        }

        public static String getWordId(String word) {
            byte[] value = null;
            try {
                value = WordToWordId.get(word.getBytes());
            } catch (RocksDBException e) {
            }

            // return word of given wordId
            return value == null ? null : new String(value);
        }

    }

    public static class Page {
        // url -> pageId
        private static String dbPath_1 = "./rocksdb/PageToPageId";
        // pageId -> url
        private static String dbPath_2 = "./rocksdb/PageIdToPage";


        public static int getTotalNumPage() {
            int count = 0;
            RocksIterator iter = PageToPageId.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                count++;
            }
            return count;
        }

        public static String getPageUrl(String pageId) throws RocksDBException {
            //return url of the given pageId
            byte[] dataBytes = null;
            try {
                dataBytes = PageIdToPage.get(pageId.getBytes());
            } catch (RocksDBException e) {
            }

            if (dataBytes == null)
                return null;
            else {
                return new String(dataBytes);
            }
        }

        public static String getPageId(String url) {
            //return pageId of the given url
            byte[] dataBytes = null;
            try {
                dataBytes = PageToPageId.get(url.getBytes());
            } catch (RocksDBException e) {
            }

            if (dataBytes == null)
                return null;
            else {
                return new String(dataBytes);
            }
        }

        public static String insertPage(String url) throws RocksDBException {
            String existingId = getPageId(url);

            if (existingId != null) {
                return existingId;
            }

            String pageId = String.valueOf(url.hashCode());


            PageToPageId.put(url.getBytes(), pageId.getBytes());
            PageIdToPage.put(pageId.getBytes(), url.getBytes());

//            System.out.println("After Inserting: " + indexed + ", " + getPage(url));

            // return the pageId of the inserted page
            return pageId;
        }

        public static HashMap<String, String> getMap_url_pageId() {
            var map = new HashMap<String, String>();

            RocksIterator iter = PageToPageId.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                map.put(new String(iter.key()), new String(iter.value()));
            }
            return map;
        }
    }

    public static class ForwardIndex {
        private static String dbPath = "./rocksdb/Forward_Index";
        // pageId -> HashMap(wordId -> freq)

        // check if a page is in Forward frequency as a key
        private static boolean pageIn_ForwardIndex(String pageId) {
            boolean isIn;
            try {
                forwardDB.get(pageId.getBytes());
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
            forwardDB.put(pageId.getBytes(), dataBytes);
        }

        // get the HashMap of word positions for a given page
        public static HashMap<String, List<Integer>> getMap_WordId_Positions(String pageId) throws RocksDBException {
            if (!pageIn_ForwardIndex(pageId)) {
                return new HashMap<>();
            }

            HashMap<String, List<Integer>> map_wordId_positions = null;
            try {
                map_wordId_positions = SerializeUtil.deserialize(forwardDB.get(pageId.getBytes()));
            } catch (RocksDBException e) {
                throw new RocksDBException(e.getMessage());
            }
            return map_wordId_positions;
        }

        public static HashMap<String, HashMap<String, List<Integer>>> getAll_ForwardIndex() {
            var forward = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = forwardDB.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_wordId_positions = SerializeUtil.deserialize(iter.value());

                forward.put(wordId, map_wordId_positions);
            }
            return forward;
        }

        public static void print() {
            RocksIterator iter = forwardDB.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
            }
        }
    }

    public static class InvertedIndex {
        private static String dbPath = "./rocksdb/Inverted_Index";

        // check if a word is in Inverted Frequency as a key
        public static boolean wordIn_InvertedFrequency(String wordId) {
            boolean isIn;
            try {
                invertedDB.get(wordId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        // wordId :String -> HashMap(pageId -> List(position :Integer))
        public static void create_InvertedIndexFile(HashMap<String, HashMap<String, List<Integer>>> inverted) {
            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : inverted.entrySet()) {
                var wordId = entry.getKey();
                var map_pageId_posList = entry.getValue();
                var dataBytes = SerializeUtil.serialize(map_pageId_posList);

                try {
                    invertedDB.put(wordId.getBytes(), dataBytes);
                } catch (RocksDBException e) {
                    System.out.println("Error putting inverted index file to db");
                    e.printStackTrace();
                }
            }
        }

        public static HashMap<String, HashMap<String, List<Integer>>> getAll_InvertedIndex() {
            var inverted = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = invertedDB.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_pageId_posList = SerializeUtil.deserialize(iter.value());

                inverted.put(wordId, map_pageId_posList);
            }
            return inverted;
        }

        public static HashMap<String, List<Integer>> getMap_pageId_wordPosList(String wordId) {
            byte[] databyte = null;

            try {
                databyte = invertedDB.get(wordId.getBytes());
            } catch (RocksDBException e) {
            }
            if (databyte == null) return new HashMap<>();

            return SerializeUtil.deserialize(databyte);
        }

    }

    public static class ForwardIndex_Title {
        private static String dbPath = "./rocksdb/Forward_Index_Title";
        // pageId -> HashMap(wordId -> freq)

        // check if a page is in Forward index as a key
        private static boolean pageIn_ForwardIndex(String pageId) {
            boolean isIn;
            try {
                forwardTitleDB.get(pageId.getBytes());
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
            forwardTitleDB.put(pageId.getBytes(), dataBytes);
        }

        // get the HashMap of word positions for a given page
        public static HashMap<String, List<Integer>> getMap_WordId_Positions(String pageId) throws RocksDBException {
            if (!pageIn_ForwardIndex(pageId)) {
                return new HashMap<>();
            }

            HashMap<String, List<Integer>> map_wordId_positions = null;
            try {
                map_wordId_positions = SerializeUtil.deserialize(forwardTitleDB.get(pageId.getBytes()));
            } catch (RocksDBException e) {
                throw new RocksDBException(e.getMessage());
            }
            return map_wordId_positions;
        }

        public static HashMap<String, HashMap<String, List<Integer>>> getAll_ForwardIndex() {
            var forward = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = forwardTitleDB.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_wordId_positions = SerializeUtil.deserialize(iter.value());

                forward.put(wordId, map_wordId_positions);
            }
            return forward;
        }

        public static void print() {
            RocksIterator iter = forwardTitleDB.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
            }
        }
    }

    public static class InvertedIndex_Title {
        private static String dbPath = "./rocksdb/Inverted_Index_Title";

        // check if a word is in Inverted Frequency as a key
        public static boolean wordIn_InvertedFrequency(String wordId) {
            boolean isIn;
            try {
                invertedTitleDB.get(wordId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        // wordId :String -> HashMap(pageId -> List(position :Integer))
        public static void create_InvertedIndexFile(HashMap<String, HashMap<String, List<Integer>>> inverted) {
            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : inverted.entrySet()) {
                var wordId = entry.getKey();
                var map_pageId_posList = entry.getValue();
                var dataBytes = SerializeUtil.serialize(map_pageId_posList);

                try {
                    invertedTitleDB.put(wordId.getBytes(), dataBytes);
                } catch (RocksDBException e) {
                    System.out.println("Error putting inverted index file to db");
                    e.printStackTrace();
                }
            }
        }

        public static HashMap<String, HashMap<String, List<Integer>>> getAll_InvertedIndex() {
            var inverted = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = invertedTitleDB.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_pageId_posList = SerializeUtil.deserialize(iter.value());

                inverted.put(wordId, map_pageId_posList);
            }
            return inverted;
        }

        public static HashMap<String, List<Integer>> getMap_pageId_wordPosList(String wordId) {
            byte[] databyte = null;

            try {
                databyte = invertedTitleDB.get(wordId.getBytes());
            } catch (RocksDBException | NullPointerException e) {
            }
            if (databyte == null) return new HashMap<>();

            return SerializeUtil.deserialize(databyte);
        }

    }

    public static class PageInfo {
        private static String dbPath = "./rocksdb/Page_Info";

        public static void addPageInfo(String pageId, model.PageInfo data) {
            try {
                pageInfoDB.put(pageId.getBytes(), SerializeUtil.serialize(data));
            } catch (RocksDBException e) {
                System.out.println("Could not save meta information of page");
                e.printStackTrace();
            }
        }

        public static model.PageInfo getPageInfo(String pageId) {
            model.PageInfo pageInfo = null;
            try {
                var bytes = pageInfoDB.get(pageId.getBytes());
                pageInfo = SerializeUtil.deserialize(bytes);
            } catch (Exception e) {
                System.out.println("Error getting PageInfo for pageId " + pageId);
                e.printStackTrace();
            }

            return pageInfo;
        }

        public static HashMap<String, model.PageInfo> getMap_pageId_pageInfo() {
            var map = new HashMap<String, model.PageInfo>();

            RocksIterator iter = pageInfoDB.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                model.PageInfo pageInfo = SerializeUtil.deserialize(iter.value());
                map.put(new String(iter.key()), pageInfo);
            }
            return map;
        }
    }
}
