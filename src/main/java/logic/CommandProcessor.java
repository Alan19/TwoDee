package logic;

import com.google.api.services.sheets.v4.model.ValueRange;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import sheets.SheetsQuickstart;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class CommandProcessor {
    private MessageAuthor author;
    private TextChannel channel;

    public CommandProcessor(MessageAuthor author, TextChannel channel) {
        this.author = author;
        this.channel = channel;
    }

    private boolean validSkill(String skillName, String param) {
        String skill = skillName.replaceAll("\\s+", "").toLowerCase();
        return skill.equals(param);
    }//Checks to see if any parameters are words to find appropriate replacements in the Google doc

    public String handleCommand(String command) {
        String convertedCommand = getConvertedCommand(command);
        if (convertedCommand == null) return null;
        System.out.println(convertedCommand);
        return convertedCommand;
    }

    private String getConvertedCommand(String command) {
        String[] paramArray = command.split(" ");
        for (int i = 0; i < paramArray.length; i++) {
            //If a parameter is a string, look into sheets for appropriate dice
            if (paramArray[i].chars().allMatch(Character::isLetter) && skillExists(paramArray, i)) {
                return null;
            }
        }
        return String.join(" ", paramArray);
    }//If the skill exists, renames array element and returns true. Otherwise, returns false.

    private boolean skillExists(String[] paramArray, int i) {
        try {
            SheetsQuickstart characterInfo = new SheetsQuickstart(author.getIdAsString());
            String change = retrieveDice(paramArray[i].toLowerCase(), characterInfo.getResult());
            //If skill is not found, kill function immediately
            if (change == null) {
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setAuthor(author)
                                .setDescription("Cannot find skill: " + paramArray[i])
                        )
                        .send(channel);
                return true;
            }
            paramArray[i] = change;

        } catch (IOException | GeneralSecurityException e) {
            new MessageBuilder()
                    .setContent("Cannot retrieve spreadsheet!")
                    .send(channel);
            e.printStackTrace();
        }
        return false;
    }//Convert a skill into a dice value (euphemanu -> d12)

    private String retrieveDice(String param, ValueRange result) {
        List<List<Object>> values = result.getValues();
        for (List<Object> skill : values) {
            if (skill.size() == 2 && validSkill((String) skill.get(0), param)) {
                Integer skillVal = Integer.parseInt((String) skill.get(1));
                return reduceDice(skill, skillVal);

            }
        }
        return null;
    }/*
    If a dice is over d12 reduce dice to facets % 12 d12 dice and the remainder as a dice if the remainder is greater
    than 2. For example (d16 -> d12 d4, d14 -> d12, d100 -> 8d12 d4)
     */

    private String reduceDice(List<Object> skill, Integer skillVal) {

        if (skillVal > 12) {
            StringBuilder pool = new StringBuilder();
            for (int i = 0; i < skillVal / 12; i++) {
                pool.append("d12 ");
            }
            if (skillVal % 12 > 2) {
                pool.append("d").append(skillVal % 12);
            }
            return pool.toString().trim();
        }
        return "d" + skill.get(1);
    }
}