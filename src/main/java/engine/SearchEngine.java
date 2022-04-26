package engine;

import indexer.Indexer;
import model.PageInfo;
import model.RetrievedDocument;
import org.rocksdb.RocksDBException;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class SearchEngine {
    private static char TERM_INDICATOR = '\"';
    private static int OUTPUT_NUM = 50;

    // in here, a term means a word or a phrase
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

//            HashSet<String> parentLinks = new HashSet<>();  // need to store it in a new DB file

            HashMap<String, Integer> term_freq_doc = new HashMap<>();   // get the query terms freq in this doc
            for (String term : inverted.keySet()) {
                term_freq_doc.put(term, inverted.get(term).getOrDefault(pageId, 0));
            }

            RetrievedDocument outputDoc = new RetrievedDocument(pageInfo, scoresOnContent.getOrDefault(pageId, 0.0), term_freq_doc);
            outputList.add(outputDoc);
        }

        return outputList;
    }

    // returns  terms, e.g. "hong kong" university  -> ["hong kong", "university"]
    public static String[] parseQuery(String query) {
        query = query.toLowerCase();
        List<String> phraseList = new ArrayList<>();

        var wordArr = query.split(" ");
        StringBuilder tempTerm = new StringBuilder();
        boolean isInTerm = false;
        for (String word : wordArr) {
            if (word.startsWith(TERM_INDICATOR + "") && word.endsWith(TERM_INDICATOR + "")) {
                if (word.length() > 2) {
                    phraseList.add(
                            Indexer.porter.stripAffixes(word.substring(1, word.length() - 1))
                    );
                }
                continue;
            }

            if (word.startsWith(TERM_INDICATOR + "")) {
                isInTerm = true;
                tempTerm.append(
                        Indexer.porter.stripAffixes(word.substring(1))
                );
            } else if (word.endsWith(TERM_INDICATOR + "")) {
                isInTerm = false;
                tempTerm.append(" ").append(
                        Indexer.porter.stripAffixes(word.substring(0, word.length() - 1))
                );

                phraseList.add(tempTerm.toString());
                tempTerm = new StringBuilder();
            } else {    // words surrounded with NO quotation
                if (isInTerm) {
                    tempTerm.append(" ").append(Indexer.porter.stripAffixes(word));
                } else {
                    phraseList.add(Indexer.porter.stripAffixes(word));
                }
            }
        }
        return phraseList.toArray(new String[0]);
    }
}
