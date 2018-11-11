package logic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "discordID",
        "col",
        "sheetID"
})
public class YAMLUsers {

    @JsonProperty("discordID")
    private Long discordID;
    @JsonProperty("col")
    private String col;
    @JsonProperty("sheetID")
    private String sheetID;

    @JsonProperty("discordID")
    public Long getDiscordID() {
        return discordID;
    }

    @JsonProperty("discordID")
    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }

    @JsonProperty("col")
    public String getCol() {
        return col;
    }

    @JsonProperty("col")
    public void setCol(String col) {
        this.col = col;
    }

    @JsonProperty("sheetID")
    public String getSheetID() {
        return sheetID;
    }

    @JsonProperty("sheetID")
    public void setSheetID(String sheetID) {
        this.sheetID = sheetID;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("discordID", discordID).append("col", col).append("sheetID", sheetID).toString();
    }
}
