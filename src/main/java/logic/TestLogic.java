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
import rolling.CoreRollParameters;

import java.util.List;

public class TestLogic implements VelenHybridHandler {
    public static void setTestCommand(Velen velen) {
        TestLogic testLogic = new TestLogic();
        List<SlashCommandOption> options = RollLogic.getRollCommandOptions();
        VelenCommand.ofHybrid("test", "Rolls some dice with opportunities disabled!", velen, testLogic)
                .addOptions(options.toArray(new SlashCommandOption[0]))
                .addFormats("test :[dicepool:of(string):hasMany()]")
                .addShortcuts("t")
                .attach();
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final CoreRollParameters coreRollParameters = CoreRollParameters.getCoreRollParametersFromHybridEvent(event, args);
        final Integer discount = args.withName("discount").flatMap(VelenOption::asInteger).orElse(0);
        final Boolean enhanceable = args.withName("enhanceable").flatMap(VelenOption::asBoolean).orElse(null);

        RollLogic.handleRoll(event, coreRollParameters.pool(), discount, coreRollParameters.diceKept(), enhanceable, false);
    }
}
