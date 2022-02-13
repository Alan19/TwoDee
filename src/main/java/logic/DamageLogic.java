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
                .setType(SlashCommandOptionType.INTEGER)
                .setName("count")
                .setDescription("the amount of damage that is being inflicted")
                .setRequired(true));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.INTEGER)
                .setName("stun-armor")
                .setDescription("the amount of stun armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.INTEGER)
                .setName("basic-armor")
                .setDescription("the amount of basic armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.INTEGER)
                .setName("wound-armor")
                .setDescription("the amount of wound armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.INTEGER)
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
            final long effectiveStunArmor = Math.max(stunArmor, (long) Math.ceil((double) basicArmor / 2));
            final long effectiveWoundArmor = Math.max(woundArmor, basicArmor / 2);
            final Triple<Long, Long, Long> damageTriple = calculateDamage(type, damageCount, resilience, effectiveStunArmor, effectiveWoundArmor);
            final EmbedBuilder embed = createDamageEmbed(damageTriple.getLeft(), damageTriple.getMiddle(), damageTriple.getRight());
            immediateResponseBuilder.addEmbed(embed).setFlags(InteractionCallbackDataFlag.EPHEMERAL).respond();
        }
        else {
            immediateResponseBuilder.setContent("invalid damage type and / or count!").setFlags(InteractionCallbackDataFlag.EPHEMERAL).respond();
        }
    }

    /**
     * Creates an embed that specifies the amount of points to mark off
     *
     * @param resilienceSpent The amount of resilience spent to blunt the damage
     * @param stun            The amount of stun suffered from the attack
     * @param wounds          The amount of wounds suffered from the attack
     * @return An embed that specifies the amount of points for resilience, stun, and wounds to mark off
     */
    private EmbedBuilder createDamageEmbed(long resilienceSpent, long stun, long wounds) {
        // TODO Add more lines
        final EmbedBuilder embed = new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setTitle("Ouch! That hurt!")
                .setDescription("Mark off the following from your character sheet:");
        if (resilienceSpent != 0) {
            embed.addField("Resilience", String.valueOf(resilienceSpent));
        }
        if (stun != 0) {
            embed.addField("Stun", String.valueOf(stun));
        }
        if (wounds != 0) {
            embed.addField("Wounds", String.valueOf(wounds));
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
     * @param woundArmor Your wound armor rating
     * @return A triple that contains the amount of resilience used on the left, the amount of stun inflicted in the middle, and the amount of wounds inflicted on the right
     */
    public Triple<Long, Long, Long> calculateDamage(DamageType type, long count, long resilience, long stunArmor, long woundArmor) {
        // Resilience affects the damage count directly, before armor
        final Pair<Long, Long> damage = splitDamage(type, count - resilience);
        long resilienceUsed = Math.min(count, resilience);
        long stunMitigated = Math.min(damage.getLeft(), stunArmor);
        long woundMitigated = Math.min(damage.getRight(), woundArmor);
        return Triple.of(resilienceUsed, damage.getLeft() - stunMitigated, damage.getRight() - woundMitigated);
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
