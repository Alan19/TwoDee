package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import players.PartyHandler;
import sheets.PlotPointHandler;
import sheets.SheetsHandler;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BleedCommand implements CommandExecutor {
    @Command(aliases = {"~b", "~bleed"}, description = "Applies plot point bleed to a party", privateMessages = false, usage = "~b <roleping>")
    public void executeBleed(DiscordApi api, Message message, MessageAuthor author, TextChannel channel, Object[] parameters) {
        final List<Role> parties = new ArrayList<>(message.getMentionedRoles());
        // Add party roles mentioned by strings to parties
        Arrays.stream(parameters)
                .filter(partyName -> partyName instanceof String)
                .forEach(partyName -> PartyHandler.getPartyByName((String) partyName).flatMap(party -> api.getRoleById(party.getRoleID())).ifPresent(parties::add));
        // If there is a number, run the replenish function using the parties and that number as the amount to replenish
        bleedParty(parties.get(0), channel, author);

    }

    /**
     * Applies bleed to the specified party, indicated by role
     *
     * @param role    The role representing the party to apply bleed
     * @param channel The channel the command was sent from
     * @param author  The author that invoked the command
     */
    private void bleedParty(Role role, TextChannel channel, MessageAuthor author) {
        List<Triple<User, Integer, Integer>> plotPointChanges = new ArrayList<>();
        List<User> uneditablePlayers = new ArrayList<>();
        final List<CompletableFuture<Optional<Integer>>> bleedFutures = role.getUsers().stream()
                .filter(user -> SheetsHandler.getPlotPoints(user).isPresent() && SheetsHandler.getPlayerBleed(user).orElse(0) > 0)
                .map(user -> applyBleed(plotPointChanges, uneditablePlayers, user))
                .collect(Collectors.toList());
        CompletableFuture.allOf(bleedFutures.toArray(new CompletableFuture[]{}))
                .thenAccept(unused -> sendBleedResultEmbed(channel, author, plotPointChanges));
    }

    /**
     * Applies end of session bleed to the party and record the changes in plot points
     *
     * @param plotPointChanges  A list recording the changes in plot points for this session
     * @param uneditablePlayers A list of players whose plot points were not edited
     * @param user              The user that will lose plot points
     * @return A CompletableFuture representing the completion of the bleed operation on a user with exceptions handled
     */
    private CompletableFuture<Optional<Integer>> applyBleed(List<Triple<User, Integer, Integer>> plotPointChanges, List<User> uneditablePlayers, User user) {
        final Optional<Integer> oldPlotPointCount = SheetsHandler.getPlotPoints(user);
        final Optional<Integer> playerBleed = SheetsHandler.getPlayerBleed(user);
        if (oldPlotPointCount.isPresent() && playerBleed.isPresent()) {
            final int newPlotPointCount = oldPlotPointCount.get() - playerBleed.get();
            return PlotPointHandler.setPlotPointsAndLog(plotPointChanges, uneditablePlayers, user, oldPlotPointCount.get(), newPlotPointCount);
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Sends an embed that contains the changes in plot points after bleed, and the total session bleed value
     * <p>
     * Players whose plot points cannot be modified will be listed in the embed
     *
     * @param channel          The channel to send the embed to
     * @param author           The author that sent the command
     * @param plotPointChanges A list of triples containing changes in plot points
     */
    private void sendBleedResultEmbed(TextChannel channel, MessageAuthor author, List<Triple<User, Integer, Integer>> plotPointChanges) {
        int totalBleed = plotPointChanges.stream().mapToInt(plotPointChange -> plotPointChange.getMiddle() - plotPointChange.getRight()).sum();
        final EmbedBuilder replenishmentEmbed = PlotPointHandler.generateEmbed(plotPointChanges, channel, author).setTitle("Post-session Bleed!").setDescription(MessageFormat.format("What can you do with **{0}** plot point(s)?", totalBleed));
        channel.sendMessage(replenishmentEmbed);
    }


}
