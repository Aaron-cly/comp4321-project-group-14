package engine;

import org.rocksdb.RocksDBException;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class SearchEngine {
    private static char TERM_INDICATOR = '\"';

    // in here, a term means a word or a phrase
    public static List<String> processQuery(String query) throws RocksDBException {

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
        Arrays.fill(query_vector, 1.0);

        var DF = computeDF(terms, termFreq);

        HashSet<String> candidatePageSet = new HashSet<>(); // pages that contain any of the terms
        for (String term : terms) {
            candidatePageSet.addAll(termFreq.get(term).keySet());
        }

        // IDF
        int totalNum_pages = Repository.Page.getTotalNumPage();
        var IDF = computeIDF(DF, totalNum_pages);

        // Page vectors
        HashMap<String, double[]> page_vector = compute_pageVectors(terms, termFreq, candidatePageSet, IDF);

        // cosine sim
//        HashMap<String, Double> cosSim = new HashMap<>();
//        double queryLength = Math.sqrt(query_vector.stream().reduce(0.0, (sum, w) -> sum + Math.pow(w,2)));
//        for (String page : candidatePageSet) {
//            double inner = page_vector.get(page).stream().reduce(0.0, Double::sum);
//            double pageLength = Math.sqrt(page_vector.get(page).stream().reduce(0.0, (sum, w) -> sum + Math.pow(w,2)));
//
//
//        }
//
        return null;

    }

//    private static cosSim

    protected static HashMap<String, double[]> compute_pageVectors(String[] terms, HashMap<String, HashMap<String, Integer>> termFreq, HashSet<String> candidatePageSet, double[] IDF) throws RocksDBException {
        HashMap<String, double[]> page_vectors = new HashMap<>();
        for (String page : candidatePageSet) {
            double[] vector = new double[terms.length];

            for (int termIndex = 0; termIndex < terms.length; termIndex++) {
                String term = terms[termIndex];
                if (!termFreq.get(term).containsKey(page)) {
                    vector[termIndex] = 0.0;
                    continue;
                }

                // compute the term weight in this page
                int tf_max = 0;
//                var termFreqInPage = Repository.ForwardIndex.getMap_WordId_Positions(page);
//                for (var freq : termFreqInPage.values()) {
//                    tf_max = Math.max(freq.stream().max(Integer::compareTo).orElse(0), tf_max);
//                }
                tf_max = 1;
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
