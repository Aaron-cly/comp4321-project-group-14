package engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchEngineTest {
    @Test
    void test_queryParser() {
        String query = "\"hong kong\" university \"asia\"";
        var expected_termList = List.of("hong kong", "university", "asia");
        assertEquals(expected_termList, SearchEngine.parseQuery(query));


        query = "HK university no 1";
        expected_termList = List.of("hk", "university", "no", "1");
        assertEquals(expected_termList, SearchEngine.parseQuery(query));
    }

    @Test
    void test_getWords_in_phrase() {
        assertEquals(List.of("hk"), SearchEngine.getWords_in_phrase("hk"));
        assertEquals(List.of("hong", "kong", "u"), SearchEngine.getWords_in_phrase("hong kong u"));
    }

    @BeforeAll
    static void setUp() {
        Repository.openConnections();
    }

    static HashMap<String, HashMap<String, List<Integer>>> invertedFile;
    static HashMap<String, HashMap<String, List<Integer>>> fowardFile;
    static String d1_hash = String.valueOf("d1".hashCode());
    static String d2_hash = String.valueOf("d2".hashCode());
    static String d3_hash = String.valueOf("d3".hashCode());
    static String d4_hash = String.valueOf("d4".hashCode());

    static String w1_hash = String.valueOf("word1".hashCode());
    static String w2_hash = String.valueOf("word2".hashCode());
    static String w3_hash = String.valueOf("word3".hashCode());

    @BeforeEach
    void beforeEach() {
        invertedFile = new HashMap<>();

        HashMap<String, List<Integer>> pageFreq_word1 = new HashMap<>();
        pageFreq_word1.put(d1_hash, List.of(1, 5, 15, 150));
        pageFreq_word1.put(d2_hash, List.of(1, 5, 15));
        pageFreq_word1.put(d3_hash, List.of(13, 25, 99));

        HashMap<String, List<Integer>> pageFreq_word2 = new HashMap<>();
        pageFreq_word2.put(d1_hash, List.of(2, 6, 70));
        pageFreq_word2.put(d4_hash, List.of(1, 5, 15));
        pageFreq_word2.put(d3_hash, List.of(50, 26, 60, 200, 202));

        HashMap<String, List<Integer>> pageFreq_word3 = new HashMap<>();
        pageFreq_word3.put(d1_hash, List.of(3, 7, 88));
        pageFreq_word3.put(d2_hash, List.of(1, 5, 17, 46));
        pageFreq_word3.put(d3_hash, List.of(51, 27, 66));

        invertedFile.put(w1_hash, pageFreq_word1);
        invertedFile.put(w2_hash, pageFreq_word2);
        invertedFile.put(w3_hash, pageFreq_word3);
    }

    @Test
    void test_computeMap_pageId_phraseANDwordFrequency() {
        HashMap<String, Integer> expectedFreq = new HashMap<>();
        expectedFreq.put(d1_hash, 2);
        expectedFreq.put(d3_hash, 1);

        String queryPhrase = "word1 word2 word3";
        SearchEngine.initializeMock(invertedFile, new HashMap<>(), 4);
        assertEquals(expectedFreq, SearchEngine.computeTermFreq_phrase(queryPhrase, invertedFile));

        expectedFreq = new HashMap<>();
        expectedFreq.put(d1_hash, 4);
        expectedFreq.put(d2_hash, 3);
        expectedFreq.put(d3_hash, 3);

        assertEquals(expectedFreq, SearchEngine.computeTermFreq_word("word1", invertedFile));
    }

    @Test
    void test_computeDF() {
        var termList = List.of("word1", "word2");
        var termFreq = new HashMap<String, HashMap<String, Integer>>();
        var w1_freq = new HashMap<String, Integer>();
        w1_freq.put(d1_hash, 4);
        w1_freq.put(d2_hash, 3);
        w1_freq.put(d3_hash, 3);
        w1_freq.put(d4_hash, 3);

        var w2_freq = new HashMap<String, Integer>();
        w2_freq.put(d1_hash, 3);
        w2_freq.put(d2_hash, 3);
        w2_freq.put(d3_hash, 3);

        var w3_freq = new HashMap<String, Integer>();
        w3_freq.put(d1_hash, 3);
        w3_freq.put(d2_hash, 3);
        w3_freq.put(d3_hash, 3);

        termFreq.put(w1_hash, w1_freq);
        termFreq.put(w2_hash, w2_freq);
        termFreq.put(w3_hash, w3_freq);

        assertEquals(List.of(4,3), SearchEngine.computeDF(termList, termFreq));
    }

    @Test
    void test_compute_pageVectors() {
        var termList = List.of("word1", "word2", "word3");
        var termFreq = new HashMap<String, HashMap<String, Integer>>();

        for (String term : termList) {
            HashMap<String, Integer> freq;
            if (!term.contains(" ")) {      // actually a single word
                freq = SearchEngine.computeTermFreq_word(term, invertedFile);
            } else {                        // a phrase containing multiple keywords
                freq = SearchEngine.computeTermFreq_phrase(term, invertedFile);
            }
            termFreq.put(term, freq);
        }

        List<Double> query_vector = new ArrayList<>();
        List<Integer> DF = SearchEngine.computeDF(termList, termFreq);

        HashSet<String> candidatePageSet = new HashSet<>();
        for (String term : termList) {
            query_vector.add(1.0);    // all terms have weight of 1 in the query
            candidatePageSet.addAll(termFreq.get(term).keySet());
        }


    }


}
