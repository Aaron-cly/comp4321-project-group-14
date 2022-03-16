package repository;

import jdk.jshell.spi.ExecutionControl.NotImplementedException;

import java.util.HashMap;

public class Repository {

    public static class Word {

        public static String getWord(int wordId) {
            // TODO

            // return word of given wordId
            return "";
        }

        public static int insertWord(String word) {
            // TODO

            // insert new word to db
            // return the wordId of the inserted word
            return 0;
        }

        public static int getWordId(String word) {
            // TODO

            // return wordId of the given word
            return 0;
        }

    }

    public static class Page {

        public static String getPageUrl(int pageId) {
            // TODO

            // return url of the given pageId
            return "";
        }

        public static int insertPage(String url) {
            // TODO
            // return pageId of the newly inserted url
            return 0;
        }

        public static int getPageId(String url) {
            //TODO
            //return pageId of the given url
            return 0;
        }

    }

    public static class ForwardFrequency {
        public static HashMap<Integer, Integer> getMap_Word_Freq(int pageId) {
            // return the freq map of a page
            return null;
        }
    }
}
