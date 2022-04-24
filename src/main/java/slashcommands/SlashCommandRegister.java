package slashcommands;

import language.LanguageCommand;
import language.LanguageLogic;
import logic.*;
import pw.mihou.velen.interfaces.Velen;

public class SlashCommandRegister {
    public static Velen setupVelen(LanguageLogic languageLogic) {
        Velen velen = Velen.builder().setDefaultPrefix("~").build();
        SnackLogic.setupSnackCommand(velen);
        BleedLogic.setupBleedCommand(velen);
        StopLogic.setupStopCommand(velen);
        PlotPointLogic.registerPlotPointCommand(velen);
        ReplenishLogic.setupReplenishCommand(velen);
        DoomLogic.setupDoomCommand(velen);
        RollLogic.setupRollCommand(velen);
        TestLogic.setTestCommand(velen);
        StatisticsLogic.setupStatisticsCommand(velen);
        DamageLogic.setupDamageCommand(velen);
        RollPoolLogic.setupPoolCommand(velen);
        CalculationLogic.setup(velen);
        AdvancementLogic.setupAdvancementCommand(velen);
        LanguageCommand.setup(velen, languageLogic);
        NewRollLogic.setupRollCommand(velen);

        return velen;
    }

}
