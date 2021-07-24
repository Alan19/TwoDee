package logic;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
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
import sheets.PlotPointChangeResult;
import sheets.PlotPointUtils;
import util.UtilFunctions;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class ReplenishLogic implements VelenSlashEvent, VelenEvent {
    public static void setupReplenishCommand(Velen velen) {
        ReplenishLogic replenishLogic = new ReplenishLogic();
        VelenCommand.ofHybrid("replenish", "Adds plot points for session replenishment, or good role playing", velen, replenishLogic, replenishLogic).addOptions(SlashCommandOption.create(SlashCommandOptionType.ROLE, "party", "the party to add plot points to", true), SlashCommandOption.create(SlashCommandOptionType.INTEGER, "count", "the number of plot points to add", true)).addShortcuts("sr", "sessionreplenishment").setServerOnly(true, 468046159781429250L).attach();
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
        return PlotPointUtils.addPlotPointsToUsers(role.getUsers(), count).thenApply(plotPointChangeResult -> getReplenishEmbed(channel, plotPointChangeResult).setFooter("Requested by " + UtilFunctions.getUsernameInChannel(author, channel), author.getAvatar()));
    }

    /**
     * Create an embed that contains the changes in plot points
     * <p>
     * Players whose plot points cannot be modified will be listed in the embed
     *
     * @param channel               The channel to send the embed to
     * @param plotPointChangeResult An object containing the changes in plot points and players that cannot be edited
     */
    private EmbedBuilder getReplenishEmbed(TextChannel channel, PlotPointChangeResult plotPointChangeResult) {
        final EmbedBuilder embed = plotPointChangeResult.generateEmbed(channel).setTitle("Session Replenishment!");
        if (!plotPointChangeResult.getUnmodifiableUsers().isEmpty()) {
            embed.setDescription("I was unable to edit the plot points of:\n - " + plotPointChangeResult.getUnmodifiableUsers().stream().map(user -> UtilFunctions.getUsernameInChannel(user, channel)).collect(Collectors.joining("\n - ")));
        }

        return embed;
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        if (args.length >= 2) {
            final Matcher matcher = DiscordRegexPattern.ROLE_MENTION.matcher(args[0]);
            final Optional<Integer> count = UtilFunctions.tryParseInt(args[1]);
            if (matcher.find() && count.isPresent()) {
                Optional<Role> party = event.getApi().getRoleById(matcher.group("id"));
                if (party.isPresent()) {
                    replenishParties(user, party.get(), count.get(), event.getChannel()).thenAccept(embedBuilder -> event.getChannel().sendMessage(embedBuilder.setFooter("Requested by " + UtilFunctions.getUsernameInChannel(user, message.getChannel()), user.getAvatar())));
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
