package logic;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenEvent;
import pw.mihou.velen.interfaces.VelenSlashEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class StopLogic implements VelenSlashEvent, VelenEvent {
    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        scheduleShutdown(event.getApi());
        event.getChannel().sendMessage(getShutdownEmbed(user));
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        scheduleShutdown(event.getApi());
        firstResponder.addEmbed(getShutdownEmbed(user)).respond();
    }

    private EmbedBuilder getShutdownEmbed(User user) {
        return new EmbedBuilder().setDescription("TwoDee is shutting down").setFooter("Requested by " + user, user.getAvatar());
    }

    private void scheduleShutdown(DiscordApi api) {
        System.out.println("TwoDee is shutting down...");
        api.getThreadPool()
                .getScheduler()
                .schedule(() -> System.exit(0), 5, TimeUnit.SECONDS);
    }
}
