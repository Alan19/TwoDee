package logic;

import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
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
import sheets.PlotPointHandler;
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
                    bleedParty(user, event.getChannel(), Collections.singletonList(foundUser), modifier).thenAccept(embed -> event.getChannel().sendMessage(embed));
                });
            }
            else if (roleMatcher.find()) {
                event.getApi().getRoleById(roleMatcher.group("id")).ifPresent(role -> {
                    int modifier = args.length > 1 ? UtilFunctions.tryParseInt(args[1]).orElse(0) : 0;
                    bleedParty(user, event.getChannel(), new ArrayList<>(role.getUsers()), modifier).thenAccept(embed -> event.getChannel().sendMessage(embed));
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
            event.respondLater().thenAcceptBoth(bleedParty(event.getUser(), event.getChannel().get(), users, event.getOptionIntValueByName("modifier").orElse(0)), (updater, embed) -> updater.addEmbed(embed).update());
        }
        else {
            firstResponder.setContent("Invalid role or channel!").respond();
        }
    }

    private CompletableFuture<EmbedBuilder> bleedParty(User sender, TextChannel channel, List<User> users, Integer modifier) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        List<User> errors = new ArrayList<>();
        AtomicInteger bleed = new AtomicInteger(modifier);
        final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(users.stream().map(user -> bleedPlayer(user).thenAccept(change -> {
            if (change.isPresent()) {
                changes.add(change.get());
                bleed.addAndGet(change.get().getMiddle() - change.get().getRight());
            }
            else {
                errors.add(user);
            }
        })).toArray(CompletableFuture[]::new));
        return voidCompletableFuture.thenApply(unused -> getBleedEmbed(sender, channel, changes, errors, bleed));
    }

    private EmbedBuilder getBleedEmbed(User sender, TextChannel channel, List<Triple<User, Integer, Integer>> changes, List<User> errors, AtomicInteger bleed) {
        return new PlotPointChangeResult(changes, errors).generateEmbed(channel)
                .setAuthor(sender)
                .setTitle("Post-session Bleed!")
                .setDescription(MessageFormat.format("What can you do with **{0}** plot point(s)?", bleed));
    }

    private CompletableFuture<Optional<Triple<User, Integer, Integer>>> bleedPlayer(User user) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
        final Optional<Integer> playerBleed = SheetsHandler.getPlayerBleed(user);
        if (plotPoints.isPresent() && playerBleed.isPresent()) {
            return PlotPointHandler.addPlotPointsToUser(user, playerBleed.get() * -1).thenApply(integer -> integer.map(value -> Triple.of(user, plotPoints.get(), value)));
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

}
