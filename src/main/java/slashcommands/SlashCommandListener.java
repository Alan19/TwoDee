package slashcommands;

import doom.DoomHandler;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.interaction.ApplicationCommandInteraction;
import org.javacord.api.interaction.ApplicationCommandInteractionOption;
import org.javacord.api.listener.interaction.InteractionCreateListener;
import sheets.PlotPointHandler;
import sheets.SheetsHandler;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SlashCommandListener implements InteractionCreateListener {
    private void handleBleedCommand(ApplicationCommandInteraction applicationCommand) {
        final Optional<Mentionable> targets = applicationCommand.getOptionMentionableValueByName("targets");
        List<User> mentionedUsers = targets.map(this::getMentionedUsers).orElseGet(ArrayList::new);
        List<Triple<User, Integer, Integer>> plotPointChanges = new ArrayList<>();
        List<User> uneditablePlayers = new ArrayList<>();
        final List<CompletableFuture<Optional<Integer>>> bleedFutureList = mentionedUsers.stream()
                .filter(user -> SheetsHandler.getPlayerBleed(user).map(integer -> integer > 0).orElse(false))
                .map(user -> applyBleed(plotPointChanges, uneditablePlayers, user))
                .collect(Collectors.toList());
        if (applicationCommand.getChannel().isPresent()) {
            final CompletableFuture<EmbedBuilder> bleedEmbedGeneratedFuture = CompletableFuture.allOf(bleedFutureList.toArray(new CompletableFuture[]{})).thenApply(unused -> generateBleedEmbed(applicationCommand.getChannel().get(), applicationCommand.getUser(), plotPointChanges, applicationCommand.getOptionIntValueByName("modifier").orElse(0)));
            applicationCommand.respondLater().thenAcceptBoth(bleedEmbedGeneratedFuture, (updater, embedBuilder) -> updater.addEmbed(embedBuilder).update());
        }
        else {
            applicationCommand.createImmediateResponder().addEmbed(new EmbedBuilder().setDescription("Unable to find a channel!")).respond();
        }
    }

    private List<User> getMentionedUsers(Mentionable targets) {
        List<User> mentionedUsers = new ArrayList<>();
        if (targets instanceof Role) {
            mentionedUsers = new ArrayList<>(((Role) targets).getUsers());
        }
        else if (targets instanceof User) {
            mentionedUsers = Collections.singletonList((User) targets);
        }
        return mentionedUsers;
    }

    /**
     * Sends an embed that contains the changes in plot points after bleed, and the total session bleed value
     * <p>
     * Players whose plot points cannot be modified will be listed in the embed
     *
     * @param channel          The channel to send the embed to
     * @param user             The user that sent the command
     * @param plotPointChanges A list of triples containing changes in plot points
     * @return An embed that contains the change in plot points the the total bleed amount
     */
    private EmbedBuilder generateBleedEmbed(TextChannel channel, User user, List<Triple<User, Integer, Integer>> plotPointChanges, int modifier) {
        int totalBleed = plotPointChanges.stream().mapToInt(plotPointChange -> plotPointChange.getMiddle() - plotPointChange.getRight()).sum();
        return PlotPointHandler.generateEmbed(plotPointChanges, channel, user).setTitle("Post-session Bleed!").setDescription(MessageFormat.format("What can you do with **{0}** plot point(s)?", totalBleed));
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


    private void handleDoomCommand(ApplicationCommandInteraction applicationCommand) {
        applicationCommand.getOptionByName("mode").map(ApplicationCommandInteractionOption::getOptions).ifPresent(applicationCommandInteractionOptions -> {
            final ApplicationCommandInteractionOption modeOption = applicationCommandInteractionOptions.get(0);
            final Optional<String> poolName = modeOption.getOptionStringValueByName("name");
            final Optional<Integer> count = modeOption.getOptionIntValueByName("count");
            // TODO Add error messages instead of using orElse
            switch (modeOption.getName()) {
                case "query":
                    if (modeOption.getOptions().isEmpty()) {
                        applicationCommand.createImmediateResponder().addEmbed(DoomHandler.generateDoomEmbed()).respond();
                    }
                    else {
                        applicationCommand.createImmediateResponder().addEmbed(DoomHandler.generateDoomEmbed(poolName.get())).respond();
                    }
                    break;
                case "add":
                    applicationCommand.createImmediateResponder().addEmbed(DoomHandler.addDoom(poolName.orElseGet(DoomHandler::getActivePool), count.orElse(0))).respond();
                    break;
                case "sub":
                    applicationCommand.createImmediateResponder().addEmbed(DoomHandler.addDoom(poolName.orElseGet(DoomHandler::getActivePool), count.orElse(0) * -1)).respond();
                    break;
                case "select":
                    applicationCommand.createImmediateResponder().addEmbed(DoomHandler.setActivePool(poolName.orElse("Doom!"))).respond();
                    break;
                case "set":
                    applicationCommand.createImmediateResponder().addEmbed(DoomHandler.setDoom(poolName.orElseGet(DoomHandler::getActivePool), count.orElse(0))).respond();
                    break;
                case "delete":
                    applicationCommand.createImmediateResponder().addEmbed(DoomHandler.deletePool(poolName.orElseGet(DoomHandler::getActivePool))).respond();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + modeOption.getName());
            }
        });
    }

    @Override
    public void onInteractionCreate(InteractionCreateEvent event) {
        event.getApplicationCommandInteraction().ifPresent(applicationCommand -> {
            switch (applicationCommand.getCommandName()) {
                case "doom":
                    handleDoomCommand(applicationCommand);
                case "bleed":
                    handleBleedCommand(applicationCommand);
            }
        });
    }
}
