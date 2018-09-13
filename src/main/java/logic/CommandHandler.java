package logic;

import com.google.api.services.sheets.v4.model.ValueRange;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import sheets.SheetsQuickstart;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class CommandHandler {

    private TextChannel channel;
    private String message;
    private MessageAuthor author;

    public CommandHandler(String content, MessageAuthor author, TextChannel channel) {
        message = content;
        this.author = author;
        this.channel = channel;
    }

    //Checks to see if any parameters are words to find appropriate replacements in the Google doc
    private void handleCommand() {
        String[] messageParams = message.split(" ");
        for (int i = 0; i < messageParams.length; i++) {
            //If a parameter is a string, look into sheets for appropriate dice
            if (messageParams[i].chars().allMatch(Character::isLetter)){
                try {
                    SheetsQuickstart characterInfo = new SheetsQuickstart(author.getIdAsString());
                    messageParams[i] = retrieveDice(messageParams[i].toLowerCase(), characterInfo.getResult());

                } catch (IOException | GeneralSecurityException e) {
                    new MessageBuilder()
                            .setContent("Cannot retrieve spreadsheet!")
                            .send(channel);
                    e.printStackTrace();
                }
            }
        }
    }

    private String retrieveDice(String param, ValueRange result) {
        List<List<Object>> values = result.getValues();
        for (List<Object> skill : values) {
            if (skill.size() == 2 && validSkill((String) skill.get(0), param)){
                Integer skillVal = Integer.parseInt((String) skill.get(1));
                //Reduce dice to facets % 12 d12 dice and the remainder if a dice is over d12
                if (skillVal > 12){
                    return "d12";
                }
                return "d" + skill.get(1);
            }
        }
        return null;
    }

    private static boolean validSkill(String skillName, String param) {
        String skill = skillName.replaceAll("[\u0000-\u001f]", "").toLowerCase();
        return skill.equals(param);
    }
}
