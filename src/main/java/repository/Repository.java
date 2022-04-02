package repository;

import java.util.*;

import model.SerializeUtil;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class Repository {
    public static class Word {
        private static String dbPath = "Word";
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
        private static String dbPath = "Page";
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

    public static class ForwardFrequency {
        private static String dbPath = "Forward_Frequency";
        private static RocksDB forward_frequency_table;
        // pageId -> HashMap(wordId -> freq)

        static {
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                // drop all data database first to ensure fresh run
                RocksDB.destroyDB(dbPath, options);
                forward_frequency_table = RocksDB.open(options, dbPath);
            } catch (RocksDBException e) {
            }
        }

        // check if a page is in Forward frequency as a key
        private static boolean pageIn_ForwardFrequency(String pageId) {
            boolean isIn;
            try {
                forward_frequency_table.get(pageId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        public static void updateUrl_wordFreq(String url, HashMap<String, Integer> frequencies) throws RocksDBException {
            String pageId = null;
            try {
                pageId = Page.insertPage(url);
            } catch (RocksDBException e) {
            }
            if (pageId == null) {
                return;
            }

            updatePage_wordFreq(pageId, frequencies);
        }

        // input frequencies: wordId -> freq
        public static void updatePage_wordFreq(String pageId, HashMap<String, Integer> frequencies) throws RocksDBException {
            byte[] dataBytes = SerializeUtil.serialize(frequencies);
            forward_frequency_table.put(pageId.getBytes(), dataBytes);
        }

        // get the HashMap of word frequencies for a given page
        public static HashMap<String, Integer> getMap_WordId_Freq(String pageId) throws RocksDBException {
            if (!pageIn_ForwardFrequency(pageId)) {
                return new HashMap<>();
            }

            HashMap<String, Integer> map_wordId_freq = null;
            try {
                map_wordId_freq = SerializeUtil.deserialize(forward_frequency_table.get(pageId.getBytes()));
            } catch (RocksDBException e) {
                throw new RocksDBException(e.getMessage());
            }
            return map_wordId_freq;
        }

        public static void print() {
            RocksIterator iter = forward_frequency_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println(new String(iter.key()) + ": " + new String(iter.value()));
            }
        }
    }

    public static class InvertedFrequency {
        private static String dbPath = "Inverted_Frequency";
        private static RocksDB inverted_frequency_table;
        private static String DELIMITER = ";";
        private static String SEPARATOR = "::";

        static {
            RocksDB.loadLibrary();
            Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                // drop all data database first to ensure fresh run
                RocksDB.destroyDB(dbPath, options);
                inverted_frequency_table = RocksDB.open(options, dbPath);
            } catch (RocksDBException e) {
            }
        }

        // check if a word is in Inverted Frequency as a key
        public static boolean wordIn_InvertedFrequency(String wordId) {
            boolean isIn;
            try {
                inverted_frequency_table.get(wordId.getBytes());
                isIn = true;
            } catch (RocksDBException e) {
                isIn = false;
            }
            return isIn;
        }

        // converts forward to inverted and saves it to invertedFrequencyDb
        public static void saveForwardToInverted() {
            HashMap<String, String> map = new HashMap<>();
            // convert forwardFrequency to invertedFrequncy
            RocksIterator iter = ForwardFrequency.forward_frequency_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                // get pageId and posting list from forward frequency
                String pageId = new String(iter.key());
                String postingList = new String(iter.value());

                // split the posting list to its entries
                var postingEntries = postingList.split(";");
                for (var entry : postingEntries) {
                    // first index is wordId, second is word frequency
                    var wordId = entry.split("::")[0];
                    var wordFrequency = entry.split("::")[1];
                    // new entry to be place into map
                    var newEntry = pageId + SEPARATOR + wordFrequency + DELIMITER;

                    if (!map.containsKey(wordId)) {
                        // put to map for the first time
                        map.put(wordId, newEntry);
                        continue;
                    }
                    // append existing entry
                    map.put(wordId, map.get(wordId) + newEntry);
                }
            }

            // save this map to db
            for (Map.Entry<String, String> entry : map.entrySet()) {
                try {
                    inverted_frequency_table.put(entry.getKey().getBytes(), entry.getValue().getBytes());
                } catch (RocksDBException e) {
                    e.printStackTrace();
                }
            }
        }

        // input frequency map pageId -> freq
//        public static void updateInvertedFrequency(String wordId, HashMap<String, Integer> frequencies) throws RocksDBException {
//            HashMap<String, Integer> map_pageId_freq = new HashMap<>();
//
//            if (wordIn_InvertedFrequency(wordId)) {
//                map_pageId_freq = getMap_PageId_Freq(wordId);
//            }
//
//            for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
//                String pageId = entry.getKey();
//                int freq = entry.getValue();
//
//                int oriValue = map_pageId_freq.getOrDefault(wordId, 0);
//                map_pageId_freq.put(wordId, oriValue + freq);
//            }
//
//            // write to db
//            StringBuilder parsed_WordFreq = new StringBuilder();
//            for (Map.Entry<String, Integer> entry : map_pageId_freq.entrySet()) {
//                parsed_WordFreq.append(entry.getKey() + SEPARATOR + entry.getValue() + DELIMITER);
//            }
//
//            try {
//                inverted_frequency_table.put(wordId.getBytes(), parsed_WordFreq.toString().getBytes());
//            } catch (RocksDBException e) {
//                throw new RocksDBException("Error writing to db");
//            }
//        }

        public static HashMap<String, Integer> getMap_PageId_Freq(String wordId) {
            if (!wordIn_InvertedFrequency(wordId)) {
                return new HashMap<>();
            }

            String raw_postings = null;
            try {
                raw_postings = new String(inverted_frequency_table.get(wordId.getBytes()));
            } catch (RocksDBException e) {
            }

            if (raw_postings == null || raw_postings.equals("")) return new HashMap<>();

            HashMap<String, Integer> map_pageId_freq = new HashMap<>();
            String[] postingArr = raw_postings.split(DELIMITER);
            for (String posting : postingArr) {
                String[] values = posting.split(SEPARATOR);
                String pageId = values[0];
                int freq = Integer.parseInt(values[1]);

                map_pageId_freq.put(pageId, freq);
            }

            // return the freq map of a word
            return map_pageId_freq;
        }
    }

    public static class PageInfo {
        private static String dbPath = "Page_Info";
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
