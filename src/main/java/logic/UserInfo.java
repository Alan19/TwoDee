package logic;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * A class for handling the retrieval of user information from users.yml
 */
public class UserInfo {

    private UserList userList;

    public UserInfo() {
        try {
            userList = new Gson().fromJson(new JsonReader(new FileReader("resources/users.json")), UserList.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Returns the string of the sheet ID if the user is a match. If not, return null.
    public String getDocID(String potentialUserID) {
        if (getUsers().contains(potentialUserID)) {
            for (User user : userList.getUsers()) {
                if (user.getUserid().toString().equals(potentialUserID)){
                    return user.getSid();
                }
            }
        }
        return null;
    }

    //Gets all the users as an ArrayList of their user IDs
    public ArrayList<String> getUsers() {
        ArrayList<String> userIDs = new ArrayList<>();
        for (User user : userList.getUsers()) {
            userIDs.add(user.getUserid().toString());
        }
        return userIDs;
    }

}
