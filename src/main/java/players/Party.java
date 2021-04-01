package players;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Party {
    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("members")
    @Expose
    private List<PartyMember> members;

    @SerializedName("channel")
    @Expose
    private long channel;

    public Party(String name, List<PartyMember> members) {
        super();
        this.name = name;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PartyMember> getMembers() {
        return members;
    }

    public void setMembers(List<PartyMember> members) {
        this.members = members;
    }

    public long getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }
}
