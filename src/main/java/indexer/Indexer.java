package indexer;

import engine.Porter;
import model.PageInfo;
import org.jsoup.nodes.Document;
import org.rocksdb.RocksDBException;
import repository.Repository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Indexer {

    public static Porter porter = new Porter();
    private static ArrayList<String> stopWords;

     {
        // retrieve stopwords from text file
        try {
            var list = Files.readAllLines(Paths.get("./stopwords.txt"));
            stopWords = new ArrayList<>(list);
        } catch (Exception e) {
            System.out.println("Something went wrong while reading the file");
            e.printStackTrace();
        }
    }

    // inserts new page and its title into dbs
    public void insert_new_page(Document doc, String url, String lastModifiedDate,
                                  int pgSize, HashSet<String> childLinks){
        // index the current page
        // extract and stem words
        var stemList = stemWords(extractWords(doc));
        var map_word_posList = consolidatePositions(stemList);
        var max_termFreq = getMax_termFreq(map_word_posList);
        PageInfo pageInfo = new PageInfo(doc.title(), url, lastModifiedDate,
                childLinks,
                String.valueOf(pgSize),
                max_termFreq
        );

        // insert into corresponding databases/tables
        insert_page(url);
        update_ForwardIndex(url, map_word_posList);
        add_pageInfo(url, pageInfo);

        // index the title of the page too
        int lastIndex = doc.title().lastIndexOf(" |");
        String title = lastIndex == -1 ? doc.title() : doc.title().substring(0, lastIndex);
        var wordList_title =
                Arrays.stream(title.split(" "))
                        .filter(w -> !w.isBlank())
                        .map(String::toLowerCase)
                        .map(w -> porter.stripAffixes(w))
                        .collect(Collectors.toList());

        map_word_posList = consolidatePositions(wordList_title);
        update_ForwardIndex_Title(url, map_word_posList);
    }

    public String insert_page(String url) {
        // index the current page
        String pageId = null;
        try {
            pageId = Repository.Page.insertPage(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return pageId;
    }

    private List<String> stemWords(List<String> words){
        return words.stream()
                .map(w -> porter.stripAffixes(w)).collect(Collectors.toList());
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

    // extract all words from a page
    private List<String> extractWords(Document d) {
        var list = new ArrayList<String>();
        var elements = d.body().select("*");
        for (var e : elements) {
            // skip the elements with empty content
            String[] words = e.ownText().split("\\W+");

            // filter out strings containing numbers and stopwords
            list.addAll(
                    Arrays.stream(words)
                            .map(String::toLowerCase)
                            .filter(s -> !s.equals("") && !s.matches(".*\\d.*") && !stopWords.contains(s))
                            .collect(Collectors.toList())
            );
        }
        return list;
    }

    private int getMax_termFreq(HashMap<String, List<Integer>> word_posList) {
        int max = 0;
        for (var posList : word_posList.values()) {
            max = Math.max(max, posList.size());
        }
        return max;
    }

    // construct word -> List(position)
    private HashMap<String, List<Integer>> consolidatePositions(List<String> extracted) {
        var wordPositions = new HashMap<String, List<Integer>>();
        for (int i = 0; i < extracted.size(); i++) {
            String word = extracted.get(i);

            List<Integer> posList = wordPositions.getOrDefault(word, null);
            if (posList == null) {
                posList = new ArrayList<>();
                posList.add(i);
                wordPositions.put(word, posList);
            } else {
                posList.add(i);
            }
        }
        return wordPositions;
    }
}
