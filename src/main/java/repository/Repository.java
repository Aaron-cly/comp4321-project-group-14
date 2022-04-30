package repository;

import model.SerializeUtil;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.*;

/** Repository Class for read-write operations to the database */
public class Repository {
    protected static Database WordToWordId = new Database("WordToWordID");
    protected static Database WordIdToWord = new Database("WordIdToWord");
    protected static Database PageToPageId = new Database("PageToPageId");
    protected static Database PageIdToPage = new Database("PageIdToPage");
    protected static Database Forward = new Database("Forward");
    protected static Database Inverted = new Database("Inverted");
    protected static Database PageInfo = new Database("PageInfo");
    protected static Database FowardTitle = new Database("ForwardTitle");
    protected static Database InvertedTitle = new Database("InvertedTitle");

    static List<Database> dbList;

    static {
        RocksDB.loadLibrary();
        dbList = List.of(WordToWordId, WordIdToWord, PageToPageId, PageIdToPage, Forward, Inverted, PageInfo, FowardTitle, InvertedTitle);
    }

    /** Destroys/drops all the databases */
    public static void destroyAll() {
        Options options = new Options();
        try {
            for (var DB : dbList) {
                RocksDB.destroyDB(DB.getPath(), options);
            }
        } catch (RocksDBException e) {
        }
    }

    /** Opens all connections of all the databases */
    public static void openConnections() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            for (var DB : dbList) {
                DB.openConnection();
            }
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    /** Closes all connections of all the databases */
    public static void closeAllConnections() {
        for (var DB : dbList) {
            DB.closeConnection();
        }
    }

    /** Word Repository for the word databases, i.e. WordToWordId and WordIdToWord */
    public static class Word {
        /** Get the words from its wordId.
         *
         * @param wordId  The target wordId of the word to be retrieved
         * @return  The word of the target wordId
         * @throws RocksDBException
         */
        public static String getWord(String wordId) throws RocksDBException {
            byte[] value = null;
            try {
                value = WordIdToWord.getDB().get(wordId.getBytes());
            } catch (RocksDBException e) {
            }

            // return word of given wordId
            return value == null ? null : new String(value);
        }

        /** inserts a word to the word databases.
         *
         * @param word The word to be inserted
         * @return  The wordId of the inserted word
         * @throws RocksDBException
         */
        public static String insertWord(String word) throws RocksDBException {
            // TODO
            String existingId = getWordId(word);

            if (existingId != null) {
                return existingId;
            }

            String wordId = String.valueOf(word.hashCode());
            // insert new word to db
            WordToWordId.getDB().put(word.getBytes(), wordId.getBytes());
            WordIdToWord.getDB().put(wordId.getBytes(), word.getBytes());
            // return the wordId of the inserted word
            return wordId;
        }

        /** Gets the wordId of the word
         *
         * @param word  The target word, from which id is to be retrieved
         * @return  The wordId of the target word
         */
        public static String getWordId(String word) {
            byte[] value = null;
            try {
                value = WordToWordId.getDB().get(word.getBytes());
            } catch (RocksDBException e) {
            }

            // return word of given wordId
            return value == null ? null : new String(value);
        }

    }

    /** Page Repository for the word databases, i.e. PageToPageId and PageIdToPage */
    public static class Page {
        /**
         *
         * @return The total number of pages from the database
         */
        public static int getTotalNumPage() {
            int count = 0;
            RocksIterator iter = PageToPageId.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                count++;
            }
            return count;
        }

        /** Gets the page url from the pageId
         *
         * @param pageId  The pageId, from which url is to be retrieved
         * @return  The url of the pageId
         * @throws RocksDBException
         */
        public static String getPageUrl(String pageId) throws RocksDBException {
            //return url of the given pageId
            byte[] dataBytes = null;
            try {
                dataBytes = PageIdToPage.getDB().get(pageId.getBytes());
            } catch (RocksDBException e) {
            }

            if (dataBytes == null)
                return null;
            else {
                return new String(dataBytes);
            }
        }

