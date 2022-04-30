package indexer;

import model.Porter;
import model.PageInfo;
import org.jsoup.nodes.Document;
import org.rocksdb.RocksDBException;
import repository.Database;
import repository.Repository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/** Indexer Class which writes data into databases, and other operations before inserting the data
 *  into the databases
 */
public class Indexer {

    public Porter porter = new Porter();
    public HashSet<String> indexedPageIds = new HashSet<>();
    public static ArrayList<String> stopWords;

    static {
        // retrieve stopwords from text file
        try {
            var pwd = System.getProperty("user.dir");
            var absoluteProjectRootDir = pwd.substring(0, pwd.indexOf(Database.projectFolder)) + Database.projectFolder;
            var list = Files.readAllLines(Paths.get(absoluteProjectRootDir + Database.rootTomcatDirectory + "/stopwords.txt"));
            stopWords = new ArrayList<>(list);
        } catch (Exception e) {
            System.out.println("Something went wrong while reading the file");
            e.printStackTrace();
        }
    }

    /** determines whether url should be ignored by the crawler.
     *  If the page is already in indexed and the last modified date is unchanged, should return true
     *  to indicate that the page should be ignored. Otherwise return false, to indicate that it should not be ignored.
     * @param url  The url of the page
     * @param latestDate  The latest last modified date of the page
     * @return  a boolean value, indicating whether the page should be ignored or not
     */
    public boolean shouldIgnoreUrl(String url, String latestDate) {
        // should not ignore if page is not yet in db/indexed
        String pgId = Repository.Page.getPageId(url);
        if (pgId == null || !indexedPageIds.contains(pgId)) {
            return false;
        }

        String existingModifiedDate = Repository.PageInfo.getPageInfo(pgId).lastModifiedDate;
        // should not ignore if any one of the dates are null
        if (existingModifiedDate == null  || existingModifiedDate.equals("N/A") || latestDate == null)
            return false;

        // compare latest last-modified-date and existing last-modified-date
        try {
            Date newDate = new SimpleDateFormat("yyyy-MM-dd").parse(latestDate);
            Date currentDate = new SimpleDateFormat("yyyy-MM-dd").parse(existingModifiedDate);
            // should not ignore if last modification date is later than the one recorded in db
            if (newDate.after(currentDate))
                return false;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // ignore if all other conditions do not hold
        return true;
    }

    /** Indexes a new page.
     * It extracts and stems the words from the page, constructs its corresponding {@link PageInfo} and
     * inserts this page into the databases
     * @param doc  The document of the page
     * @param url  The url of the page
     * @param lastModifiedDate  The last modified date of the page
     * @param pgSize  The page size of the page
     * @param childLinks  The child links of the page
     */
    public void insert_new_page(Document doc, String url, String lastModifiedDate,
                                int pgSize, HashSet<String> childLinks) {
        // index the current page
        // extract and stem words
        var words = extractWords(doc);
        var stemList = stemWords(words);
        var map_word_posList = consolidatePositions(stemList);
        var max_termFreq = getMax_termFreq(map_word_posList);

        pgSize = pgSize == -1 ? words.size() : pgSize;

        // convert child urls to its page ids
        childLinks = childLinks.stream().map(l -> {
            try {
                return Repository.Page.insertPage(l);
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
            return l;
        }).collect(Collectors.toCollection(HashSet::new));

        PageInfo pageInfo = new PageInfo(doc.title(), url, lastModifiedDate,
                childLinks,
                String.valueOf(pgSize),
                max_termFreq
        );

        // insert into corresponding databases/tables
        indexedPageIds.add(insert_page(url));
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

    // helper function to insert page to the page databases, i.e. PageToPageID and PageIDToPage
    private String insert_page(String url) {
        // index the current page
        String pageId = null;
        try {
            pageId = Repository.Page.insertPage(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return pageId;
    }

    // stems the words in a list
    private List<String> stemWords(List<String> words) {
        return words.stream()
                .map(w -> porter.stripAffixes(w)).collect(Collectors.toList());
    }

    /** Updates the forwardIndex of the pages.
     *
     * @param url The url of the page
     * @param wordPositions The word positions of the page
     */
    private void update_ForwardIndex(String url, HashMap<String, List<Integer>> wordPositions) {
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

    /** Updates the forward index for the titles
     *
     * @param url  The url of the page
     * @param wordPositions  The new word Positions
     */
    private void update_ForwardIndex_Title(String url, HashMap<String, List<Integer>> wordPositions) {
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

    // adds the page info to the PageInfo db
    private void add_pageInfo(String url, PageInfo pageInfo) {
        String pageId = insert_page(url);
        Repository.PageInfo.addPageInfo(pageId, pageInfo);
    }

    /** Constructs an inverted Index and saves it to its database, from the forward index database.
     * Should only be called when crawling is complete, when the forward indexes are already constructed */
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

    /** Constructs an inverted Index for the title and saves it to its database, from the forward index title database.
     * Should only be called when crawling is complete, when the forward indexes are already constructed */
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

    /** Constructs the parent links from the child links. Should only be called when crawling is complete,
     *  when all the child links are already inserted into the PageInfo database.
     */
    public void construct_parents_from_child_links() {
        var map = Repository.PageInfo.getMap_pageId_pageInfo();

        for (String id : indexedPageIds) {
            List<String> parents = new ArrayList<String>();
            for (Map.Entry<String, PageInfo> e : map.entrySet()) {
                if (e.getValue().childLinks.contains(id))
                    parents.add(e.getKey());
            }
            var currentPageInfo = Repository.PageInfo.getPageInfo(id);
            currentPageInfo.parentLinks = new HashSet<>(parents);
            Repository.PageInfo.addPageInfo(id, currentPageInfo);
        }
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
