package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class RetrievedDocument extends PageInfo {
    public double score;
    public HashMap<String, Integer> query_keyword_freq;
    public LinkedHashMap<String, Integer> top_document_keywords;

    private RetrievedDocument(String pageTitle, String url, String lastModifiedData, HashSet<String> childLinks, String pageSize, int max_termFreq) {
        super(pageTitle, url, lastModifiedData, childLinks, pageSize, max_termFreq);
    }

    public RetrievedDocument(PageInfo pageInfo, double score, HashMap<String, Integer> query_keyword_freq, LinkedHashMap<String, Integer> top_document_keywords) {
        this(pageInfo.pageTitle, pageInfo.url, pageInfo.lastModifiedDate, pageInfo.childLinks, pageInfo.pageSize, pageInfo.max_termFreq);
        this.parentLinks = pageInfo.parentLinks;
        this.score = score;
        this.query_keyword_freq = query_keyword_freq;
        this.top_document_keywords = top_document_keywords;
    }

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

    public String htmlString() {
        return String.format("%.2f", score) + "&emsp;" + pageTitle + "<br />" +
                "&emsp;&emsp;" + url + "<br />" +
                "&emsp;&emsp;" + lastModifiedDate + ", " + pageSize + "<br />" +
                "&emsp;&emsp;" + "Terms matched:\t\t\t\t\t" + query_keyword_freq + "<br />" +
                "&emsp;&emsp;" + "Document most frequent words:\t" + top_document_keywords + "<br />" +
                "&emsp;&emsp;Parent Links" + "<br />" +
                "&emsp;&emsp;" + parentLinks.toString() + "<br />" +
                "&emsp;&emsp;Child Links" + "<br />" +
                "&emsp;&emsp;" + childLinks.toString() + "<br />" +
                "&emsp;=======================================================<br />";
    }

}
