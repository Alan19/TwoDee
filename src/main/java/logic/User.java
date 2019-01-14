package logic;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("userid")
    @Expose
    private Long userid;
    @SerializedName("col")
    @Expose
    private String col;
    @SerializedName("sid")
    @Expose
    private String sid;

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public User withUserid(Long userid) {
        this.userid = userid;
        return this;
    }

    public String getCol() {
        return col;
    }

    public void setCol(String col) {
        this.col = col;
    }

    public User withCol(String col) {
        this.col = col;
        return this;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public User withSid(String sid) {
        this.sid = sid;
        return this;
    }

}
