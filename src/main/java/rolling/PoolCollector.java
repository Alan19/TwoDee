package rolling;

import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class PoolCollector implements Collector<Triple<List<Dice>, Integer, String>, Triple<List<Dice>, List<Integer>, List<String>>, Triple<List<Dice>, List<Integer>, List<String>>> {
    public static PoolCollector toTripleList() {
        return new PoolCollector();
    }

    @Override
    public Supplier<Triple<List<Dice>, List<Integer>, List<String>>> supplier() {
        return () -> Triple.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * An accumulator to accumulate the incoming triples into a single triple with 3 lists. Incoming elements are not merged if they are null.
     *
     * @return A BiConsumer with instructions on how to merge incoming triples into the container
     */
    @Override
    public BiConsumer<Triple<List<Dice>, List<Integer>, List<String>>, Triple<List<Dice>, Integer, String>> accumulator() {
        return (list, incomingTriple) -> {
            if (incomingTriple.getLeft() != null) {
                list.getLeft().addAll(incomingTriple.getLeft());
            }
            else if (incomingTriple.getMiddle() != null) {
                list.getMiddle().add(incomingTriple.getMiddle());
            }
            else if (incomingTriple.getRight() != null) {
                list.getRight().add(incomingTriple.getRight());
            }
        };
    }

    @Override
    public BinaryOperator<Triple<List<Dice>, List<Integer>, List<String>>> combiner() {
        return (list1, list2) -> {
            list1.getLeft().addAll(list2.getLeft());
            list1.getMiddle().addAll(list2.getMiddle());
            list1.getRight().addAll(list2.getRight());
            return list1;
        };
    }

    @Override
    public Function<Triple<List<Dice>, List<Integer>, List<String>>, Triple<List<Dice>, List<Integer>, List<String>>> finisher() {
        return listListListTriple -> listListListTriple;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return new HashSet<>(Collections.singletonList(Characteristics.IDENTITY_FINISH));
    }
}
