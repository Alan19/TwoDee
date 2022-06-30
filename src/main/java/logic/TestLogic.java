package logic;

import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOption;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;

import java.util.List;

public class TestLogic implements VelenHybridHandler {
    public static void setTestCommand(Velen velen) {
        TestLogic testLogic = new TestLogic();
        List<SlashCommandOption> options = RollLogic.getRollCommandOptions();
        VelenCommand.ofHybrid("test", "Rolls some dice with opportunities disabled!", velen, testLogic)
                .addOptions(options.toArray(new SlashCommandOption[0]))
                .addFormats("test :[dicepool:of(string):hasMany()]",
                        "test -- :[dicekept:of(integer)] :[dicepool:of(string):hasMany()]")
                .addShortcuts("t")
                .attach();
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final String dicePool = args.getManyWithName("dicepool").orElse("");
        final Integer discount = args.withName("discount").flatMap(VelenOption::asInteger).orElse(0);
        final Integer diceKept = args.withName("dicekept").flatMap(VelenOption::asInteger).orElse(2);
        final Boolean enhanceable = args.withName("enhanceable").flatMap(VelenOption::asBoolean).orElse(null);

        RollLogic.handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, false);

    }
}
