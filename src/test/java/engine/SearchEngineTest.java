package engine;

import model.PageInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rocksdb.RocksDBException;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class SearchEngineTest {
    @Test
    void test_queryParser() {
        String query = "\"hong kong\" university \"asia\"";
        String[] expected_termList = {"hong kong", "university", "asia"};

        assertArrayEquals(expected_termList, SearchEngine.parseQuery(query));

        query = "HK university no 1";
        String[] expected_termList2 = {"hk", "university", "no", "1"};

        assertArrayEquals(expected_termList2, SearchEngine.parseQuery(query));
    }

//    @Test
//    void test_getWords_in_phrase() {
//        assertEquals(List.of("hk"), SearchEngine.getWords_in_phrase("hk"));
//        assertEquals(List.of("hong", "kong", "u"), SearchEngine.getWords_in_phrase("hong kong u"));
//    }

    @BeforeAll
    static void setUp() {
//        Repository.openConnections();
    }

    static HashMap<String, HashMap<String, List<Integer>>> invertedFile;
    static HashMap<String, HashMap<String, List<Integer>>> fowardFile;
    static HashMap<String, HashMap<String, Integer>> termFreq;
    static String word1 = "word1";
    static String word2 = "word2";
    static String word3 = "word3";

    static final int TOTAL_NUM_DOCS = 4;
    static String d1 = "d1";
    static String d2 = "d2";
    static String d3 = "d3";
    static String d4 = "d4";

    static String d1_hash = String.valueOf(d1.hashCode());
    static String d2_hash = String.valueOf(d2.hashCode());
    static String d3_hash = String.valueOf(d3.hashCode());
    static String d4_hash = String.valueOf(d4.hashCode());

    static String w1_hash = String.valueOf(word1.hashCode());
    static String w2_hash = String.valueOf(word2.hashCode());
    static String w3_hash = String.valueOf(word3.hashCode());

    @BeforeEach
    void beforeEach() {
        invertedFile = new HashMap<>();
        termFreq = new HashMap<>();

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
        pageFreq_word3.put(d4_hash, List.of(1));

        invertedFile.put(w1_hash, pageFreq_word1);
        invertedFile.put(w2_hash, pageFreq_word2);
        invertedFile.put(w3_hash, pageFreq_word3);
    }

    static HashMap<String, HashMap<String, Integer>> get_termFreq(String query) {
        HashMap<String, HashMap<String, Integer>> termFreq = new HashMap<>();

        try (MockedStatic<Repository.Word> WORD = Mockito.mockStatic(Repository.Word.class);
             MockedStatic<Repository.InvertedIndex> INVERTED = Mockito.mockStatic(Repository.InvertedIndex.class)
        ) {
            WORD.when(() -> Repository.Word.getWordId(word1)).thenReturn(w1_hash);
            WORD.when(() -> Repository.Word.getWordId(word2)).thenReturn(w2_hash);
            WORD.when(() -> Repository.Word.getWordId(word3)).thenReturn(w3_hash);
            INVERTED.when(() -> Repository.InvertedIndex.getMap_pageId_wordPosList(w1_hash)).thenReturn(invertedFile.get(w1_hash));
            INVERTED.when(() -> Repository.InvertedIndex.getMap_pageId_wordPosList(w2_hash)).thenReturn(invertedFile.get(w2_hash));
            INVERTED.when(() -> Repository.InvertedIndex.getMap_pageId_wordPosList(w3_hash)).thenReturn(invertedFile.get(w3_hash));

            String[] terms = SearchEngine.parseQuery(query);

            for (String term : terms) {
                HashMap<String, Integer> freq;
                if (!term.contains(" ")) {      // actually a single word
                    freq = SearchEngine.computeTermFreq_word(term);
                } else {                        // a phrase containing multiple keywords
                    freq = SearchEngine.computeTermFreq_phrase(term);
                }
                termFreq.put(term, freq);
            }

        }
        return termFreq;
    }

    @Test
    void test_computeTermFreq() {

        String queryPhrase = "word1 word2 word3";

        try (MockedStatic<Repository.Word> WORD = Mockito.mockStatic(Repository.Word.class);
             MockedStatic<Repository.InvertedIndex> INVERTED = Mockito.mockStatic(Repository.InvertedIndex.class)
        ) {
            WORD.when(() -> Repository.Word.getWordId(word1)).thenReturn(w1_hash);
            WORD.when(() -> Repository.Word.getWordId(word2)).thenReturn(w2_hash);
            WORD.when(() -> Repository.Word.getWordId(word3)).thenReturn(w3_hash);
            INVERTED.when(() -> Repository.InvertedIndex.getMap_pageId_wordPosList(w1_hash)).thenReturn(invertedFile.get(w1_hash));
            INVERTED.when(() -> Repository.InvertedIndex.getMap_pageId_wordPosList(w2_hash)).thenReturn(invertedFile.get(w2_hash));
            INVERTED.when(() -> Repository.InvertedIndex.getMap_pageId_wordPosList(w3_hash)).thenReturn(invertedFile.get(w3_hash));

            HashMap<String, Integer> expectedFreq = new HashMap<>();
            expectedFreq.put(d1_hash, 4);
            expectedFreq.put(d2_hash, 3);
            expectedFreq.put(d3_hash, 3);
            assertEquals(expectedFreq, SearchEngine.computeTermFreq_word(word1));

            expectedFreq = new HashMap<>();
            expectedFreq.put(d1_hash, 2);
            expectedFreq.put(d3_hash, 1);
            assertEquals(expectedFreq, SearchEngine.computeTermFreq_phrase(queryPhrase));
        }

    }

    @Test
    void test_computeDF() {
        String[] terms = {word1, word2, word3};
        var termFreq = new HashMap<String, HashMap<String, Integer>>();
//        var w1_freq = new HashMap<String, Integer>();
//        w1_freq.put(d1_hash, invertedFile.get(w1_hash).get(d1_hash).size());
//        w1_freq.put(d2_hash, invertedFile.get(w1_hash).get(d2_hash).size());
//        w1_freq.put(d3_hash, invertedFile.get(w1_hash).get(d3_hash).size());
//
//        var w2_freq = new HashMap<String, Integer>();
//        w2_freq.put(d1_hash, invertedFile.get(w2_hash).get(d1_hash).size());
//        w2_freq.put(d4_hash, invertedFile.get(w2_hash).get(d4_hash).size());
//        w2_freq.put(d3_hash, invertedFile.get(w2_hash).get(d3_hash).size());
//
//        var w3_freq = new HashMap<String, Integer>();
//        w3_freq.put(d1_hash, invertedFile.get(w3_hash).get(d1_hash).size());
//        w3_freq.put(d2_hash, invertedFile.get(w3_hash).get(d2_hash).size());
//        w3_freq.put(d3_hash, invertedFile.get(w3_hash).get(d3_hash).size());
//
//        termFreq.put(w1_hash, w1_freq);
//        termFreq.put(w2_hash, w2_freq);
//        termFreq.put(w3_hash, w3_freq);

        for (String term : terms) {
            var term_pagePosList = invertedFile.get(String.valueOf(term.hashCode()));
            var docFreq = new HashMap<String, Integer>();
            for (String page : term_pagePosList.keySet()) {
                int freq = term_pagePosList.get(page).size();
                docFreq.put(page, freq);
            }
            termFreq.put(term, docFreq);
        }

        int[] expected = {3, 3, 4};
        assertArrayEquals(expected, SearchEngine.computeDF(terms, termFreq));

    }

    @Test
    void test_compute_pageVectors() throws RocksDBException {
        String[] terms = {word1, word2, word3};
        HashMap<String, HashMap<String, Integer>> termFreq = new HashMap<>();


        for (String term : terms) {
            var term_pagePosList = invertedFile.get(String.valueOf(term.hashCode()));
            var docFreq = new HashMap<String, Integer>();
            for (String page : term_pagePosList.keySet()) {
                int freq = term_pagePosList.get(page).size();
                docFreq.put(page, freq);
            }
            termFreq.put(term, docFreq);
        }

        HashSet<String> candidatePageSet = new HashSet<>(); // pages that contain any of the terms
        for (String term : terms) {
            candidatePageSet.addAll(termFreq.get(term).keySet());
        }


        var DF = SearchEngine.computeDF(terms, termFreq);
        var IDF = SearchEngine.computeIDF(DF, TOTAL_NUM_DOCS);

        try (
                MockedStatic<Repository.PageInfo> PAGEINFO = Mockito.mockStatic(Repository.PageInfo.class)
        ) {
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d1_hash)).thenReturn(new PageInfo("", "", "", new HashSet<>(), "", 4));
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d2_hash)).thenReturn(new PageInfo("", "", "", new HashSet<>(), "", 3));
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d3_hash)).thenReturn(new PageInfo("", "", "", new HashSet<>(), "", 5));
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d4_hash)).thenReturn(new PageInfo("", "", "", new HashSet<>(), "", 3));


            double epsilon = 0.001d;
            HashMap<String, double[]> expected = new HashMap<>();

            double[] d1 = {0.415, 0.311, 0};
            double[] d2 = {0.415, 0, 0};
            double[] d3 = {0.249, 0.415, 0};
            double[] d4 = {0, 0.415, 0};
            expected.put(d1_hash, d1);
            expected.put(d2_hash, d2);
            expected.put(d3_hash, d3);
            expected.put(d4_hash, d4);

            var vectors = SearchEngine.compute_docVectors(terms, termFreq, candidatePageSet, IDF);
            for (String page : vectors.keySet()) {
                for (int i = 0; i < terms.length; i++) {
                    assertTrue(Math.abs(expected.get(page)[i] - vectors.get(page)[i]) < epsilon);
                }
            }

        }
    }

    @Test
    void test_compute_scores() {
        double[] query_vector = {1, 1, 1};

        HashMap<String, double[]> doc_vectors = new HashMap<>();
        double[] d1 = {0.415, 0.311, 0};
        double[] d2 = {0.415, 0, 0};
        double[] d3 = {0.249, 0.415, 0};
        double[] d4 = {0, 0.415, 0};
        doc_vectors.put(d1_hash, d1);
        doc_vectors.put(d2_hash, d2);
        doc_vectors.put(d3_hash, d3);
        doc_vectors.put(d4_hash, d4);


        HashMap<String, Double> expected = new HashMap<>();
        expected.put(d1_hash, 0.808);
        expected.put(d2_hash, 0.577);
        expected.put(d3_hash, 0.792);
        expected.put(d4_hash, 0.577);

        double epsilon = 0.001d;

        var scores = SearchEngine.compute_scores(doc_vectors, query_vector);
        for (String page : doc_vectors.keySet()) {
            for (int i = 0; i < query_vector.length; i++) {
                assertTrue(Math.abs(expected.get(page) - scores.get(page)) < epsilon);
            }
        }
    }

    @Test
    void test_construct_output() throws RocksDBException {
        HashMap<String, Double> scores = new HashMap<>();
        scores.put(d1_hash, 0.808);
        scores.put(d2_hash, 0.577);
        scores.put(d3_hash, 0.792);
        scores.put(d4_hash, 0.578);

        String query = "word1 word2 word3";
        var termFreq = get_termFreq(query);

        try (
                MockedStatic<Repository.PageInfo> PAGEINFO = Mockito.mockStatic(Repository.PageInfo.class)
        ) {
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d1_hash)).thenReturn(new PageInfo("", d1, "", new HashSet<>(), "", 4));
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d2_hash)).thenReturn(new PageInfo("", d2, "", new HashSet<>(), "", 3));
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d3_hash)).thenReturn(new PageInfo("", d3, "", new HashSet<>(), "", 5));
            PAGEINFO.when(() -> Repository.PageInfo.getPageInfo(d4_hash)).thenReturn(new PageInfo("", d4, "", new HashSet<>(), "", 3));


            var outputList = SearchEngine.constructOutput(scores, termFreq);
            var expectedOrder = List.of(d1, d3, d4, d2);

            assertEquals(expectedOrder.size(), outputList.size());
            for (int i = 0; i < outputList.size(); i++) {
                assertEquals(expectedOrder.get(i), outputList.get(i).url);
            }
        }
    }
}
