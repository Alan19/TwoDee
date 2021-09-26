package util;

/**
 * A class that contains 2 strings to be used as an Embed's title and content
 */
public class EmbedField {
    private String title;
    private String content;

    public EmbedField() {
        title = "";
        content = "";
    }

    public EmbedField(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void appendContent(String content) {
        this.content += content;
    }
}
