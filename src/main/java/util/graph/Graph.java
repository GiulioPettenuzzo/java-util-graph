package util.graph;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Undirected graph.
 */
public abstract class Graph {

    public abstract void addEdge(Integer source, Integer target);

    public abstract int degreeOf(Vertex vertex);

    public abstract Stream<Vertex> neighborsOf(Vertex v);

    public abstract int numEdges();

    public abstract int numVertices();

    public abstract void optimize();

    public abstract Vertex vertex(Integer v);

    public abstract Join join(Partition partition);

    public abstract Set<Vertex> vertices();

    public abstract Edge edge(Integer source, Integer target);

    public abstract List<Vertex> selectSeeds(int distance) throws IllegalArgumentException;

    public Block communityDetectionPpr(Vertex seed) {
        return communityDetectionPpr(seed, 0.99, 0.01);
    }

    /**
     * Parallel community detection
     */
    public Partition communityDetectionPpr(List<Vertex> seeds, double
            alpha, double tolerance) {
        return seeds.parallelStream().map(s ->
                communityDetectionPpr(s, alpha, tolerance))
                .collect(Partition::new,
                        Partition::add,
                        Partition::addAll);
    }

    /**
     * Translation from: https://gist.github.com/dgleich/6201856
     * <p>
     * Algorithm to detect communities using the page-rank nibble
     * method:
     * (1) Compute random walk scores r_u load seed node s using
     * PageRank-nibble.
     * (2) Order nodes u by the decreasing value of r_u/degree(u).
     * (3) Compute the community scoring function f(S_k) of the first k nodes
     * for every k.
     * (4) Detect local minimal of f(S_k).
     *
     * @return set of vertices that makes up a community, the seed is also in
     * this set
     */
    public Block communityDetectionPpr(Vertex seed, double alpha,
                                      double tolerance) {
        Map<Vertex, Double> x = new HashMap<>();
        Map<Vertex, Double> r = new HashMap<>();
        r.put(seed, 1.); // modified
        Queue<Vertex> queue = new ArrayDeque<>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            Vertex v = queue.remove();
            double vx = x.getOrDefault(v, 0.) + (1 - alpha) * r.get(v);
            x.put(v, vx);
            double mass = alpha * r.get(v) / (2 * v.degree());
            v.neighbors() // skip loops
                    .forEach(u -> {
                        double ur = r.getOrDefault(u, 0.);
                        double rdegtol = u.degree() * tolerance;
                        if (ur < rdegtol && (ur + mass) >= rdegtol)
                            queue.add(u);
                        r.put(u, ur + mass);
                    });
            r.put(v, mass * v.degree());
            if (r.get(v) >= v.degree() * tolerance)
                queue.add(v);
        }
        // find cluster, first normalize by degree
        x.forEach((k, v) -> x.put(k, v / k.degree()));
        // sort by values in x, decreasing
        List<Vertex> sv = x.keySet().stream().sorted((o1, o2) -> x.get(o2)
                .compareTo(x.get(o1))).collect(Collectors.toList());
        Block S = new Block();
        double bestcond = 1.;
        int volS = 0;
        int cutS = 0;
        int numEdges = numEdges();
        Block bestBlock = new Block();
        bestBlock.add(sv.get(0));
        for (Vertex s : sv) {
            volS += s.degree(); // add degree to volume
            cutS += s.neighbors().map(v -> S.contains(v) ? -1 : 1)
                    .reduce(Integer::sum).get();
            S.add(s);
            double cond = ((double) cutS) / (2*volS + cutS);
            if (cond < bestcond) {
                bestcond = cond;
                bestBlock = new Block(S);
            }
        }
        return bestBlock;
    }

    public Graph loadFrom(File f) throws IOException {
        if (f.getName().endsWith(".graph")) {
            loadMetis(f);
        }
        optimize();
        return this;
    }

    public Graph loadMetis(File f) throws IOException {
        Iterator<Scanner> lineScanner = Files.lines(f.toPath())
                .filter(s -> !(s.startsWith("%") || s.isEmpty()))
                .map(Scanner::new)
                .iterator();
        {   // process header
            Scanner s = lineScanner.next();
            s.nextInt(); // num vertices
            s.nextInt(); // num edges
            List<Integer> fmt = Stream2.generate(s::nextInt, () -> s.hasNextInt())
                    .collect(Collectors.toList());
            if (fmt.size() > 0 && fmt.get(0) != 0) {
                throw new IllegalArgumentException("Found unsupported format in " +
                        "file: " + fmt.get(0));
            }
        }
        int vertexIndex = 1;
        while (lineScanner.hasNext()) {
            final Scanner s = lineScanner.next();
            int v = vertexIndex++;
            Stream2.generate(s::nextInt, () -> s.hasNextInt()).sequential().forEach(i -> {
                addEdge(v, i);
            });
        }
        return this;
    }

    public Stream<Vertex> cut(Block members) {
        // better to use set difference?
        return members.parallelStream().flatMap(Vertex::neighbors)
                .collect(Collectors.toSet()).parallelStream()
                .filter( v -> !members.contains(v) );
    }

    public double conductance(Block community) {
        long cut = cut(community).count();
        return ((double) cut) / (2*community.size() + cut);
    }


}

