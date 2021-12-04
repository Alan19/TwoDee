package logic;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;
import util.RandomColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DamageLogic implements VelenSlashEvent {

    public static void setupDamageCommand(Velen velen) {
        final List<SlashCommandOptionBuilder> options = new ArrayList<>();
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.STRING)
                .setName("type")
                .setDescription("the type of damage being inflicted")
                .addChoice("stun", "stun")
                .addChoice("basic", "basic")
                .addChoice("wound", "wound")
                .setRequired(true));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("count")
                .setDescription("the amount of damage that is being inflicted")
                .setRequired(true));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("stun-armor")
                .setDescription("the amount of stun armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("basic-armor")
                .setDescription("the amount of basic armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("wound-armor")
                .setDescription("the amount of wound armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("resilience")
                .setDescription("the amount of resilience you have"));

        DamageLogic logic = new DamageLogic();

        VelenCommand.ofSlash("damage", "calculates the damage of each type received from an attack", velen, logic)
                .addOptions(options.toArray(new SlashCommandOptionBuilder[0]))
                .attach();
    }

    /**
     * Sends an ephemeral embed to the user that contains the amount of points to mark off after taking damage
     *
     * @param interaction              The interaction that contains the command
     * @param user                     The user that used the command
     * @param arguments                The arguments in the command
     * @param list                     The list of interaction options in the command
     * @param immediateResponseBuilder The response builder to respond immediately
     */
    @Override
    public void onEvent(SlashCommandInteraction interaction, User user, VelenArguments arguments, List<SlashCommandInteractionOption> list, InteractionImmediateResponseBuilder immediateResponseBuilder) {
        final Optional<String> typeOptional = interaction.getOptionStringValueByName("type");
        final Optional<Long> countOptional = interaction.getOptionLongValueByName("count");
        if (typeOptional.isPresent() && countOptional.isPresent()) {
            // Get params
            final DamageType type = DamageType.getType(typeOptional.get());
            long damageCount = countOptional.get();
            long stunArmor = interaction.getOptionLongValueByName("stun-armor").orElse(0L);
            long basicArmor = interaction.getOptionLongValueByName("basic-armor").orElse(0L);
            long woundArmor = interaction.getOptionLongValueByName("wound-armor").orElse(0L);
            long resilience = interaction.getOptionLongValueByName("resilience").orElse(0L);

            final EmbedBuilder embed = createDamageEmbed(type, damageCount, stunArmor, basicArmor, woundArmor, resilience);
            immediateResponseBuilder.addEmbed(embed).setFlags(InteractionCallbackDataFlag.EPHEMERAL).respond();
        }
        else {
            immediateResponseBuilder.setContent("invalid damage type and / or count!").setFlags(InteractionCallbackDataFlag.EPHEMERAL).respond();
        }
    }

    /**
     * Creates an embed that specifies the amount of points to mark off
     *
     * @param type        The type of damage being inflicted
     * @param damageCount The amount of damage being inflicted
     * @param stunArmor   Your stun armor rating
     * @param basicArmor  Your basic armor rating
     * @param woundArmor  Your wound armor rating
     * @param resilience  Your resilience
     * @return An embed that specifies the amount of points for resilience, stun, and wounds to mark off
     */
    private EmbedBuilder createDamageEmbed(DamageType type, long damageCount, long stunArmor, long basicArmor, long woundArmor, long resilience) {
        final Triple<Long, Long, Long> damageResult = calculateDamage(type, damageCount, resilience, stunArmor, basicArmor, woundArmor);
        // TODO Add more lines
        final EmbedBuilder embed = new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setTitle("Ouch! That hurt!")
                .setDescription("Mark off the following from your character sheet:");
        if (damageResult.getLeft() != 0) {
            embed.addField("Resilience", String.valueOf(damageResult.getLeft()));
        }
        if (damageResult.getMiddle() != 0) {
            embed.addField("Stun", String.valueOf(damageResult.getMiddle()));
        }
        if (damageResult.getRight() != 0) {
            embed.addField("Wounds", String.valueOf(damageResult.getRight()));
        }
        return embed;
    }

    /**
     * Calculates the amount of damage inflicted after factoring in resilience and armor
     *
     * @param type       The type of damage being inflicted
     * @param count      The amount of damage being inflicted
     * @param resilience The amount of resilience you have
     * @param stunArmor  Your stun armor rating
     * @param basicArmor Your basic armor rating
     * @param woundArmor Your wound armor rating
     * @return A triple that contains the amount of resilience used on the left, the amount of stun inflicted in the middle, and the amount of wounds inflicted on the right
     */
    public Triple<Long, Long, Long> calculateDamage(DamageType type, long count, long resilience, long stunArmor, long basicArmor, long woundArmor) {
        // Resilience affects the damage count directly, before armor
        final Pair<Long, Long> damage = splitDamage(type, count - resilience);
        long resilienceUsed = Math.min(count, resilience);
        long stunMitigated = Math.min(damage.getLeft(), stunArmor);
        long woundMitigated = Math.min(damage.getRight(), woundArmor);
        long basicMitigated = calculateBasicMitigation(damage, basicArmor);
        // Prioritize in the order of basic > wounds > stun in the case of a tie
        if (basicMitigated >= stunMitigated && basicMitigated >= woundMitigated) {
            final long basicStunMitigation = Math.min(damage.getLeft(), (int) Math.ceil((double) basicArmor / 2));
            final long basicWoundMitigation = Math.min(damage.getRight(), basicArmor / 2);
            return Triple.of(resilienceUsed, damage.getLeft() - basicStunMitigation, damage.getRight() - basicWoundMitigation);
        }
        else if (woundMitigated >= stunMitigated) {
            return Triple.of(resilienceUsed, damage.getLeft(), damage.getRight() - woundMitigated);
        }
        else {
            return Triple.of(resilienceUsed, damage.getLeft() - stunMitigated, damage.getRight());
        }
    }

    /**
     * Calculates the amount of damage basic armor would mitigate
     *
     * @param damage     The amount of damage being dealt
     * @param basicArmor Your basic armor rating
     * @return The total points of damage being mitigated through basic armor
     */
    private long calculateBasicMitigation(Pair<Long, Long> damage, long basicArmor) {
        // Basic armor and basic damage round up on stun, round down on wounds
        long stunMitigated = Math.min(damage.getLeft(), (long) Math.ceil((double) basicArmor / 2));
        long woundsMitigated = Math.min(damage.getRight(), basicArmor / 2);
        return stunMitigated + woundsMitigated;
    }

    /**
     * Returns damage inflicted as a pair, in order store the amount of damage inflicted of each type when basic damage is inflicted
     *
     * @param type  The type of damage being inflicted
     * @param count The amount of damage being inflicted
     * @return A pair that contains the amount of stun on the left, and the amount of wounds on the right
     */
    public Pair<Long, Long> splitDamage(DamageType type, long count) {
        if (type == DamageType.STUN) {
            return Pair.of(count, 0L);
        }
        else if (type == DamageType.WOUND) {
            return Pair.of(0L, count);
        }
        else {
            return Pair.of((long) Math.ceil((double) count / 2), count / 2);
        }
    }

    public enum DamageType {
        STUN, BASIC, WOUND;

        static DamageType getType(String string) {
            switch (string) {
                case "stun":
                    return STUN;
                case "basic":
                    return BASIC;
                case "wound":
                    return WOUND;
                default:
                    throw new IllegalStateException("Unexpected value: " + string);
            }
        }
    }
}
