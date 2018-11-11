package logic;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

/**
 * A class for handling the retrieval of user information from users.yml
 */
public class UserInfo {

    private Map playerList;

    public UserInfo() {
        try {
            YamlReader playerInfo = new YamlReader(new FileReader("resources/users.yaml"));
            playerList = (Map) playerInfo.read();
        } catch (FileNotFoundException | YamlException e) {
            e.printStackTrace();
        }
    }

    //Returns the string of the document ID if the user is a match. If not, return null.
    public String getDocID(String potentialUserID) {
        if (getUsers().contains(potentialUserID)) {
            Map playerRecord = (Map) playerList.get(potentialUserID);
            return (String) playerRecord.get("sid");
        }
        return null;
    }

    //Gets all the users as an ArrayList of their user IDs
    public ArrayList<String> getUsers() {
        ArrayList<String> userIDs = new ArrayList<>();
        userIDs.addAll(playerList.keySet());
        return userIDs;
    }

}
