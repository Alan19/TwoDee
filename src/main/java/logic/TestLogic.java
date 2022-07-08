package logic;

import org.javacord.api.entity.user.User;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;

import java.util.List;

import static logic.RollLogic.*;

public class TestLogic implements VelenHybridHandler {
    private final int diceKept;

    public TestLogic(int diceKept) {
        this.diceKept = diceKept;
    }

    public static void setTestCommand(Velen velen) {
        TestLogic testLogic = new TestLogic(2);
        VelenCommand.ofHybrid("test", "Rolls some dice with opportunities disabled!", velen, testLogic)
                .addOptions(DICE_POOL, DISCOUNT, ENHANCEABLE, DICE_KEPT)
                .addFormats("test :[dicepool:of(string):hasMany()]")
                .addShortcuts("t")
                .attach();
        List.of(1, 3, 4, 5).forEach(integer -> VelenCommand.ofHybrid("test%d".formatted(integer), "Roll some dice with opportuities disabled and keeps %s dice!".formatted(integer), velen, new TestLogic(integer))
                .addOptions(DICE_POOL, DISCOUNT, ENHANCEABLE)
                .addFormats("test%d :[dicepool:of(string):hasMany()]".formatted(integer))
                .addShortcuts("t%d".formatted(integer))
                .attach());

    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final String dicePool = args.getManyWithName("dicepool").orElse("");
        final Integer kept = args.withName("dicekept").flatMap(VelenOption::asInteger).orElse(this.diceKept);
        final Integer discount = args.withName("discount").flatMap(VelenOption::asInteger).orElse(0);
        final Boolean enhanceable = args.withName("enhanceable").flatMap(VelenOption::asBoolean).orElse(null);

        RollLogic.handleRoll(event, dicePool, discount, kept, enhanceable, false);
    }
}
