package rolling;

import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;

public record CoreRollParameters(String pool, int diceKept) {
    /**
     * Extracts the number of kept dice and the dice pool from the parameters of a hybrid event
     *
     * @param event The event to determine if it's a slash or message event
     * @param args  The list of hybrid arguments
     * @return The number of kept dice (default 2), and the pool of dice to roll
     */
    public static CoreRollParameters getCoreRollParametersFromHybridEvent(VelenGeneralEvent event, VelenHybridArguments args) {
        int diceKept = 2;
        String dicePool;
        if (event.isMessageEvent()) {
            final String commandName = args.get(0).asString().orElseThrow(IllegalStateException::new);
            if (commandName.charAt(commandName.length() - 1) == '3') {
                diceKept = 3;
            }
            dicePool = args.getManyWithName("dicepool").orElse("");
        }
        else {
            dicePool = args.getManyWithName("dicepool").orElse("");
            diceKept = args.withName("dicekept").flatMap(VelenOption::asInteger).orElse(2);
        }
        return new CoreRollParameters(dicePool, diceKept);
    }
}
