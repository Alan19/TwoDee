package logic.statisticstates;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class StatisticsContext {
    private StatisticsState state;
    private EmbedBuilder embedBuilder;

    public StatisticsContext(String message){
        state = new ScanDice(message);
    }

    public void setState(StatisticsState state){
        this.state = state;
    }

    public void setEmbedBuilder(EmbedBuilder embedBuilder){
        this.embedBuilder = embedBuilder;
    }

    public void execute(){
        state.process(this);
    }
}
