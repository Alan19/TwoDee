package roles;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Player {
    @SerializedName("discord_id")
    @Expose
    private final long discordId;

    @SerializedName("sheet_id")
    @Expose
    private final String sheetId;

    public Player(Integer discordId, String sheetId) {
        this.discordId = discordId;
        this.sheetId = sheetId;
    }

    public long getDiscordId() {
        return discordId;
    }

    public String getSheetId() {
        return sheetId;
    }

}
