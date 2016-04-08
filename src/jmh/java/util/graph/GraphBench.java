package util.graph;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.management.RuntimeErrorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
public class GraphBench {

    private Graph g;
    private Stream<Vertex> seeds;

//    @Param({"hep-th.graph", "tiny_01.graph", "pprcd.py.graph"})
    @Param({"hep-th.graph"})
    private String graph;

    @Param({"0.99"})
    private double alpha;

    @Param({"0.01"})
    private double tolerance;

    @Param({"4", "8", "16", "32", "64", "128"})
    private int distance;

    @Setup(Level.Invocation)
    public void setup() throws IOException {
        g = new BitSetGraph().loadFrom(new File("src/test/resources/" + graph));
        List<Vertex> selection = g.selectSeeds(distance);
        seeds = selection.parallelStream();
    }

    @Benchmark
    public Partition parStream(GraphBench state) {
        return state.seeds.map(s ->
                g.communityDetectionPpr(s, alpha, tolerance))
                .collect(Partition::new,
                        Partition::add,
                        Partition::addAll);
    }

    @Benchmark
    public List<Vertex> seedSelection() {
        return g.selectSeeds(distance);
    }


    @Benchmark
    public Partition seqStream() {
        return seeds.sequential().map(s ->
                g.communityDetectionPpr(s, alpha, tolerance))
                .collect(Partition::new,
                        Partition::add,
                        Partition::addAll);
    }

    @Benchmark
    public Partition executorWorkStealing() throws InterruptedException {
        Partition p = new Partition();
        ExecutorService service = Executors.newWorkStealingPool();
        seeds.forEach( s -> {
            service.submit(() -> p.add(g.communityDetectionPpr(s, alpha,
                    tolerance)));
        });
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
        return p;
    }

    @Benchmark
    public Partition executorCachedThread() throws InterruptedException {
        Partition p = new Partition();
        ExecutorService service = Executors.newCachedThreadPool();
        seeds.forEach( s -> {
            service.submit(() -> p.add(g.communityDetectionPpr(s, alpha,
                    tolerance)));
        });
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
        return p;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(GraphBench.class.getSimpleName()
                        + ".(parStream|seqStream)")
                .syncIterations(true)
                .forks(5)
                .measurementIterations(5)
                .warmupForks(0)
                .warmupIterations(0)
//                .addProfiler(LinuxPerfNormProfiler.class)
                .resultFormat(ResultFormatType.CSV)
                .result(GraphBench.class.getName()+".csv")
                .build();
        new Runner(opt).run();
    }

}
