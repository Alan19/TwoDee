package statistics;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import pw.mihou.velen.interfaces.*;
import pw.mihou.velen.interfaces.routed.VelenRoutedOptions;
import rolling.DicePoolBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class StatisticsCommand implements VelenSlashEvent, VelenEvent {

    public static void setupStatisticsCommand(Velen velen) {
        final StatisticsCommand statisticsCommand = new StatisticsCommand();
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "dicepool", "The dice pool to roll with.", true));
        options.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "dicekept", "The number of dice kept. Keeps two dice by default.", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "nonephemeral", "Makes the statistics for the roll visible. Defaults to false.", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "devastating", "Makes dice that rolled max be treated as double its value. Defaults to false", false));
        VelenCommand.ofHybrid("statistics", "Generates the statistics for a dice pool!", velen, statisticsCommand, statisticsCommand)
                .addOptions(options.toArray(new SlashCommandOption[0]))
                .addShortcuts("s", "stat", "stats")
                .addFormats("statistics :[dicepool:of(string):hasMany()]")
                .attach();
    }

    private static String getContent(String dicePool, boolean devastating) {
        String output = "Here are the statistics for **" + dicePool + "**";
        if (devastating) {
            output += " on a **Devastating** roll :sparkles:";
        }
        return output;
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args, VelenRoutedOptions options) {
        // Add delete component
        String dicePool = String.join(" ", args);
        final DicePoolBuilder builder = new DicePoolBuilder(dicePool, s -> s, false);
        final CompletableFuture<EmbedBuilder> result = CompletableFuture.supplyAsync(() -> new StatisticsLogic(builder).generateEmbed());
        CompletableFuture<Message> statisticsPM = new MessageBuilder()
                .setContent("Sent you a PM with your statistics for ")
                .append(message.getContent(), MessageDecoration.BOLD)
                .append(" " + user.getMentionTag())
                .send(event.getChannel());
        result.thenCompose(builder1 -> new MessageBuilder().setContent("Here are the statistics for **" + dicePool).addEmbed(builder1).send(user)).thenAcceptBoth(statisticsPM, (message1, message2) -> message.delete());
    }

    @Override
    public void onEvent(SlashCommandCreateEvent originalEvent, SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Optional<String> dicePool = event.getArgumentStringValueByName("dicepool");
        final boolean devastating = event.getArgumentBooleanValueByName("devastating").orElse(false);
        if (dicePool.isPresent()) {
            final CompletableFuture<EmbedBuilder> result = CompletableFuture.supplyAsync(() -> new StatisticsLogic(new DicePoolBuilder(dicePool.get(), s -> s, devastating).withDiceKept(event.getArgumentLongValueByName("dicekept").map(Math::toIntExact).orElse(2))).generateEmbed());

            final boolean ephemeral = !event.getArgumentBooleanValueByName("nonephemeral").orElse(false);
            event.respondLater(ephemeral).thenAcceptBoth(result, (updater, embedBuilder) -> {
                InteractionOriginalResponseUpdater responseUpdater = updater.addEmbed(embedBuilder).setContent(getContent(dicePool.get(), devastating));
                responseUpdater.update();
            });
        } else {
            firstResponder.setContent("Dice pool not found!").setFlags(MessageFlag.EPHEMERAL).respond();
        }
    }
}
