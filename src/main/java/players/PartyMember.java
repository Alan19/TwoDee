package players;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PartyMember {
    @SerializedName("discord_id")
    @Expose
    private long discordId;

    @SerializedName("sheet_id")
    @Expose
    private String sheetId;

    public PartyMember(Integer discordId, String sheetId) {
        this.discordId = discordId;
        this.sheetId = sheetId;
    }

    public long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(Integer discordId) {
        this.discordId = discordId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }
}
