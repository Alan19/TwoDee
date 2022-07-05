package rolling;

import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

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
            final Optional<String> diceKeptOptional = args.asMessageOptions()
                    .flatMap(strings -> Arrays.stream(strings).skip(1).findFirst())
                    .filter(s -> s.matches("[1-9]\\d*"));
            if (diceKeptOptional.isPresent()) {
                diceKept = Integer.parseInt(diceKeptOptional.get());
                dicePool = Arrays.stream(args.asMessageOptions().orElseThrow(IllegalStateException::new))
                        .skip(2)
                        .collect(Collectors.joining(" "));
            }
            else {
                dicePool = args.getManyWithName("dicepool").orElse("");
            }
        }
        else {
            dicePool = args.getManyWithName("dicepool").orElse("");
            diceKept = args.withName("dicekept").flatMap(VelenOption::asInteger).orElse(2);
        }
        return new CoreRollParameters(dicePool, diceKept);
    }
}
