package engine;

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

        var score_on_content = ScoresUtil.Content.compute_scoresOnContent(queryTerms, termFreq);

        // sort by topic DESC then content DESC

        return constructOutput(score_on_content, termFreq);
    }

    protected static List<RetrievedDocument> constructOutput(HashMap<String, Double> scores, HashMap<String, HashMap<String, Integer>> inverted) throws RocksDBException {
        var sortedDoc = scores.keySet().stream().sorted(Comparator.comparing(scores::get)).collect(Collectors.toList());
        Collections.reverse(sortedDoc);
        if (sortedDoc.size() > 50) {
            sortedDoc = sortedDoc.subList(0, OUTPUT_NUM);
        }

        List<RetrievedDocument> outputList = new ArrayList<>();
        for (var doc : sortedDoc) {
            PageInfo pageInfo = Repository.PageInfo.getPageInfo(doc);
            HashSet<String> parentLinks = new HashSet<>();  // need to store it in a new DB file

            HashMap<String, Integer> term_freq_doc = new HashMap<>();   // get the query terms freq in this doc
            for (String term : inverted.keySet()) {
                term_freq_doc.put(term, inverted.get(term).getOrDefault(doc, 0));
            }

            RetrievedDocument outputDoc = new RetrievedDocument(pageInfo, scores.get(doc), parentLinks, term_freq_doc);
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
                    phraseList.add(word.substring(1, word.length() - 1));
                }
                continue;
            }

            if (word.startsWith(TERM_INDICATOR + "")) {
                isInTerm = true;
                tempTerm.append(word.substring(1));
            } else if (word.endsWith(TERM_INDICATOR + "")) {
                isInTerm = false;
                tempTerm.append(" ").append(word.substring(0, word.length() - 1));

                phraseList.add(tempTerm.toString());
                tempTerm = new StringBuilder();
            } else {    // words surrounded with NO quotation
                if (isInTerm) {
                    tempTerm.append(" ").append(word);
                } else {
                    phraseList.add(word);
                }
            }
        }
        return phraseList.toArray(new String[0]);
    }

}
