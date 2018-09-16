package logic;

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
                .setColor(new Color(doomVal, doomVal, doomVal));
    }

    public int getDoom() {
        try {
            prop.load(new FileInputStream("src/main/resources/bot.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Integer.parseInt(prop.getProperty("doom"));
    }

    public EmbedBuilder setDoom(int newDoom) {
        prop.setProperty("doom", String.valueOf(newDoom));
        try {
            prop.store(new FileOutputStream("src/main/resources/bot.properties"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generateDoomEmbed();
    }

}