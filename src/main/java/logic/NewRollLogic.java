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

public class NewRollLogic implements VelenHybridHandler {

    public static final String DICE_POOL = "dice-pool";

    public static void setupRollCommand(Velen velen) {
        NewRollLogic newRollLogic = new NewRollLogic();
        VelenCommand.ofHybrid("foo", "Rolls some dice!", velen, newRollLogic)
                .addOptions(SlashCommandOption.createStringOption(DICE_POOL, "the dice pool to be rolled", true))
                .addFormats(String.format("foo :[%s:of(string):hasMany()]", DICE_POOL))
                .attach();
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        responder.setContent("Received " + args.withName(DICE_POOL).flatMap(VelenOption::asString).orElse("")).respond();
    }
}
