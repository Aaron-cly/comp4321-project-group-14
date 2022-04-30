package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Subclass of PageInfo, which contains the scores, frequencies of the queried
 * terms and top keywords in the page
 */
public class RetrievedDocument extends PageInfo {
    public double score;
    public HashMap<String, Integer> query_keyword_freq;
    public LinkedHashMap<String, Integer> top_document_keywords;

    private RetrievedDocument(String pageTitle, String url, String lastModifiedData, HashSet<String> childLinks,
            String pageSize, int max_termFreq) {
        super(pageTitle, url, lastModifiedData, childLinks, pageSize, max_termFreq);
    }

    /**
     * Public Constructor for the retrievedDocument. Will call
     * {@link RetrievedDocument#RetrievedDocument(String, String, String, HashSet, String, int)}
     *
     * @param pageInfo              The pageInfo of the document
     * @param score                 The score of the document with respect to the
     *                              queried terms
     * @param query_keyword_freq    The frequencies of the queried words
     * @param top_document_keywords The keywords of the documents with highest
     *                              weight
     */
    public RetrievedDocument(PageInfo pageInfo, double score, HashMap<String, Integer> query_keyword_freq,
            LinkedHashMap<String, Integer> top_document_keywords) {
        this(pageInfo.pageTitle, pageInfo.url, pageInfo.lastModifiedDate, pageInfo.childLinks, pageInfo.pageSize,
                pageInfo.max_termFreq);
        this.parentLinks = pageInfo.parentLinks;
        this.score = score;
        this.query_keyword_freq = query_keyword_freq;
        this.top_document_keywords = top_document_keywords;
    }

    /**
     * Overriden toString() method for representation of the
     * {@link RetrievedDocument}, to be used for ouput in query_result.txt
     *
     * @return String representation of the {@link RetrievedDocument}
     */
    @Override
    public String toString() {
        return String.format("%.2f", score) + "\t" + pageTitle + '\n' +
                "\t\t" + url + '\n' +
                "\t\t" + lastModifiedDate + ", " + pageSize + '\n' +
                "\t\t" + "Terms matched:\t\t\t\t\t" + query_keyword_freq + '\n' +
                "\t\t" + "Document most frequent words:\t" + top_document_keywords + '\n' +
                "\t\tParent Links" + '\n' +
                "\t\t" + parentLinks.toString() + '\n' +
                "\t\tChild Links" + '\n' +
                "\t\t" + childLinks.toString() + '\n' +
                "\t=======================================================\n";
    }

    /**
     * Method for returning the string representation formatted in html for jsp
     * 
     * @return The htmlString representation of the {@link RetrievedDocument}
     */
    public String htmlString() {
        return "<div class='resultWrapper'>" +
                String.format("%.2f", score)
                + "<div class='heading'>" + "<a href='" + url + "''>" + pageTitle
                + "  </a></div>"
                + "<div class='url'>" + url + "</div>" +
                "<span class='date'>"
                + lastModifiedDate +
                " </span>" + pageSize + "<br />" +
                "Terms matched:\t\t\t\t\t" + query_keyword_freq + "<br />" +
                "Document most frequent words:\t" + top_document_keywords + "<br />" +
                "Parent Links" + "<br />" +
                parentLinks.stream().limit(30).collect(Collectors.toList()).toString() + "<br />" +
                "Child Links" + "<br />" +
                childLinks.stream().limit(30).collect(Collectors.toList()).toString() + "<br />" +
                "</div></br>";
    }

}
