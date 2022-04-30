package logic;

import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
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
import sheets.PlotPointChangeResult;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;
import util.DiscordHelper;
import util.UtilFunctions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class BleedLogic implements VelenHybridHandler {

    public static final String BLEED_MENTIONABLE = "bleed-mentionable";
    public static final String BLEED_MODIFIER = "bleed-modifier";

    public static void setupBleedCommand(Velen velen) {
        BleedLogic bleedLogic = new BleedLogic();
        VelenCommand.ofHybrid("bleed", "Applies plot point bleed!", velen, bleedLogic)
                .addOptions(SlashCommandOption.create(SlashCommandOptionType.ROLE, BLEED_MENTIONABLE, "The party to bleed", true), SlashCommandOption.create(SlashCommandOptionType.LONG, BLEED_MODIFIER, "The bonus or penalty on the bleed", false))
                .addFormats(String.format("bleed :[%s:of(role)]", BLEED_MENTIONABLE))
                .attach();
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        Try.of(() -> args.withName(BLEED_MENTIONABLE)
                        .flatMap(VelenOption::asRole)
                        .orElseThrow(IllegalArgumentException::new))
                .map(DiscordHelper::getUsersForMentionable)
                .onSuccess(users -> handleBleed(users, args.withName(BLEED_MODIFIER).flatMap(VelenOption::asInteger).orElse(0), user, event.getChannel()).thenAccept(embedBuilder -> responder.addEmbed(embedBuilder).respond()));
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

    private CompletableFuture<EmbedBuilder> handleBleed(Collection<User> users, Integer modifier, User sender, TextChannel channel) {
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

}
