package doom;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class DoomWriter {

    private Properties prop;

    public DoomWriter() {
        prop = new Properties();
        try {
            prop.load(new FileInputStream("resources/bot.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public EmbedBuilder addDoom(int doomVal) {
        int currentDoom = getDoom();
        int newDoomInt = currentDoom + doomVal;
        setDoom(newDoomInt);
        return generateDoomEmbed();
    }

    public EmbedBuilder generateDoomEmbed() {
        int doomVal = getDoom();
        return new EmbedBuilder()
                .setTitle("Doom!")
                .setDescription(String.valueOf(getDoom()))
                .setColor(new Color(Integer.max(doomVal, 255)));
    }

    public int getDoom() {
        return Integer.parseInt(prop.getProperty("doom"));
    }

    /**
     * Sets the doom on the properties file. If doom points would drop below 0, return an error.
     * @param newDoom
     * @return The embed with a new doom value
     */
    public EmbedBuilder setDoom(int newDoom) {
        if (newDoom < 0){
            return generateInvalidDoomEmbed();
        }
        else {
            prop.setProperty("doom", String.valueOf(newDoom));
            try {
                prop.store(new FileOutputStream("resources/bot.properties"), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return generateDoomEmbed();
        }
    }

    private EmbedBuilder generateInvalidDoomEmbed() {
        return new EmbedBuilder()
                .setTitle("Not enough doom points!");
    }

}