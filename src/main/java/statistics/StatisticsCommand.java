package statistics;

import io.vavr.control.Try;
import org.javacord.api.entity.message.Message;
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
import statistics.strategies.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class StatisticsCommand implements VelenSlashEvent, VelenEvent {

    public static final String NONEPHEMERAL = "nonephemeral";
    public static final String DICEPOOL = "dicepool";

    public static void setupStatisticsCommand(Velen velen) {
        final StatisticsCommand statisticsCommand = new StatisticsCommand();
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.STRING, DICEPOOL, "The dice pool to roll with.", true));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, NONEPHEMERAL, "Makes the statistics for the roll visible. Defaults to false.", false));
        VelenCommand.ofHybrid("statistics", "Generates the statistics for a dice pool!", velen, statisticsCommand, statisticsCommand)
                .addOptions(options.toArray(new SlashCommandOption[0]))
                .addShortcuts("s", "stat", "stats")
                .addFormats("statistics :[dicepool:of(string):hasMany()]")
                .attach();
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args, VelenRoutedOptions options) {
        // Add delete component
        String dicePool = String.join(" ", args);
        final EmbedBuilder result = getStatisticsEmbed(new DicePool(dicePool));
        user.sendMessage("Here are the statistics for **" + dicePool, result);
    }

    @Override
    public void onEvent(SlashCommandCreateEvent originalEvent, SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        boolean nonephemeral = !event.getArgumentBooleanValueByName(NONEPHEMERAL).orElse(false);
        CompletableFuture<InteractionOriginalResponseUpdater> updaterFuture = event.respondLater(nonephemeral);
        Try<DicePool> dicePoolTry = Try.of(() -> event.getArgumentStringValueByName(DICEPOOL).orElseThrow(() -> new IllegalArgumentException("Dice pool not found!")))
                .mapTry(DicePool::new)
                .onFailure(throwable -> firstResponder.addEmbed(new EmbedBuilder().setTitle("Error when running command!").setDescription(throwable.getMessage())).respond());
        if (dicePoolTry.isSuccess()) {
            DicePool dicePool = dicePoolTry.get();
            EmbedBuilder results = getStatisticsEmbed(dicePool);
            updaterFuture.thenAccept(updater -> updater.addEmbed(results).update());
        }
    }

    private static EmbedBuilder getStatisticsEmbed(DicePool dicePool) {
        List<ResultStrategy> resultStrategies = List.of(
                new SumStrategy(),
                new DifficultyStrategy(),
                new DoomStrategy(),
                new OpportunityStrategy(),
                new StatisticsStrategy()
        );
        return Try.of(() -> CompletableFuture.supplyAsync(() -> new DicePoolStatistics(dicePool)).get(30, TimeUnit.SECONDS))
                .fold(throwable -> new EmbedBuilder().setTitle("Error when running command!").setDescription(throwable.getMessage()),
                        statistics -> {
                            EmbedBuilder embed = new EmbedBuilder().setTitle(MessageFormat.format("Statistics for **{0}**", dicePool)).setDescription("Statistics formatting WIP");
                            resultStrategies.stream()
                                    .flatMap(resultStrategy -> resultStrategy.executeStrategy(statistics.getResults(), statistics.getResults().values().stream().mapToLong(value -> value).sum()).stream())
                                    .forEach(embedField -> embed.addInlineField(embedField.getTitle(), embedField.getContent()));
                            return embed;
                        });

    }
}
