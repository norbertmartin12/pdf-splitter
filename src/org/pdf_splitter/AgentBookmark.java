package org.pdf_splitter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentBookmark {
    private static final String BULLETIN_PREFIX = "Bulletin_";
    private static final int FULLNAME = 1;
    private static final int MONTH = 2;
    private static final int YEAR = 3;
    private static final Pattern PATTERN = Pattern.compile("^(.*) (\\d{2})/(\\d{4})$");

    private String title;
    private int startAtPage = -1;
    private int endAtPage = -1;

    /**
     * @param title
     * @param startAtPage included
     * @param endAtPage
     */
    public AgentBookmark(String title, int startAtPage, int endAtPage) {
        this.title = title;
        this.startAtPage = startAtPage;
        this.endAtPage = endAtPage;
    }

    public String getStandardizedTitle() {
        Matcher matcher = PATTERN.matcher(title);
        if (matcher.matches()) {
            return BULLETIN_PREFIX + matcher.group(FULLNAME).replace(" ", "_").toUpperCase() + "_" + matcher.group(YEAR) + "-" + matcher.group(MONTH);
        }
        return defaultTitleFormat();
    }

    public String getStandardizedFolder() {
        Matcher matcher = PATTERN.matcher(title);
        if (matcher.matches()) {
            return matcher.group(FULLNAME).replace(" ", "_").toUpperCase();
        }
        return defaultTitleFormat();
    }

    private String defaultTitleFormat() {
        return title.replace(" ", "_").replace("/", "-").toUpperCase();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStartAtPage() {
        return startAtPage;
    }

    public void setStartAtPage(int startAtPage) {
        this.startAtPage = startAtPage;
    }

    public int getEndAtPage() {
        return endAtPage;
    }

    public void setEndAtPage(int endAtPage) {
        this.endAtPage = endAtPage;
    }

    @Override
    public String toString() {
        return "AgentBookmark [" + (title != null ? "title=" + title + ", " : "") + "startAtPage=" + startAtPage + ", endAtPage=" + endAtPage + "]";
    }

}
