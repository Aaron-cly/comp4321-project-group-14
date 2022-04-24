package indexer;

import model.PageInfo;
import org.rocksdb.RocksDBException;
import repository.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Indexer {

    public String insert_page(String url) {
        String pageId = null;
        try {
            pageId = Repository.Page.insertPage(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return pageId;
    }

    public void update_ForwardIndex(String url, HashMap<String, List<Integer>> wordPositions) {
        // replace the word in key with corresponding wordId
        HashMap<String, List<Integer>> postings = new HashMap<>();
        try {
            for (String word : wordPositions.keySet()) {
                String wordId = Repository.Word.insertWord(word);
                postings.put(wordId, wordPositions.get(word));
            }
            Repository.ForwardIndex.updateUrl_wordPositions(url, postings);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void update_ForwardIndex_Title(String url, HashMap<String, List<Integer>> wordPositions) {
        HashMap<String, List<Integer>> postings = new HashMap<>();
        try {
            for (String word : wordPositions.keySet()) {
                String wordId = Repository.Word.insertWord(word);
                postings.put(wordId, wordPositions.get(word));
            }
            Repository.ForwardIndex_Title.updateUrl_wordPositions(url, postings);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void add_pageInfo(String url, PageInfo pageInfo) {
        String pageId = insert_page(url);
        Repository.PageInfo.addPageInfo(pageId, pageInfo);
    }

    // should call only after all crawling completed
    public static void construct_invertedIndex() {
        // forwardIndex:           pageId -> HashMap(wordId -> List(position))
        var forwardIndex_file = Repository.ForwardIndex.getAll_ForwardIndex();
        var inverted = new HashMap<String, HashMap<String, List<Integer>>>();

        // loop through each word  O(sum(# keyword in each page))
        for (String pageId : forwardIndex_file.keySet()) {
            var map_wordId_posList = forwardIndex_file.get(pageId);

            for (Map.Entry<String, List<Integer>> entry : map_wordId_posList.entrySet()) {
                var wordId = entry.getKey();
                var posList = entry.getValue();

                if (!inverted.containsKey(wordId) || inverted.get(wordId) == null) {
                    inverted.put(wordId, new HashMap<>());
                }
                assert inverted.containsKey(wordId);

                inverted.get(wordId).put(pageId, posList);
            }
        }
        // write to db
        Repository.InvertedIndex.create_InvertedIndexFile(inverted);
    }

    // should call only after all crawling completed
    public static void construct_invertedIndex_Title() {
        var forwardIndexTitle_file = Repository.ForwardIndex_Title.getAll_ForwardIndex();
        var inverted = new HashMap<String, HashMap<String, List<Integer>>>();

        // loop through each word  O(sum(# keyword in each page))
        for (String pageId : forwardIndexTitle_file.keySet()) {
            var map_wordId_posList = forwardIndexTitle_file.get(pageId);

            for (Map.Entry<String, List<Integer>> entry : map_wordId_posList.entrySet()) {
                var wordId = entry.getKey();
                var posList = entry.getValue();

                if (!inverted.containsKey(wordId) || inverted.get(wordId) == null) {
                    inverted.put(wordId, new HashMap<>());
                }
                assert inverted.containsKey(wordId);

                inverted.get(wordId).put(pageId, posList);
            }
        }
        // write to db
        Repository.InvertedIndex_Title.create_InvertedIndexFile(inverted);
    }
}
