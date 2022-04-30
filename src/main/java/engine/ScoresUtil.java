package engine;

import org.rocksdb.RocksDBException;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class ScoresUtil {

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

    /** Get the words in a phrase
     *
     * @param phrase  Phrase, from which words are to be extracted
     * @return  The words in the given phrase parameter
     */
    public static String[] getWords_in_phrase(String phrase) {
        return phrase.split(" ");
    }

    /** Class for computing idf, df, tf, similarity scores and cosSim, etc from the content of pages */
    public static class Content {
        /** Computes the similarity scores from the term frequencies of the content of pages that contains the query terms
         *  and returns a map of pages to its scores
         *  It does this by computing the df and idf and calculates the score based on CosSim similarity
         *
         * @param queryTerms  The terms queried
         * @param termFreq  The frequency terms of the content
         * @return  The map of pages to its scores, based on
         * @throws RocksDBException
         */
        protected static HashMap<String, Double> compute_scoresOnContent(String[] queryTerms, HashMap<String, HashMap<String, Integer>> termFreq) throws RocksDBException {
            double[] query_vector = new double[queryTerms.length];
            Arrays.fill(query_vector, 1.0);     // assume all terms have equal weight

            var DF = computeDF(queryTerms, termFreq);

            HashSet<String> candidatePageSet = new HashSet<>(); // pages that contain any of the terms
            for (String term : queryTerms) {
                candidatePageSet.addAll(termFreq.get(term).keySet());
            }

            // IDF
            var IDF = computeIDF(DF);

            // Page vectors
            HashMap<String, double[]> doc_vectors = compute_docVectors(queryTerms, termFreq, candidatePageSet, IDF);

            // cosine sim scores
            HashMap<String, Double> scores = compute_CosSimScores(doc_vectors, query_vector);

            return scores;
        }

        /**
         * Computes the CosSim scores of pages with the query terms
         * @param page_vectors  Map from candidate pages to its weight vectors
         * @param query_vector  The weights of the query terms
         * @return  The CosSim
         */
        protected static HashMap<String, Double> compute_CosSimScores(HashMap<String, double[]> page_vectors, double[] query_vector) {
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

                double score = (doc_length == 0)
                        ? 0
                        : inner / (doc_length * query_length);

                pages_score.put(page, score);
            }
            return pages_score;
        }

        /** Calculates the vector length from a vector
         *
         * @param vector  The vector, from which length is calculated
         * @return  The vector length
         */
        protected static double compute_vectorLength(double[] vector) {
            double length = 0;
            for (var weight : vector) {
                length += Math.pow(weight, 2);
            }
            return Math.sqrt(length);
        }

        /** Computes the weight vectors of the candidate page set. It does this by looking over all pages in the candidate page set
         * and computes the tfmax and calculates the weight of the page from the tfmax the tf of the term and its idf.
         *
         * @param terms  The query terms
         * @param termFreq  The term frequencies
         * @param candidatePageSet  The candidate pages
         * @param IDF  IDF of the term parameter
         * @return  The weight vectors of the candidate page set
         * @throws RocksDBException
         */
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

        /** Computes Idf vector from a DF vector
         *
         * @param DF  The DF vector, from which IDF is to be derived
         * @return  The IDF vector of the given DF vector parameter
         */
        protected static double[] computeIDF(int[] DF) {
            int totalNum_pages = Repository.Page.getTotalNumPage();

            double[] IDF = new double[DF.length];
            for (int i = 0; i < DF.length; i++) {
                double idf = (Math.log(totalNum_pages) - Math.log(DF[i])) / Math.log(2);
                IDF[i] = idf;
            }
            return IDF;
        }

        /** Computes the DF of the terms from the term frequencies
         *
         * @param terms  The terms, from which DF is to be calculated
         * @param termFreq  The term frequencies of the terms
         * @return  The DF of the terms
         */
        protected static int[] computeDF(String[] terms, HashMap<String, HashMap<String, Integer>> termFreq) {
            int[] DF = new int[terms.length];
            for (int i = 0; i < terms.length; i++) {
                DF[i] = termFreq.get(terms[i]).size();
            }
            return DF;
        }

        /** Computes the term frequencies of a given word. It does this by getting the mapping of the pages to its wordPos list of the word and
         *  calculates the term frequencies
         *
         * @param word  The word, for which term frequencies are derived
         * @return
         */
        public static HashMap<String, Integer> computeTermFreq_word(String word) {
            String wordId = Repository.Word.getWordId(word);

            var map_pageId_wordPosList = Repository.InvertedIndex.getMap_pageId_wordPosList(wordId);
            if (map_pageId_wordPosList == null || map_pageId_wordPosList.isEmpty()) return new HashMap<>();

            HashMap<String, Integer> termFreq = new HashMap<>();
            for (String pageId : map_pageId_wordPosList.keySet()) {
                var posList = map_pageId_wordPosList.get(pageId);
                termFreq.put(pageId, posList.size());
            }

            return termFreq;
        }

        // returns phrase frequency of each page that contains it
        /** Computes the phrase frequencies of a given word. It first gets the words in the phrase and then gets the mapping of the pages
         *  to its wordPos list of the word and
         *  calculates the frequencies
         *
         * @param phrase  The phrase, for which frequencies are derived
         * @return
         */
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
                    var posList = invertedIndex.get(word).getOrDefault(pageId, new ArrayList<>());
                    positionList.add(new HashSet<>(posList));
                }
                HashSet<Integer> posIntersection = getIntersection(positionList);
                if (!posIntersection.isEmpty()) {
                    map_pageId_freq.put(pageId, posIntersection.size());
                }
            }

            return map_pageId_freq;
        }
    }

    /** Title class for computing scores and phrases */
    public static class Title {

        /** Computes Scores of the query terms with respect to the titles of pages
         *
         * @param queryTerms  The query terms
         * @return  Map from pageIds to their scores
         * @throws RocksDBException
         */
        protected static HashMap<String, Double> compute_scoresOnTitle(String[] queryTerms) throws RocksDBException {
            HashMap<String, Double> scores = new HashMap<>();  // score of a page title is the number matches of query terms in the title

            for (var term : queryTerms) {
                HashSet<String> pageSet;
                if (term.contains(" ")) {
                    pageSet = get_pagesWithPhraseInTitle(term);
                } else {
                    pageSet = get_pagesWithWordInTitle(term);
                }
                for (var pageId : pageSet) {
                    var currentScore = scores.getOrDefault(pageId, 0.0);
                    scores.put(pageId, currentScore + 1);
                }
            }
            return scores;
        }

        /** Gets all pageIds, whose pages contain the given word
         *
         * @param word  The target word
         * @return  The pageIds of all pages that contain the given word parameter
         */
        protected static HashSet<String> get_pagesWithWordInTitle(String word) {
            var wordId = Repository.Word.getWordId(word);
            var map_pageId_posList = Repository.InvertedIndex_Title.getMap_pageId_wordPosList(wordId);
            return (new HashSet<String>(map_pageId_posList.keySet()));
        }

        /** Gets all pageIds, whose pages contain the given phrase
         *
         * @param phrase  The target phrase
         * @return  The pageIds of all pages that contain the given phrase parameter
         */
        protected static HashSet<String> get_pagesWithPhraseInTitle(String phrase) throws RocksDBException {
            // a different approach from page content because title is very concise
            var wordArr = getWords_in_phrase(phrase);
            HashSet<String> return_pageSet = new HashSet<>();
            HashSet<String> candidate_pageSet = new HashSet<>();

            for (var word : wordArr) {      // get candidate pages
                var wordId = Repository.Word.getWordId(word);
                var map_pageId_posList = Repository.InvertedIndex_Title.getMap_pageId_wordPosList(wordId);
                candidate_pageSet.addAll(map_pageId_posList.keySet());
            }

            for (var pageId : candidate_pageSet) {  // get the real pages that have titles with the phrase from the candidates
                var map_wordId_posList = Repository.ForwardIndex_Title.getMap_WordId_Positions(pageId);
                String firstWord = wordArr[0];
                String firstWord_id = Repository.Word.getWordId(firstWord);
                if (!map_wordId_posList.containsKey(firstWord_id)) continue;    // does not even contain the first word in query phrase
                int firstWordIndex = map_wordId_posList.get(firstWord_id).get(0);

                boolean isConsecutive = true;
                for (int wordIndex = 1; wordIndex < wordArr.length; wordIndex++) {
                    String word = wordArr[wordIndex];
                    String wordId = Repository.Word.getWordId(word);
                    if (map_wordId_posList.containsKey(wordId)) {
                        int thisWordIndex = map_wordId_posList.get(wordId).get(0);
                        if (thisWordIndex - wordIndex != firstWordIndex) {
                            isConsecutive = false;
                            break;
                        }
                    } else {
                        isConsecutive = false;
                        break;
                    }

                }
                if (isConsecutive) {
                    return_pageSet.add(pageId);
                }
            }

            return return_pageSet;
        }
    }
}
