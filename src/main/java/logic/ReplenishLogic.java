package logic;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.util.DiscordRegexPattern;
import pw.mihou.velen.interfaces.*;
import sheets.PlotPointUtils;
import util.UtilFunctions;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

public class ReplenishLogic implements VelenSlashEvent, VelenEvent {
    public static void setupReplenishCommand(Velen velen) {
        ReplenishLogic replenishLogic = new ReplenishLogic();
        VelenCommand.ofHybrid("replenish", "Adds plot points for session replenishment, or good role playing", velen, replenishLogic, replenishLogic).addOptions(SlashCommandOption.create(SlashCommandOptionType.ROLE, "party", "the party to add plot points to", true), SlashCommandOption.create(SlashCommandOptionType.INTEGER, "count", "the number of plot points to add", true)).addShortcuts("sr", "sessionreplenishment").attach();
    }

    /**
     * Adds the specified number of plot points to all players with the input roles, and return an embed with the results
     * <p>
     * The embed will list players that the bot cannot successfully edit
     *
     * @param author  The author of the message
     * @param role    The party to add plot points to
     * @param count   The number of plot point to add ot each player
     * @param channel The channel the message was sent from
     * @return A future that contains the embed with the result for the plot point changes
     */
    private CompletableFuture<EmbedBuilder> replenishParties(User author, Role role, Integer count, TextChannel channel) {
        return PlotPointUtils.addPlotPointsToUsers(role.getUsers(), count).thenApply(plotPointChangeResult -> plotPointChangeResult.getReplenishEmbed(channel).setFooter("Requested by " + UtilFunctions.getUsernameInChannel(author, channel), author.getAvatar()));
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        if (args.length >= 2) {
            final Matcher matcher = DiscordRegexPattern.ROLE_MENTION.matcher(args[0]);
            final Optional<Integer> count = UtilFunctions.tryParseInt(args[1]);
            if (matcher.find() && count.isPresent()) {
                Optional<Role> party = event.getApi().getRoleById(matcher.group("id"));
                if (party.isPresent()) {
                    replenishParties(user, party.get(), count.get(), event.getChannel()).thenAccept(embedBuilder -> new MessageBuilder().addEmbed(embedBuilder.setFooter("Requested by " + UtilFunctions.getUsernameInChannel(user, message.getChannel()), user.getAvatar())).send(event.getChannel()));
                }
                else {
                    event.getChannel().sendMessage("Unable to find role!");
                }
            }
            else {
                event.getChannel().sendMessage("Unable to find role or count!");
            }
        }
        else {
            event.getChannel().sendMessage("Unable to find role or count!");
        }
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Optional<Role> party = event.getOptionRoleValueByName("party");
        final Optional<Integer> count = event.getOptionIntValueByName("count");
        if (party.isPresent() && count.isPresent() && event.getChannel().isPresent()) {
            event.respondLater().thenAcceptBoth(replenishParties(user, party.get(), count.get(), event.getChannel().get()), (updater, embed) -> updater.addEmbed(embed).update());
        }
        firstResponder.setContent("Unable to find channel!").respond();
    }
}
