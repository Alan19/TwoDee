package logic;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;
import sheets.PlotPointUtils;
import util.DiscordHelper;

import java.util.concurrent.CompletableFuture;

public class ReplenishLogic implements VelenHybridHandler {
    public static void setupReplenishCommand(Velen velen) {
        ReplenishLogic replenishLogic = new ReplenishLogic();
        VelenCommand.ofHybrid("replenish", "Adds plot points for session replenishment, or good role playing", velen, replenishLogic)
                .addOptions(SlashCommandOption.create(SlashCommandOptionType.ROLE, "party", "the party to add plot points to", true), SlashCommandOption.create(SlashCommandOptionType.LONG, "count", "the number of plot points to add", true))
                .addFormats("replenish :[party:of(role)] :[count:of(numeric)]")
                .addShortcuts("sr", "sessionreplenishment")
                .attach();
    }

    /**
     * Adds the specified number of plot points to all players with the input roles, and return an embed with the results
     * <p>
     * The embed will list players that the bot cannot successfully edit
     *
     * @param role    The party to add plot points to
     * @param count   The number of plot point to add ot each player
     * @param channel The channel the message was sent from
     * @return A future that contains the embed with the result for the plot point changes
     */
    private CompletableFuture<EmbedBuilder> replenishParties(Role role, Integer count, TextChannel channel) {
        return PlotPointUtils.addPlotPointsToUsers(role.getUsers(), count).thenApply(plotPointChangeResult -> plotPointChangeResult.getReplenishEmbed(channel));
    }


    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final Role party = args.withName("party")
                .flatMap(VelenOption::asRole)
                .orElseThrow(IllegalStateException::new);
        final int count = args.withName("count").flatMap(VelenOption::asInteger).orElseThrow(IllegalStateException::new);
        replenishParties(party, count, event.getChannel()).thenAccept(output -> responder.setEmbed(DiscordHelper.addUserToFooter(event, user, output)).respond());
    }

}
