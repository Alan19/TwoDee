package logic;

import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.util.DiscordRegexPattern;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenEvent;
import pw.mihou.velen.interfaces.VelenSlashEvent;
import sheets.PlotPointChangeResult;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;
import util.UtilFunctions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

public class BleedLogic implements VelenSlashEvent, VelenEvent {
    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        if (args.length > 0) {
            final Matcher userMatcher = DiscordRegexPattern.USER_MENTION.matcher(args[0]);
            final Matcher roleMatcher = DiscordRegexPattern.ROLE_MENTION.matcher(args[0]);
            if (userMatcher.find()) {
                event.getApi().getUserById(userMatcher.group("id")).thenAccept(foundUser -> {
                    int modifier = args.length > 1 ? UtilFunctions.tryParseInt(args[1]).orElse(0) : 0;
                    onBleedCommand(user, event.getChannel(), Collections.singletonList(foundUser), modifier).thenAccept(embed -> new MessageBuilder().addEmbed(embed).send(event.getChannel()));
                });
            }
            else if (roleMatcher.find()) {
                event.getApi().getRoleById(roleMatcher.group("id")).ifPresent(role -> {
                    int modifier = args.length > 1 ? UtilFunctions.tryParseInt(args[1]).orElse(0) : 0;
                    onBleedCommand(user, event.getChannel(), new ArrayList<>(role.getUsers()), modifier).thenAccept(embed -> new MessageBuilder().addEmbed(embed).send(event.getChannel()));
                });
            }
        }
        else {
            event.getChannel().sendMessage("Invalid role or channel!");
        }
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Optional<Mentionable> targets = event.getOptionMentionableValueByName("target");
        if (targets.isPresent() && event.getChannel().isPresent()) {
            List<User> users = new ArrayList<>();
            if (targets.get() instanceof Role) {
                users.addAll(((Role) targets.get()).getUsers());
            }
            else {
                users.add((User) targets.get());
            }
            event.respondLater().thenAcceptBoth(onBleedCommand(event.getUser(), event.getChannel().get(), users, event.getOptionIntValueByName("modifier").orElse(0)), (updater, embed) -> updater.addEmbed(embed).update());
        }
        else {
            firstResponder.setContent("Invalid role or channel!").respond();
        }
    }

    /**
     * Applies plot point bleed to all specified users
     *
     * @param sender   The sender of the message
     * @param channel  The channel the message was sent in
     * @param users    The users to apply bleed to
     * @param modifier The modifier on the bleed
     * @return A CompletableFuture that contains an embed that records the changes in plot points
     */
    private CompletableFuture<EmbedBuilder> onBleedCommand(User sender, TextChannel channel, List<User> users, Integer modifier) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        List<User> errors = new ArrayList<>();
        AtomicInteger bleed = new AtomicInteger();
        List<CompletableFuture<Void>> list = new ArrayList<>();
        for (User user : users) {
            if (SheetsHandler.getPlayerBleed(user).orElse(0) > 0) {
                CompletableFuture<Void> completableFuture = bleedPlayer(user).thenAccept(change -> {
                    if (change.isPresent()) {
                        changes.add(change.get());
                        bleed.addAndGet((change.get().getMiddle() - change.get().getRight()));
                    }
                    else {
                        errors.add(user);
                    }
                });
                list.add(completableFuture);
            }
        }
        final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(list.toArray(new CompletableFuture[0]));
        return voidCompletableFuture.thenApply(unused -> getBleedEmbed(sender, channel, changes, errors, bleed.get(), modifier));
    }

    /**
     * Applies plot point bleed to one player. If the plot points cannot be retrieved, return an empty optional.
     *
     * @param user The user to apply bleed to
     * @return An optional that contains the change in plot points for one player
     */
    private CompletableFuture<Optional<Triple<User, Integer, Integer>>> bleedPlayer(User user) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
        final Optional<Integer> playerBleed = SheetsHandler.getPlayerBleed(user);
        if (plotPoints.isPresent() && playerBleed.isPresent()) {
            return PlotPointUtils.addPlotPointsToUser(user, playerBleed.get() * -1).thenApply(integer -> integer.map(value -> Triple.of(user, plotPoints.get(), value)));
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Generates the embed that contains the information for the bleed
     *
     * @param sender   The person who sent the message
     * @param channel  The channel the message was sent in
     * @param changes  The changes in plot points as a triple in the format: user, old plot points, new plot points
     * @param errors   The list of users that had an error when modifying plot points
     * @param bleed    The total amount of bleed
     * @param modifier The modifier on the bleed
     * @return An embed containing the changes in plot points and the total amount of bleed
     */
    private EmbedBuilder getBleedEmbed(User sender, TextChannel channel, List<Triple<User, Integer, Integer>> changes, List<User> errors, int bleed, int modifier) {
        String message = modifier != 0 ? MessageFormat.format("You normally would have {0} plot point(s) to work with, but something happened and you now have **{1}** plot point(s) in bleed!", bleed, bleed + modifier) : MessageFormat.format("What can you do with **{0}** plot point(s)?", bleed);
        return new PlotPointChangeResult(changes, errors).generateEmbed(channel)
                .setTitle("Applying post-session bleed!")
                .setDescription(message)
                .setFooter("Requested by " + UtilFunctions.getUsernameInChannel(sender, channel), sender.getAvatar());
    }
}
