package logic;

import advancement.GeneralSkillCalculator;
import advancement.SpecialtySkill;
import io.vavr.Tuple4;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdvancementLogic implements VelenSlashEvent {
    public static void setupAdvancementCommand(Velen velen) {
        final List<SlashCommandOption> options = new ArrayList<>();
        final SlashCommandOptionBuilder startingArray = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.STRING)
                .setName("starting-array")
                .setDescription("The current array of specialty skills, e.g. use 4 6 8 if you have a d4 d6 d8")
                .setRequired(false);
        final SlashCommandOptionBuilder targetFacets = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("target-facets")
                .setDescription("The amount of facets to target for the general skill")
                .setRequired(true);
        final SlashCommandOptionBuilder targetExpert = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.BOOLEAN)
                .setName("target-expert")
                .setDescription("Attempts to ensure at least one specialty skill reaches d14, defaults to false")
                .setRequired(false);
        final SlashCommandOptionBuilder minimumFacets = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("minimum-facets")
                .setDescription("Ensures as many specialty skills as possible are above the specified facet count, defaults to 12")
                .setRequired(false);
        final SlashCommandOptionBuilder nervewrightMana = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("nervewright-mana")
                .setDescription("Specifies the amount of mana that can be invested for Nervewright, defaults to 0")
                .setRequired(false);
        final SlashCommandOptionBuilder adolescentInterests = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.BOOLEAN)
                .setName("adolescent-interests")
                .setDescription("Set to true to factor in the effects of adolescent interests, defaults to false")
                .setRequired(false);
        final SlashCommandOptionBuilder nonEphemeral = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.BOOLEAN)
                .setName("non-ephemeral")
                .setDescription("Set to true to send the message to the channel, defaults to false")
                .setRequired(false);
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "tall", "Calculates the skill array to reach a given general skill by going tall.", targetFacets, startingArray, targetExpert, minimumFacets, nervewrightMana, adolescentInterests, nonEphemeral));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "wide", "Calculates the skill array to reach a given general skill facet as cheaply as possible.", targetFacets, startingArray, targetExpert, nervewrightMana, adolescentInterests, nonEphemeral));
        VelenCommand.ofSlash("advancement", "Calculates the AP cost to get a general skill", velen, new AdvancementLogic())
                .addOptions(options.toArray(new SlashCommandOption[]{}))
                .setServerOnly(true, 817619574450028554L)
                .attach();
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        // Generate Validation objects on objects that need validation
        final SlashCommandInteractionOption subcommandOption = event.getOptions().get(0);
        final Optional<Long> targetFacets = subcommandOption.getOptionLongValueByName("target-facets");
        final Validation<Throwable, Long> targetFacetsTry = targetFacets.map(Try::success)
                .orElseGet(() -> Try.failure(new IllegalStateException("target-facets: Unable to find target facets")))
                .flatMap(aLong -> isValidFacetCount(aLong, "target-facets"))
                .toValidation();
        final Validation<Throwable, Seq<Long>> startingArrayValidation = Try.of(() -> getStartingArray(subcommandOption))
                // TODO Try to map the exception to prepend the parameter
                .flatMap(integers -> Try.sequence(integers.stream().map(aLong -> isValidFacetCount(aLong, "target-facets")).collect(Collectors.toList())))
                .toValidation();
        final Validation<Throwable, Long> validateMinimum = Try.success(subcommandOption.getOptionLongValueByName("minimum-facets").orElse(12L))
                .flatMap(aLong -> isValidFacetCount(aLong, "minimum-facets"))
                .toValidation();
        final Validation<Throwable, Long> manaValidation = Try.success(subcommandOption.getOptionLongValueByName("nervewright-mana").orElse(0L))
                .flatMap(aLong -> flatMapPositiveLong(aLong, "nervewright-mana"))
                .toValidation();

        // Gather params that don't need validation and then generate and send output on success, or send error message on failure
        Validation.combine(targetFacetsTry, startingArrayValidation, validateMinimum, manaValidation)
                .ap((target, longs, minimum, mana) -> new Tuple4<>(target, longs.map(Math::toIntExact).asJava(), minimum, mana))
                .fold(throwables -> firstResponder.setFlags(InteractionCallbackDataFlag.EPHEMERAL).setContent(throwables.map(Throwable::getMessage).collect(Collectors.joining("\n"))).respond(),
                        tuple -> {
                            final boolean nonEphemeral = subcommandOption.getOptionBooleanValueByName("non-ephemeral").orElse(false);
                            final boolean tall = subcommandOption.getName().equals("tall");
                            final Boolean expert = subcommandOption.getOptionBooleanValueByName("target-expert").orElse(false);
                            final Boolean adolescentInterests = subcommandOption.getOptionBooleanValueByName("adolescent-interests").orElse(false);

                            List<SpecialtySkill> skills = new GeneralSkillCalculator(tuple._2, expert, tall, tuple._3, adolescentInterests, tuple._4).generate(tuple._1);
                            EmbedBuilder resultsEmbed = getResultEmbed(subcommandOption, tuple._1, skills);

                            firstResponder.addEmbed(resultsEmbed);
                            if (!nonEphemeral) {
                                firstResponder.setFlags(InteractionCallbackDataFlag.EPHEMERAL);
                            }
                            return firstResponder.respond();
                        });

    }

    private EmbedBuilder getResultEmbed(SlashCommandInteractionOption subcommandOption, Long target, List<SpecialtySkill> skills) {
        EmbedBuilder resultsEmbed = new EmbedBuilder();
        resultsEmbed.setTitle("How to get this general skill to f" + target);
        resultsEmbed.setDescription("In order to advance this general skill and go " + subcommandOption.getName() + ", you can raise the specialty skills under the general skill like this (assuming you have a high enough attribute or mentors):");
        final String existingSpecialtyAdvancement = skills.stream()
                .filter(specialtySkill -> specialtySkill.getStartingFacets() != 0)
                .map(specialtySkill -> MessageFormat.format("f{0} → f{1} ({2} AP)", specialtySkill.getStartingFacets(), specialtySkill.getCurrentFacets(), specialtySkill.getAPSpent()))
                .collect(Collectors.joining("\n"));
        if (!existingSpecialtyAdvancement.trim().isEmpty()) {
            resultsEmbed.addInlineField("Existing Specialties", existingSpecialtyAdvancement);
        }
        final String newSpecialtySkillAdvancement = skills.stream()
                .filter(specialtySkill -> specialtySkill.getStartingFacets() == 0)
                .map(specialtySkill -> MessageFormat.format("f{0} ({1} AP)", specialtySkill.getCurrentFacets(), specialtySkill.getAPSpent()))
                .collect(Collectors.joining("\n"));
        if (!newSpecialtySkillAdvancement.trim().isEmpty()) {
            resultsEmbed.addInlineField("New Specialties", newSpecialtySkillAdvancement);
        }
        resultsEmbed.addField("Total AP Cost", String.valueOf(skills.stream().mapToInt(SpecialtySkill::getAPSpent).sum()));
        return resultsEmbed;
    }

    private Try<Long> flatMapPositiveLong(Long aLong, String parameterName) {
        return aLong >= 0 ? Try.success(aLong) : Try.failure(new IllegalArgumentException(parameterName + ": Cannot enter negative mana!"));
    }

    private List<Long> getStartingArray(SlashCommandInteractionOption subcommandOption) {
        return subcommandOption.getOptionStringValueByName("starting-array")
                .map(s -> Arrays.stream(s.split(" ")).map(Long::parseLong).collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }

    private Try<Long> isValidFacetCount(Long aLong, String parameterName) {
        return aLong >= 4 && aLong % 2 == 0 ? Try.success(aLong) : Try.failure(new IllegalStateException(parameterName + ": Facets must be even and greater than or equal to 4"));
    }
}
