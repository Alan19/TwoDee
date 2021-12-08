package rolling;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.List;

public class RollOutput {
    private final List<EmbedBuilder> embeds;
    private final boolean plotDiceUsed;
    private final int rollTotal;
    private final boolean opportunity;

    public RollOutput(List<EmbedBuilder> embeds, boolean plotDiceUsed, int rollTotal, boolean opportunity) {
        this.embeds = embeds;
        this.plotDiceUsed = plotDiceUsed;
        this.rollTotal = rollTotal;
        this.opportunity = opportunity;
    }

    public boolean isOpportunity() {
        return opportunity;
    }

    public List<EmbedBuilder> getEmbeds() {
        return embeds;
    }

    public boolean isPlotDiceUsed() {
        return plotDiceUsed;
    }

    /**
     * Returns whether the roll triggered an opportunity, after accounting for overrides
     *
     * @return If the roll has an opportunity
     */
    public int getRollTotal() {
        return rollTotal;
    }
}
