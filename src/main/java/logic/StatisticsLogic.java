package logic;

import com.vdurmont.emoji.EmojiParser;
import dicerolling.DicePoolBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import statistics.GenerateStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class StatisticsLogic implements VelenSlashEvent, VelenEvent {
    public static CompletableFuture<Void> addCancelReactToMessage(Message sentMessage) {
        return sentMessage.addReaction(EmojiParser.parseToUnicode(":x:"));
    }

    public static void setupStatisticsCommand(Velen velen) {
        final StatisticsLogic statisticsLogic = new StatisticsLogic();
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "dicepool", "The dice pool to roll with.", true));
        options.add(SlashCommandOption.create(SlashCommandOptionType.INTEGER, "dicekept", "The number of dice kept. Keeps two dice by default.", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "nonephemeral", "Makes the statistics for the roll visible. Defaults to false.", false));
        VelenCommand.ofHybrid("statistics", "Generates the statistics for a dice pool!", velen, statisticsLogic, statisticsLogic).addOptions(options.toArray(new SlashCommandOption[0])).addShortcuts("s", "stat", "stats").setServerOnly(true, 468046159781429250L).attach();
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        String dicePool = String.join(" ", args);
        final DicePoolBuilder builder = new DicePoolBuilder(user, dicePool);
        final CompletableFuture<EmbedBuilder> result = CompletableFuture.supplyAsync(() -> new GenerateStatistics(builder).getResult());
        CompletableFuture<Message> statisticsPM = new MessageBuilder()
                .setContent("Sent you a PM with your statistics for ")
                .append(message.getContent(), MessageDecoration.BOLD)
                .append(" " + user.getMentionTag())
                .send(event.getChannel());
        result.thenCompose(builder1 -> user.sendMessage("Here are the statistics for **" + dicePool + "**:", builder1)).thenAcceptBoth(statisticsPM, (message1, message2) -> message.delete());
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Optional<String> dicepool = event.getOptionStringValueByName("dicepool");
        if (dicepool.isPresent()) {
            final DicePoolBuilder builder = new DicePoolBuilder(user, dicepool.get()).withDiceKept(event.getOptionIntValueByName("dicekept").orElse(2));
            final CompletableFuture<EmbedBuilder> result = CompletableFuture.supplyAsync(() -> new GenerateStatistics(builder).getResult());
            InteractionImmediateResponseBuilder responseBuilder = firstResponder.setContent("Here are the statistics for **" + dicepool.get() + "**:");
            if (!event.getOptionBooleanValueByName("nonephemeral").orElse(false)) {
                responseBuilder = responseBuilder.setFlags(MessageFlag.EPHEMERAL);
            }
            responseBuilder.respond().thenAcceptBoth(result, (updater, statisticsEmbed) -> updater.addComponents(ActionRow.of(Button.primary("delete-stats", EmojiParser.parseToUnicode(":x:")))).addEmbeds(statisticsEmbed).update());
        }
        else {
            firstResponder.setContent("Dice pool not found!").setFlags(MessageFlag.EPHEMERAL).respond();
        }
    }
}
