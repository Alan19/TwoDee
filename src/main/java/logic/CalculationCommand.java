package logic;

import calculation.CalculationLogic;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;

import java.util.List;

public class CalculationCommand implements VelenEvent, VelenSlashEvent {
    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        CalculationLogic.beginCalculations(event.getApi(), event.getChannel(), Long.parseLong(args[0]), message.getId());
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args,
                        List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {

    }

    public static void setup(Velen velen) {
        CalculationCommand calculationCommand = new CalculationCommand();

        VelenCommand.ofHybrid("calculation", "Runs Calculations of Roll Results", velen, calculationCommand, calculationCommand)
                .addOption(SlashCommandOption.create(
                        SlashCommandOptionType.NUMBER,
                        "start",
                        "Message Id for the starting message",
                        true
                ))
                .addOption(SlashCommandOption.create(
                        SlashCommandOptionType.NUMBER,
                        "end",
                        "Message Id for the ending message",
                        false
                ))
                .addShortcut("calc")
                .attach();
    }
}
