package engine;

import org.rocksdb.RocksDBException;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class SearchEngine {
    private static char TERM_INDICATOR = '\"';
    public static HashMap<String, HashMap<String, List<Integer>>> INVERTED;
    public static HashMap<String, HashMap<String, List<Integer>>> FORWARD;
    public static int TOTAL_NUM_PAGES;


    static {
        INVERTED = Repository.InvertedIndex.getAll_InvertedIndex();
        FORWARD = Repository.ForwardIndex.getAll_ForwardIndex();
        TOTAL_NUM_PAGES = Repository.Page.getTotalNumPage();
    }

    public static void initializeMock(HashMap<String, HashMap<String, List<Integer>>> inverted, HashMap<String, HashMap<String, List<Integer>>> forward, int num) {
        INVERTED = inverted;
        FORWARD = forward;
        TOTAL_NUM_PAGES = num;
    }

    // in here, a term means a word or a phrase
    public static List<String> processQuery(String query) throws RocksDBException {

        var termList = parseQuery(query);
        HashMap<String, HashMap<String, Integer>> termFreq = new HashMap<>(); // map term -> (map page -> freq)

        for (String term : termList) {
            HashMap<String, Integer> freq;
            if (!term.contains(" ")) {      // actually a single word
                freq = computeTermFreq_word(term, INVERTED);
            } else {                        // a phrase containing multiple keywords
                freq = computeTermFreq_phrase(term, INVERTED);
            }
            termFreq.put(term, freq);
        }

        List<Double> query_vector = new ArrayList<>();
        List<Integer> DF = computeDF(termList, termFreq);

        HashSet<String> candidatePageSet = new HashSet<>();
        for (String term : termList) {
            query_vector.add(1.0);    // all terms have weight of 1 in the query
            candidatePageSet.addAll(termFreq.get(term).keySet());
        }

        // IDF
        List<Double> IDF = computeIDF(DF, TOTAL_NUM_PAGES);

        // Page vectors
        HashMap<String, List<Double>> page_vector = compute_pageVectors(termList, termFreq, candidatePageSet, IDF);

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

    private static HashMap<String, List<Double>> compute_pageVectors(List<String> termList, HashMap<String, HashMap<String, Integer>> termFreq, HashSet<String> candidatePageSet, List<Double> IDF) throws RocksDBException {
        HashMap<String, List<Double>> page_vector = new HashMap<>();
        for (String page : candidatePageSet) {
            List<Double> vector = new ArrayList<>();
            for (int termIndex = 0; termIndex < termList.size(); termIndex++) {
                String term = termList.get(termIndex);
                if (!termFreq.get(term).containsKey(page)) {
                    vector.add(0.0);
                    continue;
                }

                // compute weight of term in this page
                int tf_max = 0;
                var termFreqInPage = Repository.ForwardIndex.getMap_WordId_Positions(page);
                for (var freq : termFreqInPage.values()) {
                    tf_max = Math.max(freq.stream().max(Integer::compareTo).orElse(0), tf_max);
                }
                double weight = (termFreq.get(term).get(page) / (double) tf_max) * IDF.get(termIndex);
                vector.add(weight);
            }
            page_vector.put(page, vector);
        }
        return page_vector;
    }

    protected static List<Double> computeIDF(List<Integer> DF, int totalNum_pages) {
        List<Double> IDF = new ArrayList<>();
        for (int i = 0; i < DF.size(); i++) {
            double idf = (Math.log(totalNum_pages) - Math.log(DF.get(i))) / Math.log(2);
            IDF.add(idf);
        }
        return IDF;
    }


    protected static List<Integer> computeDF(List<String> termList, HashMap<String, HashMap<String, Integer>> termFreq) {
        List<Integer> DF = new ArrayList<>();
        for (String term : termList) {
            DF.add(termFreq.get(term).size());
        }
        return DF;
    }

    // returns list of terms, e.g. "hong kong" university  -> List("hong kong", "university")
    public static List<String> parseQuery(String query) {
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
        return phraseList;
    }

    public static List<String> getWords_in_phrase(String phrase) {
        return List.of(phrase.split(" "));
    }

    // returns map(pageId -> tf of a single word)
    public static HashMap<String, Integer> computeTermFreq_word(String word, HashMap<String, HashMap<String, List<Integer>>> inverted) {
        String wordId = String.valueOf(word.hashCode());

        var map_page_posList = inverted.get(wordId);
        if (map_page_posList == null || map_page_posList.isEmpty()) return new HashMap<>();

        HashMap<String, Integer> map_page_freq = new HashMap<>();
        for (String page : map_page_posList.keySet()) {
            var posList = map_page_posList.get(page);
            map_page_freq.put(page, posList.size());
        }

        return map_page_freq;
    }

    // returns phrase frequency of each page that contains it
    public static HashMap<String, Integer> computeTermFreq_phrase(String phrase, HashMap<String, HashMap<String, List<Integer>>> inverted) {
        var wordList = getWords_in_phrase(phrase);
//        var invertedIndex = new HashMap<>(invertedFile);    // for testing

        var invertedIndex = new HashMap<String, HashMap<String, List<Integer>>>();   // relevant entries in inverted
        for (String word : wordList) {
//            String wordId = Repository.Word.getWordId(word);
            String wordId = String.valueOf(word.hashCode());
            if (wordId == null) {   // no page contains the phrase
                return new HashMap<>();
            }
            var temp = inverted.get(wordId);
            invertedIndex.put(word, temp);
        }


        List<Set<String>> listOfPages = new ArrayList<>();      // pages that contain any of the words
        for (var map_page_posList : invertedIndex.values()) {
            listOfPages.add(map_page_posList.keySet());
        }

        HashSet<String> pages_contain_allWords = getIntersection(listOfPages);
        if (pages_contain_allWords.isEmpty()) return new HashMap<>();

        // decrement positions
        for (int i = 1; i < wordList.size(); i++) {
            var map_page_posList = invertedIndex.get(wordList.get(i));
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
            for (String word : wordList) {
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
