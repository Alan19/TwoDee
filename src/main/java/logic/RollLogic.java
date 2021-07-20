package logic;

import dicerolling.DicePoolBuilder;
import dicerolling.RollHandlers;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
 */
public class RollLogic implements VelenSlashEvent, VelenEvent {
    public static void setupRollCommand(Velen velen) {
        // TODO Add desired tier option
        RollLogic rollLogic = new RollLogic();
        final List<SlashCommandOption> rollCommandOptions = getRollCommandOptions();
        rollCommandOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "opportunity", "Allows for opportunities on the roll. Defaults to true.", false));
        VelenCommand.ofHybrid("roll", "Rolls some dice!", velen, rollLogic, rollLogic).addOptions(rollCommandOptions.toArray(new SlashCommandOption[0])).addShortcuts("r").setServerOnly(true, 468046159781429250L).attach();

    }

    static List<SlashCommandOption> getRollCommandOptions() {
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "dicepool", "The dice pool to roll with.", true));
        options.add(SlashCommandOption.create(SlashCommandOptionType.INTEGER, "discount", "The number of plot points to discount (negative results in a plot point cost increase).", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.INTEGER, "dicekept", "The number of dice kept. Keeps two dice by default.", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "enhanceable", "Allows the roll to be enhanced after the roll.", false));
        return options;
    }


    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        String dicePool = String.join(" ", args);

        //Variables containing getResults information
        DicePoolBuilder builder = new DicePoolBuilder(user, dicePool).withOpportunity(true);
        RollHandlers.handleTextCommandRoll(event, user, builder);
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Boolean opportunity = event.getOptionBooleanValueByName("opportunity").orElse(true);
        final Integer discount = event.getOptionIntValueByName("discount").orElse(0);
        final Optional<String> dicePool = event.getOptionStringValueByName("dicepool");
        final Integer diceKept = event.getOptionIntValueByName("dicekept").orElse(2);
        final Optional<Boolean> enhanceable = event.getOptionBooleanValueByName("enhanceable");

        RollHandlers.handleSlashCommandRoll(event, firstResponder, dicePool, discount, diceKept, enhanceable, opportunity);
    }

}
