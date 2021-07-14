package slashcommands;

import logic.*;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;

public class SlashCommandRegister {
    public static Velen setupVelen() {
        Velen velen = Velen.builder().setDefaultPrefix(".").build();
        SnackLogic.setupSnackCommand(velen);
        setupBleedCommand(velen);
        setupStopCommand(velen);
        PlotPointLogic.registerPlotPointCommand(velen);

        ReplenishLogic.setupReplenishCommand(velen);
        return velen;
    }

    private static void setupStopCommand(Velen velen) {
        StopLogic stopLogic = new StopLogic();
        VelenCommand.ofHybrid("stop", "Shuts down the bot the bot!", velen, stopLogic, stopLogic).setServerOnly(true, 468046159781429250L).attach();
    }

    private static void setupBleedCommand(Velen velen) {
        BleedLogic bleedLogic = new BleedLogic();
        VelenCommand.ofHybrid("bleed", "Applies plot point bleed!", velen, bleedLogic, bleedLogic).addOptions(SlashCommandOption.create(SlashCommandOptionType.MENTIONABLE, "target", "The party to bleed", true), SlashCommandOption.create(SlashCommandOptionType.INTEGER, "modifier", "The bonus or penalty on the bleed", false)).setServerOnly(true, 468046159781429250L).attach();
    }

}
