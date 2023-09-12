package statistics.opposed;

import io.vavr.control.Try;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;
import rolling.BuildablePoolResult;
import rolling.DicePoolBuilder;
import statistics.StatisticsLogic;
import statistics.opposed.strategy.ExtraordinaryFailureStrategy;
import statistics.opposed.strategy.ExtraordinarySuccessStrategy;
import statistics.opposed.strategy.OpposedStrategy;
import statistics.opposed.strategy.SuccessStrategy;

import java.util.HashMap;
import java.util.List;

public class OpposedLogic implements VelenSlashEvent {
    public static void setupOpposedCommand(Velen velen) {
        final OpposedLogic statisticsCommand = new OpposedLogic();
        List<SlashCommandOption> commandOptions = List.of(
                SlashCommandOption.create(SlashCommandOptionType.STRING, "attacker-pool", "The dice pool of the attacker. Wins on ties", true),
                SlashCommandOption.create(SlashCommandOptionType.STRING, "defender-pool", "The dice pool of the defender. Loses on ties", true),
                SlashCommandOption.create(SlashCommandOptionType.LONG, "attacker-dice-kept", "The number of dice kept for the attacker. Defaults to 2.", false),
                SlashCommandOption.create(SlashCommandOptionType.LONG, "defender-dice-kept", "The number of dice kept for the defender. Defaults to 2.", false),
                SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "nonephemeral", "Makes the statistics for the roll visible. Defaults to false.", false)
        );
        SlashCommandOption[] options = {SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "attacker", "Calculates statistics for opposed checks where you are the defender", commandOptions),
                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "defender", "Calculates statistics for opposed checks where you are the attacker", commandOptions)};
        VelenCommand.ofSlash("opposed", "Generates the statistics for an opposed check!", velen, statisticsCommand)
                .addOptions(options)
                .addShortcuts("s", "stat", "stats")
                .attach();
    }

    public static EmbedBuilder getOpposedCheckStatsEmbed(boolean attacker, String attackerPool, String defenderPool, HashMap<BuildablePoolResult, Long> attackerResults, HashMap<BuildablePoolResult, Long> defenderResults) {

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Opposed check statistics");
        String pools;
        if (attacker) {
            pools = "**%s** vs. **%s**".formatted(attackerPool, defenderPool);
        } else {
            pools = "**%s** vs. **%s**".formatted(defenderPool, attackerPool);
        }
        embedBuilder.setDescription("Here are the statistics for an opposed roll of " + pools + " as a" + (attacker ? "n attacker" : " defender"));
        embedBuilder.addField("Success Rate", OpposedStrategy.generatePercentage(new SuccessStrategy().getProbability(attacker, attackerResults, defenderResults)));
        embedBuilder.addField("Extraordinary Success Rate", OpposedStrategy.generatePercentage(new ExtraordinarySuccessStrategy().getProbability(attacker, attackerResults, defenderResults)));
        embedBuilder.addField("Extraordinary Failure Rate", OpposedStrategy.generatePercentage(new ExtraordinaryFailureStrategy().getProbability(attacker, attackerResults, defenderResults)));
        return embedBuilder;
    }

    @Override
    public void onEvent(SlashCommandCreateEvent originalEvent, SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        SlashCommandInteractionOption commandType = event.getOptionByIndex(0).orElseThrow(IllegalArgumentException::new);
        String type = commandType.getName();

        Try<HashMap<BuildablePoolResult, Long>> attackerStats = commandType.getOptionByName("attacker-pool")
                .flatMap(SlashCommandInteractionOption::getStringValue)
                .map(pool -> new DicePoolBuilder(pool, s -> s, false))
                .map(StatisticsLogic::new)
                .map(StatisticsLogic::getResults)
                .orElseThrow(IllegalStateException::new);
        Try<HashMap<BuildablePoolResult, Long>> defenderStats = commandType.getOptionByName("defender-pool")
                .flatMap(SlashCommandInteractionOption::getStringValue)
                .map(pool -> new DicePoolBuilder(pool, s -> s, false))
                .map(StatisticsLogic::new)
                .map(StatisticsLogic::getResults)
                .orElseThrow(IllegalStateException::new);

        Try.sequence(List.of(attackerStats, defenderStats)).onSuccess(hashMaps -> event.respondLater().thenAccept(updater -> updater.addEmbed(getOpposedCheckStatsEmbed(type.equals("attacker"), commandType.getOptionByName("attacker-pool").flatMap(SlashCommandInteractionOption::getStringValue).orElseThrow(IllegalStateException::new), commandType.getOptionByName("defender-pool").flatMap(SlashCommandInteractionOption::getStringValue).orElseThrow(IllegalStateException::new), hashMaps.get(0), hashMaps.get(1))).update()));
    }


}
