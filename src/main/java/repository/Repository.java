package repository;

import java.nio.charset.StandardCharsets;
import java.util.*;

import model.MetaData;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class Repository {
    public static class Word {
        private static String dbPath = "Word";
        protected static RocksDB word_table;

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
            byte[] value = word_table.get(wordId.getBytes());

            // return word of given wordId
            return new String(value);
        }

        public static String insertWord(String word) throws RocksDBException {
            // TODO
            String existingId = getWordId(word);

            if (existingId != null) {
                return existingId;
            }

            String wordId = String.valueOf(word.hashCode());
            // insert new word to db
            word_table.put(wordId.getBytes(), word.getBytes());
            // return the wordId of the inserted word
            return wordId;
        }

        public static String getWordId(String word) {
            RocksIterator iter = word_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String entry_word = new String(iter.value());
                if (entry_word.equals(word)) {
                    // return the wordId of the given word
                    return new String(iter.key());
                }
            }
            return null;
        }

    }

    public static class Page {
        private static String dbPath = "Page";
        protected static RocksDB page_table;

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
            byte[] value = page_table.get(pageId.getBytes());

            // return word of given wordId
            return new String(value);
        }

        public static String insertPage(String url) throws RocksDBException {
            String existingId = getPageId(url);

            if (existingId != null) {
                return existingId;
            }

            String pageId = String.valueOf(url.hashCode());
            // insert new word to db
            page_table.put(pageId.getBytes(), url.getBytes());
            // return the pageId of the inserted word
            return pageId;
        }

        public static String getPageId(String url) {
            //TODO
            //return pageId of the given url
            RocksIterator iter = page_table.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                String entry_word = new String(iter.value());
                if (entry_word.equals(url)) {
                    // return the wordId of the given word
                    return new String(iter.key());
                }
            }
            return null;
        }

    }

    public static class ForwardFrequency {
        private static String dbPath = "Forward_Frequency";
        private static RocksDB forward_frequency_table;
        private static String DELIMITER = ";";
        private static String SEPARATOR = "::";

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
            HashMap<String, Integer> map_wordId_freq = new HashMap<>();

            if (pageIn_ForwardFrequency(pageId)) {
                map_wordId_freq = getMap_WordId_Freq(pageId);
            }

            for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
                String wordId = entry.getKey();
                int freq = entry.getValue();

                int oriValue = map_wordId_freq.getOrDefault(wordId, 0);
                map_wordId_freq.put(wordId, oriValue + freq);
            }

            // write to db
            StringBuilder parsed_WordFreq = new StringBuilder();
            for (Map.Entry<String, Integer> entry : map_wordId_freq.entrySet()) {
                parsed_WordFreq.append(entry.getKey() + SEPARATOR + entry.getValue() + DELIMITER);
            }

            try {
                forward_frequency_table.put(pageId.getBytes(), parsed_WordFreq.toString().getBytes());
            } catch (RocksDBException e) {
                throw new RocksDBException("Error writing to db");
            }
        }

        // get posting for a given page
        public static HashMap<String, Integer> getMap_WordId_Freq(String pageId) {
            if (!pageIn_ForwardFrequency(pageId)) {
                return new HashMap<>();
            }

            String raw_postings = null;
            try {
                raw_postings = new String(forward_frequency_table.get(pageId.getBytes()));
            } catch (RocksDBException e) {
            }

            if (raw_postings == null) return new HashMap<>();

            HashMap<String, Integer> map_wordId_freq = new HashMap<>();
            String[] postingArr = raw_postings.split(DELIMITER);
            for (String posting : postingArr) {
                String[] values = posting.split(SEPARATOR);
                String wordId = values[0];
                int freq = Integer.parseInt(values[1]);

                map_wordId_freq.put(wordId, freq);
            }

            // return the freq map of a page
            return map_wordId_freq;
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
        public static void saveForwardToInverted(){
            HashMap<String, String> map = new HashMap<>();
            // convert forwardFrequency to invertedFrequncy
            RocksIterator iter = ForwardFrequency.forward_frequency_table.newIterator();
            for(iter.seekToFirst(); iter.isValid(); iter.next()) {
                // get pageId and posting list from forward frequency
                String pageId = new String(iter.key());
                String postingList = new String(iter.value());

                // split the posting list to its entries
                var postingEntries = postingList.split(";");
                for(var entry : postingEntries){
                    // first index is wordId, second is word frequency
                    var wordId = entry.split("::")[0];
                    var wordFrequency = entry.split("::")[1];
                    // new entry to be place into map
                    var newEntry = pageId + SEPARATOR + wordFrequency + DELIMITER;

                    if(!map.containsKey(wordId)){
                        // put to map for the first time
                        map.put(wordId, newEntry);
                        continue;
                    }
                    // append existing entry
                    map.put(wordId, map.get(wordId) + newEntry);
                }
            }

            // save this map to db
            for(Map.Entry<String, String> entry : map.entrySet()){
                try {
                    inverted_frequency_table.put(entry.getKey().getBytes(), entry.getValue().getBytes());
                } catch(RocksDBException e){
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

            if (raw_postings == null || raw_postings.equals(""))  return new HashMap<>();

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

    public static class PageInfo{
        private static String dbPath = "Page_Info";
        private static RocksDB pageInfoDb;

        public static void addPageInfo(int docId, MetaData data) {
            try {
                pageInfoDb.put(String.valueOf(docId).getBytes(), MetaData.convertToByteArray(data));
            } catch (RocksDBException e) {
                System.out.println("Could not save meta information of page");
                e.printStackTrace();
            }
        }

        public static MetaData getMetaInfo(int docId) throws RocksDBException {
            return MetaData.deserialize(pageInfoDb.get(String.valueOf(docId).getBytes()));
        }

    }

}
