package logic;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;

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

    public static Triple<Integer, Integer, Integer> calculateDamage(DamageType type, int count, int resilience, int stunArmor, int basicArmor, int woundArmor) {
        final Pair<Integer, Integer> damage = splitDamage(type, count - resilience);
        int resilienceUsed = Math.min(count, resilience);
        int stunMitigated = Math.min(damage.getLeft(), stunArmor);
        int woundMitigated = Math.min(damage.getRight(), woundArmor);
        int basicMitigated = calculateBasicMitigation(damage, basicArmor);
        if (basicMitigated >= stunMitigated && basicMitigated >= woundMitigated) {
            return Triple.of(resilienceUsed, damage.getLeft() - (int) Math.ceil((double) basicArmor / 2), Math.min(damage.getRight(), basicArmor / 2));
        }
        else if (woundMitigated >= stunMitigated) {
            return Triple.of(resilienceUsed, damage.getLeft(), damage.getRight() - woundMitigated);
        }
        else {
            return Triple.of(resilienceUsed, damage.getLeft() - stunMitigated, damage.getRight());
        }
    }

    private static int calculateBasicMitigation(Pair<Integer, Integer> damage, int basicArmor) {
        int stunMitigated = Math.min(damage.getLeft(), (int) Math.ceil((double) basicArmor / 2));
        int woundsMitigated = Math.min(damage.getRight(), basicArmor / 2);
        return stunMitigated + woundsMitigated;
    }

    public static Pair<Integer, Integer> splitDamage(DamageType type, int count) {
        if (type == DamageType.STUN) {
            return Pair.of(count, 0);
        }
        else if (type == DamageType.WOUND) {
            return Pair.of(0, count);
        }
        else {
            return Pair.of((int) Math.ceil((double) count / 2), count / 2);
        }
    }

    @Override
    public void onEvent(SlashCommandInteraction interaction, User user, VelenArguments arguments, List<SlashCommandInteractionOption> list, InteractionImmediateResponseBuilder immediateResponseBuilder) {

        final Optional<String> typeOptional = interaction.getOptionStringValueByName("type");
        final Optional<Integer> countOptional = interaction.getOptionIntValueByName("count");
        if (typeOptional.isPresent() && countOptional.isPresent()) {
            final DamageType type = DamageType.getType(typeOptional.get());
            int stunArmor = interaction.getOptionIntValueByName("stun-armor").orElse(0);
            int basicArmor = interaction.getOptionIntValueByName("basic-armor").orElse(0);
            int woundArmor = interaction.getOptionIntValueByName("wound-armor").orElse(0);
            int resilience = interaction.getOptionIntValueByName("resilience").orElse(0);

            // Resilience affects the damage count directly
            final Triple<Integer, Integer, Integer> damageResult = calculateDamage(type, countOptional.get(), resilience, stunArmor, basicArmor, woundArmor);
            final EmbedBuilder embed = new EmbedBuilder().setTitle("Damage Calculator!").setDescription("Mark off the following points from your character sheet:");
            if (damageResult.getLeft() != 0) {
                embed.addField("Resilience", String.valueOf(damageResult.getLeft()));
            }
            if (damageResult.getMiddle() != 0) {
                embed.addField("Stun", String.valueOf(damageResult.getMiddle()));
            }
            if (damageResult.getRight() != 0) {
                embed.addField("Wounds", String.valueOf(damageResult.getRight()));
            }
            immediateResponseBuilder.addEmbed(embed).setFlags(MessageFlag.EPHEMERAL).respond();
        }
        else {
            immediateResponseBuilder.setContent("invalid damage type and / or count!").setFlags(MessageFlag.EPHEMERAL).respond();
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
