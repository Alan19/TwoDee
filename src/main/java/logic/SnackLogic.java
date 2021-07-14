package logic;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import util.RandomColor;

import java.util.List;

public class SnackLogic implements VelenSlashEvent, VelenEvent {
    public static void setupSnackCommand(Velen velen) {
        SnackLogic snackLogic = new SnackLogic();
        VelenCommand.ofHybrid("snack", "Gives you a snack!", velen, snackLogic, snackLogic);
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        event.getChannel().sendMessage(getSnackEmbed(user));
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        firstResponder.addEmbed(getSnackEmbed(user)).respond();
    }

    private EmbedBuilder getSnackEmbed(User user) {
        return new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setAuthor(user)
                .setTitle("A snack for " + user)
                .setDescription("Here is a cookie!")
                .setImage("https://upload.wikimedia.org/wikipedia/commons/5/5c/Choc-Chip-Cookie.png");
    }
}
