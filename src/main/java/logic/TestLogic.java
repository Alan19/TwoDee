package logic;

import dicerolling.DicePoolBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;

import java.util.List;

public class TestLogic implements VelenEvent, VelenSlashEvent {
    public static void setTestCommand(Velen velen) {
        TestLogic testLogic = new TestLogic();
        List<SlashCommandOption> options = RollLogic.getRollCommandOptions();
        VelenCommand.ofHybrid("test", "Rolls some dice with opportunities disabled!", velen, testLogic, testLogic).addOptions(options.toArray(new SlashCommandOption[0])).addShortcuts("t").attach();
    }


    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        String dicePool = String.join(" ", args);

        //Variables containing getResults information
        DicePoolBuilder builder = new DicePoolBuilder(user, dicePool).withOpportunity(false);
        RollLogic.handleTextCommandRoll(event, user, builder);

    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final String dicePool = event.getOptionStringValueByName("dicepool").orElse("");
        final Integer discount = event.getOptionIntValueByName("discount").orElse(0);
        final Integer diceKept = event.getOptionIntValueByName("dicekept").orElse(2);
        final Boolean enhanceable = event.getOptionBooleanValueByName("enhanceable").orElse(null);

        RollLogic.handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, false, target);
    }

}
