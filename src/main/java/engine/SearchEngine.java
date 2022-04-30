package engine;

import model.PageInfo;
import model.Porter;
import model.RetrievedDocument;
import org.rocksdb.RocksDBException;
import repository.Database;
import repository.Repository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/** SearchEngine class to process and parse queries, and retrieve results/pages for the queries */
public class SearchEngine {
    private static String TERM_INDICATOR = "\"";
    private static int OUTPUT_NUM = 50;
    private static ArrayList<String> stopWords;


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

    /** Processes query and returns the relevant pages, with respect to CosSim scores. Invokes functions from
     * {@link ScoresUtil} to calculate the scores.
     *
     * @param query  The query to be processed
     * @return  The relevant documents in {@link RetrievedDocument} form
     * @throws RocksDBException
     */
    public static List<RetrievedDocument> processQuery(String query) throws RocksDBException {
        String[] queryTerms = parseQuery(query);
        HashMap<String, HashMap<String, Integer>> termFreq = new HashMap<>(); // tf_ij, map term -> (map pageId -> freq)

        for (String term : queryTerms) {
            HashMap<String, Integer> freq;
            if (!term.contains(" ")) {      // actually a single word
                freq = ScoresUtil.Content.computeTermFreq_word(term);
            } else {                        // a phrase containing multiple keywords
                freq = ScoresUtil.Content.computeTermFreq_phrase(term);
            }
            termFreq.put(term, freq);
        }

        var scores_on_content = ScoresUtil.Content.compute_scoresOnContent(queryTerms, termFreq);
        var scores_on_title = ScoresUtil.Title.compute_scoresOnTitle(queryTerms);

        return constructOutput(scores_on_content, scores_on_title, termFreq);
    }

    /** Constructs the output of the results, based on scores of content and titles
     * Sorts documents/results, based on title scores and content scores, with the former having higher priority
     * Retrieves the urls from the ids and returns a list of {@link RetrievedDocument} as the results
     *
     * @param scoresOnContent  The scores based on content of the pages
     * @param scoresOnTitle  The scores based on title of the pages
     * @param inverted  The inverted index
     * @return  The sorted results in the form of {@link RetrievedDocument}
     * @throws RocksDBException
     */
    protected static List<RetrievedDocument> constructOutput(HashMap<String, Double> scoresOnContent, HashMap<String, Double> scoresOnTitle, HashMap<String, HashMap<String, Integer>> inverted) throws RocksDBException {
        // sort by title DESC then content DESC
        HashSet<String> pageSet = new HashSet<>();
        pageSet.addAll(scoresOnContent.keySet());
        pageSet.addAll(scoresOnTitle.keySet());
        List<RetrievedDocument> outputList = new ArrayList<>();

        ArrayList<String> sortedPages = new ArrayList<>(pageSet);
        sortedPages.sort((page1, page2) -> {
            double EPSILON = 0.0001;
            var page1_titleScore = scoresOnTitle.getOrDefault(page1, 0.0);
            var page2_titleScore = scoresOnTitle.getOrDefault(page2, 0.0);
            if (Math.abs(page1_titleScore - page2_titleScore) > EPSILON) {
                return Double.compare(page2_titleScore, page1_titleScore);
            }

            var page1_contentScore = scoresOnContent.getOrDefault(page1, 0.0);
            var page2_contentScore = scoresOnContent.getOrDefault(page2, 0.0);
            if (Math.abs(page1_contentScore - page2_contentScore) > EPSILON) {
                return Double.compare(page2_contentScore, page1_contentScore);
            }

            return 0;
        });

        if (sortedPages.size() > OUTPUT_NUM) {
            sortedPages = sortedPages.stream().limit(OUTPUT_NUM).collect(Collectors.toCollection(ArrayList::new));
        }

        for (var pageId : sortedPages) {
            PageInfo pageInfo = Repository.PageInfo.getPageInfo(pageId);
            pageInfo.childLinks = pageInfo.childLinks.stream().map(id -> {
                try {
                    return Repository.Page.getPageUrl(id);
                } catch (RocksDBException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toCollection(HashSet::new));

            pageInfo.parentLinks = pageInfo.parentLinks.stream().map(id -> {
                try {
                    return Repository.Page.getPageUrl(id);
                } catch (RocksDBException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toCollection(HashSet::new));

            HashMap<String, Integer> term_freq_doc = new HashMap<>();   // get the query terms freq in this doc
            for (String term : inverted.keySet()) {
                term_freq_doc.put(term, inverted.get(term).getOrDefault(pageId, 0));
            }

            // the top 5 frequent stemmed keywords of the doc
            var forward = Repository.ForwardIndex.getMap_WordId_Positions(pageId);
            var top5_word = forward.keySet().stream().sorted(Comparator.comparing(word -> -forward.get(word).size()))
                    .limit(5)
                    .collect(Collectors.toList());

            LinkedHashMap<String, Integer> sorted_top5_words = new LinkedHashMap<>();
            top5_word.forEach(wId -> {
                try {
                    sorted_top5_words.put(Repository.Word.getWord(wId), forward.get(wId).size());
                } catch (RocksDBException e) {
                    throw new RuntimeException(e);
                }
            });

            RetrievedDocument outputDoc = new RetrievedDocument(pageInfo, scoresOnContent.getOrDefault(pageId, 0.0), term_freq_doc, sorted_top5_words);
            outputList.add(outputDoc);
        }

        return outputList;
    }

    // returns stemmed terms, e.g. "hong kong" university  -> ["hong kong", "univers"]

    /** Parses the query and stems the words/phrases using porter's algorithm
     *
     * @param query  Query to be parse
     * @return  The individual terms and phrases of the queries in an array
     */
    public static String[] parseQuery(String query) {
        query = query.toLowerCase();
        List<String> phraseList = new ArrayList<>();
        Porter porter = new Porter();

        var wordArr = query.split(" ");
        StringBuilder tempTerm = new StringBuilder();
        boolean isInTerm = false;
        for (String word : wordArr) {
            if (stopWords.contains(word)) continue;

            if (word.startsWith(TERM_INDICATOR + "") && word.endsWith(TERM_INDICATOR + "")) {
                if (word.length() > 2) {
                    phraseList.add(
                            porter.stripAffixes(word.substring(1, word.length() - 1))
                    );
                }
                continue;
            }

            if (word.startsWith(TERM_INDICATOR + "")) {
                isInTerm = true;
                tempTerm.append(
                        porter.stripAffixes(word.substring(1))
                );
            } else if (word.endsWith(TERM_INDICATOR + "")) {
                isInTerm = false;
                tempTerm.append(" ").append(
                        porter.stripAffixes(word.substring(0, word.length() - 1))
                );

                phraseList.add(tempTerm.toString());
                tempTerm = new StringBuilder();
            } else {    // words surrounded with NO quotation
                if (isInTerm) {
                    tempTerm.append(" ").append(porter.stripAffixes(word));
                } else {
                    phraseList.add(porter.stripAffixes(word));
                }
            }
        }
        return phraseList.toArray(new String[0]);
    }
}
