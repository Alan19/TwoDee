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

    @SerializedName("doom_pool")
    @Expose
    private final String doomPool;

    public Player(Integer discordId, String sheetId, String doomPool) {
        this.discordId = discordId;
        this.sheetId = sheetId;
        this.doomPool = doomPool;
    }

    public String getDoomPool() {
        return doomPool;
    }

    public long getDiscordId() {
        return discordId;
    }

    public String getSheetId() {
        return sheetId;
    }

}
