package logic;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import pw.mihou.velen.interfaces.routed.VelenRoutedOptions;

import java.util.List;

public class TestLogic implements VelenEvent, VelenSlashEvent {
    public static void setTestCommand(Velen velen) {
        TestLogic testLogic = new TestLogic();
        List<SlashCommandOption> options = RollLogic.getRollCommandOptions();
        VelenCommand.ofHybrid("test", "Rolls some dice with opportunities disabled!", velen, testLogic, testLogic)
                .addOptions(options.toArray(new SlashCommandOption[0]))
                .addShortcuts("t")
                .attach();
    }


    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args, VelenRoutedOptions options) {
        String dicePool = String.join(" ", args);
        RollLogic.handleTextCommandRoll(user, event.getChannel(), dicePool, false);
    }

    @Override
    public void onEvent(SlashCommandCreateEvent originalEvent, SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final String dicePool = event.getOptionStringValueByName("dicepool").orElse("");
        final Integer discount = event.getOptionLongValueByName("discount").map(Math::toIntExact).orElse(0);
        final Integer diceKept = event.getOptionLongValueByName("dicekept").map(Math::toIntExact).orElse(2);
        final Boolean enhanceable = event.getOptionBooleanValueByName("enhanceable").orElse(null);

        RollLogic.handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, false);
    }

}
