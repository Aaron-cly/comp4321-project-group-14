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

        String[] terms = parseQuery(query);
        HashMap<String, HashMap<String, Integer>> termFreq = new HashMap<>(); // map term -> (map pageId -> freq)

        for (String term : terms) {
            HashMap<String, Integer> freq;
            if (!term.contains(" ")) {      // actually a single word
                freq = computeTermFreq_word(term);
            } else {                        // a phrase containing multiple keywords
                freq = computeTermFreq_phrase(term);
            }
            termFreq.put(term, freq);
        }

        double[] query_vector = new double[terms.length];
        Arrays.fill(query_vector, 1.0);     // assume all terms have equal weight

        var DF = computeDF(terms, termFreq);

        HashSet<String> candidatePageSet = new HashSet<>(); // pages that contain any of the terms
        for (String term : terms) {
            candidatePageSet.addAll(termFreq.get(term).keySet());
        }

        // IDF
        int totalNum_pages = Repository.Page.getTotalNumPage();
        var IDF = computeIDF(DF, totalNum_pages);

        // Page vectors
        HashMap<String, double[]> doc_vectors = compute_docVectors(terms, termFreq, candidatePageSet, IDF);

        // cosine sim scores
        HashMap<String, Double> scores = compute_scores(doc_vectors, query_vector);

        return constructOutput(scores, termFreq);
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

            HashMap<String, Integer> term_freq_doc = new HashMap<>();
            for (String term : inverted.keySet()) {
                term_freq_doc.put(term, inverted.get(term).getOrDefault(doc, 0));
            }

            RetrievedDocument outputDoc = new RetrievedDocument(pageInfo, scores.get(doc), parentLinks, term_freq_doc);
            outputList.add(outputDoc);
        }

        return outputList;
    }

    protected static HashMap<String, Double> compute_scores(HashMap<String, double[]> page_vectors, double[] query_vector) {
        HashMap<String, Double> pages_score = new HashMap<>();
        double query_length = compute_vectorLength(query_vector);

        for (String page : page_vectors.keySet()) {
            var pageVec = page_vectors.get(page);

            double inner = 0;
            for (int i = 0; i < query_vector.length; i++) {
                var query_weight = query_vector[i];
                var doc_term_weight = pageVec[i];
                inner += query_weight * doc_term_weight;
            }

            double doc_length = compute_vectorLength(pageVec);

            double score = inner / (doc_length * query_length);
            pages_score.put(page, score);
        }
        return pages_score;
    }

    protected static double compute_vectorLength(double[] vector) {
        double length = 0;
        for (var weight : vector) {
            length += Math.pow(weight, 2);
        }
        return Math.sqrt(length);
    }

    protected static HashMap<String, double[]> compute_docVectors(String[] terms, HashMap<String, HashMap<String, Integer>> termFreq, HashSet<String> candidatePageSet, double[] IDF) throws RocksDBException {
        HashMap<String, double[]> page_vectors = new HashMap<>();
        for (String page : candidatePageSet) {
            double[] vector = new double[terms.length];

            for (int termIndex = 0; termIndex < terms.length; termIndex++) {
                String term = terms[termIndex];
                if (!termFreq.get(term).containsKey(page)) {
                    vector[termIndex] = 0.0;
                    continue;
                }

                int tf_max = Repository.PageInfo.getPageInfo(page).max_termFreq;

                double weight = (termFreq.get(term).get(page) / (double) tf_max) * IDF[termIndex];
                vector[termIndex] = weight;
            }
            page_vectors.put(page, vector);
        }
        return page_vectors;
    }

    protected static double[] computeIDF(int[] DF, int totalNum_pages) {
        double[] IDF = new double[DF.length];
        for (int i = 0; i < DF.length; i++) {
            double idf = (Math.log(totalNum_pages) - Math.log(DF[i])) / Math.log(2);
            IDF[i] = idf;
        }
        return IDF;
    }


    protected static int[] computeDF(String[] terms, HashMap<String, HashMap<String, Integer>> termFreq) {
        int[] DF = new int[terms.length];
        for (int i = 0; i < terms.length; i++) {
            DF[i] = termFreq.get(terms[i]).size();
        }
        return DF;
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

    public static String[] getWords_in_phrase(String phrase) {
        return phrase.split(" ");
    }

    // returns map(pageId -> tf of a single word)
    public static HashMap<String, Integer> computeTermFreq_word(String word) {
//        String wordId = String.valueOf(word.hashCode());
        String wordId = Repository.Word.getWordId(word);

        var termPos_word = Repository.InvertedIndex.getMap_pageId_wordPosList(wordId);
        if (termPos_word == null || termPos_word.isEmpty()) return new HashMap<>();

        HashMap<String, Integer> termFreq = new HashMap<>();
        for (String page : termPos_word.keySet()) {
            var posList = termPos_word.get(page);
            termFreq.put(page, posList.size());
        }

        return termFreq;
    }

    // returns phrase frequency of each page that contains it
    public static HashMap<String, Integer> computeTermFreq_phrase(String phrase) {
        var wordArr = getWords_in_phrase(phrase);

        var invertedIndex = new HashMap<String, HashMap<String, List<Integer>>>();   // relevant entries in inverted
        for (String word : wordArr) {
            String wordId = Repository.Word.getWordId(word);
            if (wordId == null) {   // no page contains the phrase
                return new HashMap<>();
            }
            var temp = Repository.InvertedIndex.getMap_pageId_wordPosList(wordId);
            invertedIndex.put(word, temp);
        }

        List<Set<String>> listOfPages = new ArrayList<>();      // pages that contain any of the words
        for (var map_page_posList : invertedIndex.values()) {
            listOfPages.add(map_page_posList.keySet());
        }

        HashSet<String> pages_contain_allWords = getIntersection(listOfPages);
        if (pages_contain_allWords.isEmpty()) return new HashMap<>();

        // decrement positions
        for (int i = 1; i < wordArr.length; i++) {
            var map_page_posList = invertedIndex.get(wordArr[i]);
            for (String pageId : pages_contain_allWords) {
                var posList = map_page_posList.get(pageId);
                final int diff = i;
                var displaced = posList.stream().map(pos -> pos - diff).collect(Collectors.toList());
                map_page_posList.put(pageId, displaced);
            }
        }

        // intersection of positions and return intersection size
        HashMap<String, Integer> map_pageId_freq = new HashMap<>(); // return map
        for (String pageId : pages_contain_allWords) {
            List<Set<Integer>> positionList = new ArrayList<>();
            for (String word : wordArr) {
                var posList = invertedIndex.get(word).get(pageId);
                positionList.add(new HashSet<>(posList));
            }
            HashSet<Integer> posIntersection = getIntersection(positionList);
            map_pageId_freq.put(pageId, posIntersection.size());
        }

        return map_pageId_freq;
    }

    private static <E> HashSet<E> getIntersection(List<Set<E>> setList) {
        var intersection = new HashSet<E>();
        for (var set : setList) {
            if (intersection.isEmpty()) {
                intersection.addAll(set);
            } else {
                intersection.retainAll(set);
            }
        }
        return intersection;
    }
}
