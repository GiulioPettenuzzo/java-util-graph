package util.graph;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * TODO: switch with protonpack (https://github.com/poetix/protonpack)
 */
public class Stream2 {

    public static<T> Stream<T> generate(Supplier<T> s, Supplier<Boolean> limiter) {
        Objects.requireNonNull(s);
        Objects.requireNonNull(limiter);
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>
                (Long.MAX_VALUE, 0) {

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                boolean cont = limiter.get();
                if (cont) action.accept(s.get());
                return cont;
            }
        }, true);
    }

    public static<T> Stream<T> generate(Supplier<T> s, Function<T, Boolean>
            l) {
        Objects.requireNonNull(s);
        Objects.requireNonNull(l);
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>
                (Long.MAX_VALUE, 0) {

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                T current = s.get();
                boolean cont = l.apply(current);
                if (cont) action.accept(current);
                return cont;
            }
        }, true);
    }

    public static <E> Stream<List<E>> combinations(List<E> l, int size) {
        if (size == 0) {
            return Stream.of(Collections.emptyList());
        } else {
            return IntStream.range(0, l.size()).boxed().
                    <List<E>> flatMap(i -> combinations(l.subList(i+1, l.size()), size - 1).map(t -> pipe(l.get(i), t)));
        }
    }

    private static <E> List<E> pipe(E head, List<E> tail) {
        List<E> newList = new ArrayList<>(tail);
        newList.add(0, head);
        return newList;
    }

    public static void main(String[] args) {
        System.out.println(Stream2.combinations(Arrays.asList("a", "b", "c",
                "d"), 2).collect(Collectors.toList()));
    }

}