        /**
         * Gets the pageId from its url
         * @param url  The url, from which the pageId is to be retrieved
         * @return  The pageId from its url
         */
        public static String getPageId(String url) {
            //return pageId of the given url
            byte[] dataBytes = null;
            try {
                dataBytes = PageToPageId.getDB().get(url.getBytes());
            } catch (RocksDBException e) {
            }

            if (dataBytes == null)
                return null;
            else {
                return new String(dataBytes);
            }
        }

        /** Inserts a page to the databases.
         *
         * @param url The url of the page
         * @return  The pageId
         * @throws RocksDBException
         */
        public static String insertPage(String url) throws RocksDBException {
            String existingId = getPageId(url);

            if (existingId != null) {
                return existingId;
            }

            String pageId = String.valueOf(url.hashCode());


            PageToPageId.getDB().put(url.getBytes(), pageId.getBytes());
            PageIdToPage.getDB().put(pageId.getBytes(), url.getBytes());

            // return the pageId of the inserted page
            return pageId;
        }

        /** Gets the mapping of all urls to its pageIds from the database
         *
         * @return  The mapping of urls to its pageIds
         */
        public static HashMap<String, String> getMap_url_pageId() {
            var map = new HashMap<String, String>();

            RocksIterator iter = PageToPageId.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                map.put(new String(iter.key()), new String(iter.value()));
            }
            return map;
        }
    }

    /**
     * Forward Index repository for the forward index database.
     */
    public static class ForwardIndex {
        // check if a page is in Forward frequency as a key
        private static boolean pageIn_ForwardIndex(String pageId) {
            boolean isIn;
            try {
                Forward.getDB().get(pageId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        /** Update the word positions of the page from its url
         *
         * @param url  The url of the page
         * @param wordPositions  The new word positions of the page
         * @throws RocksDBException
         */
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

        /** Updates the word positions of a page from its id
         *
         * @param pageId  The pageId of the page
         * @param wordPositions  The new word Positions
         * @throws RocksDBException
         */
        public static void updatePage_wordPositions(String pageId, HashMap<String, List<Integer>> wordPositions) throws RocksDBException {
            byte[] dataBytes = SerializeUtil.serialize(wordPositions);
            Forward.getDB().put(pageId.getBytes(), dataBytes);
        }

        /**
         * Returns a page's wordIds to its word positions, from the page Id, from the database
         * @param pageId  The pageId to the page
         * @return  The mapping of the wordIds to the word positions, from the pageId
         * @throws RocksDBException
         */
        public static HashMap<String, List<Integer>> getMap_WordId_Positions(String pageId) throws RocksDBException {
            if (!pageIn_ForwardIndex(pageId)) {
                return new HashMap<>();
            }

            HashMap<String, List<Integer>> map_wordId_positions = null;
            try {
                map_wordId_positions = SerializeUtil.deserialize(Forward.getDB().get(pageId.getBytes()));
            } catch (RocksDBException e) {
                throw new RocksDBException(e.getMessage());
            }
            return map_wordId_positions;
        }

        /**
         * Gets all the mapping of wordIds to wordPositions for all pages from the database
         * @return the mapping of wordIds to wordPositions for all pages
         */
        public static HashMap<String, HashMap<String, List<Integer>>> getAll_ForwardIndex() {
            var forward = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = Forward.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_wordId_positions = SerializeUtil.deserialize(iter.value());

                forward.put(wordId, map_wordId_positions);
            }
            return forward;
        }

        public static void print() {
            RocksIterator iter = Forward.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
            }
        }
    }

    public static class InvertedIndex {
        private static String dbPath = "./rocksdb/Inverted_Index";

        /** Checks if a word is already in the database
         *
         * @param wordId  The wordId of the word
         * @return  A boolean value indicating whether the word is already in the database
         */
        public static boolean wordIn_InvertedFrequency(String wordId) {
            boolean isIn;
            try {
                Inverted.getDB().get(wordId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        /** Creates an InvertedIndex from a hashmap representation of the inverted index and saves it into the database
         *
         * @param inverted The hashmap representation of the inverted index
         */
        public static void create_InvertedIndexFile(HashMap<String, HashMap<String, List<Integer>>> inverted) {
            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : inverted.entrySet()) {
                var wordId = entry.getKey();
                var map_pageId_posList = entry.getValue();
                var dataBytes = SerializeUtil.serialize(map_pageId_posList);

                try {
                    Inverted.getDB().put(wordId.getBytes(), dataBytes);
                } catch (RocksDBException e) {
                    System.out.println("Error putting inverted index file to db");
                    e.printStackTrace();
                }
            }
        }

        /** Return the hashmap representation of the invertedIndex from the database
         *
         * @return hashmap representation of the invertedIndex
         */
        public static HashMap<String, HashMap<String, List<Integer>>> getAll_InvertedIndex() {
            var inverted = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = Inverted.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_pageId_posList = SerializeUtil.deserialize(iter.value());

                inverted.put(wordId, map_pageId_posList);
            }
            return inverted;
        }

        /** Gets the mapping from pageId to its word position for a given word
         *
         * @param wordId  The wordId of the word
         * @return  mapping from pageId to wordPosition of the given word parameter
         */
        public static HashMap<String, List<Integer>> getMap_pageId_wordPosList(String wordId) {
            byte[] databyte = null;

            try {
                databyte = Inverted.getDB().get(wordId.getBytes());
            } catch (RocksDBException | NullPointerException e) {
            }
            if (databyte == null) return new HashMap<>();

            return SerializeUtil.deserialize(databyte);
        }

    }

    /** ForwardIndex Repository for title */
    public static class ForwardIndex_Title {
        /** Checks if a page is already in the database
         *
         * @param pageId  The pageId of the page
         * @return  A boolean value indicating whether the page is already in the database
         */
        private static boolean pageIn_ForwardIndex(String pageId) {
            boolean isIn;
            try {
                FowardTitle.getDB().get(pageId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        /** Updates the wordPositions of the title of a page from its url
         *
         * @param url  The url of the page
         * @param wordPositions  The new wordPositions
         * @throws RocksDBException
         */
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

        /** Updates the word positions of a page
         *
         * @param pageId  The pageId of a page
         * @param wordPositions  The new word positions
         * @throws RocksDBException
         */
        public static void updatePage_wordPositions(String pageId, HashMap<String, List<Integer>> wordPositions) throws RocksDBException {
            byte[] dataBytes = SerializeUtil.serialize(wordPositions);
            FowardTitle.getDB().put(pageId.getBytes(), dataBytes);
        }

        /** Gets the mapping from wordId to its position for a given page
         *
         * @param pageId  The pageId of the page
         * @return  mapping from wordId to its position of the given page parameter
         */
        public static HashMap<String, List<Integer>> getMap_WordId_Positions(String pageId) throws RocksDBException {
            if (!pageIn_ForwardIndex(pageId)) {
                return new HashMap<>();
            }

            HashMap<String, List<Integer>> map_wordId_positions = null;
            try {
                map_wordId_positions = SerializeUtil.deserialize(FowardTitle.getDB().get(pageId.getBytes()));
            } catch (RocksDBException e) {
                throw new RocksDBException(e.getMessage());
            }
            return map_wordId_positions;
        }

        /** Gets all the mapping of the forwardIndex title from the database
         *
         * @return mapping from the pageId to the words the page contains
         */
        public static HashMap<String, HashMap<String, List<Integer>>> getAll_ForwardIndex() {
            var forward = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = FowardTitle.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_wordId_positions = SerializeUtil.deserialize(iter.value());

                forward.put(wordId, map_wordId_positions);
            }
            return forward;
        }

        public static void print() {
            RocksIterator iter = FowardTitle.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
            }
        }
    }

    /** InvetedIndex Repository for title */
    public static class InvertedIndex_Title {
        private static String dbPath = "./rocksdb/Inverted_Index_Title";

        /**
         * Checks if a word is in the inverted Index title
         * @param wordId  The wordId of the word
         * @return  A boolean value indicating whether the word is in the invertedIndex
         */
        public static boolean wordIn_InvertedFrequency(String wordId) {
            boolean isIn;
            try {
                InvertedTitle.getDB().get(wordId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        // wordId :String -> HashMap(pageId -> List(position :Integer))

        /** Creates an invertedIndex title from a hashmap representation of the invertedIndex title, and saves
         * it into the database
         *
         * @param inverted  Hashmap represenation of the inverted Index title
         */
        public static void create_InvertedIndexFile(HashMap<String, HashMap<String, List<Integer>>> inverted) {
            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : inverted.entrySet()) {
                var wordId = entry.getKey();
                var map_pageId_posList = entry.getValue();
                var dataBytes = SerializeUtil.serialize(map_pageId_posList);

                try {
                    InvertedTitle.getDB().put(wordId.getBytes(), dataBytes);
                } catch (RocksDBException e) {
                    System.out.println("Error putting inverted index file to db");
                    e.printStackTrace();
                }
            }
        }

        /** Gets the inverted index from the database and returns it in a hashmap representation
         *
         * @return  Hashmap represenation of the inverted index
         */
        public static HashMap<String, HashMap<String, List<Integer>>> getAll_InvertedIndex() {
            var inverted = new HashMap<String, HashMap<String, List<Integer>>>();
            RocksIterator iter = InvertedTitle.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String wordId = new String(iter.key());
                HashMap<String, List<Integer>> map_pageId_posList = SerializeUtil.deserialize(iter.value());

                inverted.put(wordId, map_pageId_posList);
            }
            return inverted;
        }

        /** Gets the mapping of pageId to wordPosition of a given word
         *
         * @param wordId  The wordId of the word
         * @return  The mapping of pageId to the wordPositions for the given wordId parameter
         */
        public static HashMap<String, List<Integer>> getMap_pageId_wordPosList(String wordId) {
            byte[] databyte = null;

            try {
                databyte = InvertedTitle.getDB().get(wordId.getBytes());
            } catch (RocksDBException | NullPointerException e) {
            }
            if (databyte == null) return new HashMap<>();

            return SerializeUtil.deserialize(databyte);
        }

    }

    /** PageInfo Repository */
    public static class PageInfo {
        private static String dbPath = "./rocksdb/Page_Info";

        /** Adds a new {@link model.PageInfo} for a page
         *
         * @param pageId  The pageId of the page
         * @param data  The data to be inserted into the database
         */
        public static void addPageInfo(String pageId, model.PageInfo data) {
            try {
                PageInfo.getDB().put(pageId.getBytes(), SerializeUtil.serialize(data));
            } catch (RocksDBException e) {
                System.out.println("Could not save meta information of page");
                e.printStackTrace();
            }
        }

        /** Gets the {@link model.PageInfo} of a page
         *
         * @param pageId  The pageId of the page
         * @return
         */
        public static model.PageInfo getPageInfo(String pageId) {
            model.PageInfo pageInfo = null;
            try {
                var bytes = PageInfo.getDB().get(pageId.getBytes());
                pageInfo = SerializeUtil.deserialize(bytes);
            } catch (Exception e) {
                System.out.println("Error getting PageInfo for pageId " + pageId);
                e.printStackTrace();
            }

            return pageInfo;
        }

        /** Gets the map of pageId to its pageInfo for all pages
         *
         * @return  mapping of pageId to pageInfo of all pages
         */
        public static HashMap<String, model.PageInfo> getMap_pageId_pageInfo() {
            var map = new HashMap<String, model.PageInfo>();

            RocksIterator iter = PageInfo.getDB().newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                model.PageInfo pageInfo = SerializeUtil.deserialize(iter.value());
                map.put(new String(iter.key()), pageInfo);
            }
            return map;
        }
    }
}
