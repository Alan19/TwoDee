package logic.statisticstates;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class StatisticsContext {
    private StatisticsState state;
    private EmbedBuilder embedBuilder;

    public StatisticsContext(String message){
        state = new ScanDice(message);
        execute();
    }

    public EmbedBuilder getEmbedBuilder() {
        return embedBuilder;
    }

    public void setEmbedBuilder(EmbedBuilder embedBuilder){
        this.embedBuilder = embedBuilder;
    }

    public void setState(StatisticsState state){
        this.state = state;
    }

    public EmbedBuilder execute(){
        while (embedBuilder == null){
            state.process(this);
        }
        return embedBuilder;
    }
}
