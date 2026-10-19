package slashcommands;

import language.LanguageCommand;
import language.LanguageLogic;
import logic.*;
import pw.mihou.velen.interfaces.Velen;
import statistics.StatisticsCommand;

import java.time.Duration;

public class SlashCommandRegister {
    public static Velen setupVelen(LanguageLogic languageLogic) {
        Velen velen = Velen.builder().setDefaultPrefix("~").setDefaultCooldownTime(Duration.ZERO).build();
        SnackLogic.setupSnackCommand(velen);
        BleedLogic.setupBleedCommand(velen);
        StopLogic.setupStopCommand(velen);
        PlotPointLogic.registerPlotPointCommand(velen);
        ReplenishLogic.setupReplenishCommand(velen);
        DoomLogic.setupDoomCommand(velen);
        RollLogic.setupRollCommand(velen);
        TestLogic.setTestCommand(velen);
        StatisticsCommand.setupStatisticsCommand(velen);
        DamageLogic.setupDamageCommand(velen);
        RollPoolLogic.setupPoolCommand(velen);
        CalculationLogic.setup(velen);
        AdvancementLogic.setupAdvancementCommand(velen);
        LanguageCommand.setup(velen, languageLogic);
        QuoteLogic.setupQuoteLogic(velen);

        return velen;
    }

}
