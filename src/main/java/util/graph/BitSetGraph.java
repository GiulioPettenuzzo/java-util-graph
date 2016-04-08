package util.graph;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class BitSetGraph extends Graph {

    Map<Vertex, RoaringBitmap> vertices = new HashMap<>();

    @Override
    public void addEdge(Integer source, Integer target) {
        RoaringBitmap
                s = vertices.getOrDefault(vertex(source), new RoaringBitmap()),
                t = vertices.getOrDefault(vertex(target), new RoaringBitmap());
        s.add(target);
        t.add(source);
        vertices.put(vertex(source), s);
        vertices.put(vertex(target), t);
    }

    @Override
    public int degreeOf(Vertex vertex) {
        return vertices.get(vertex).getCardinality();
    }

    @Override
    public Stream<Vertex> neighborsOf(Vertex v) {
        Iterator<Integer> iter = vertices.get(v).iterator();
        return Stream2.generate(iter::next, iter::hasNext).map(i -> new
                Vertex(i, this));
    }

    @Override
    public int numEdges() {
        Optional<Integer> numEdges = vertices.values().parallelStream().map
                (RoaringBitmap::getCardinality).reduce(Integer::sum);
        return numEdges.isPresent() ? numEdges.get()/2 : 0;
    }

    @Override
    public int numVertices() {
        return vertices.size();
    }

    @Override
    public void optimize() {
        vertices.values().parallelStream().forEach( x -> {
            x.runOptimize();
            x.trim();
        } );
    }

    @Override
    public Vertex vertex(Integer v) {
        return new Vertex(v, this);
    }

    @Override
    public Join join(Partition partition) {
        Set<Overlap> overlaps = new HashSet<>();
        Map<Vertex, Integer> vertexSize = new HashMap<>(partition.size());
        Map<Edge, Integer> edgeSize = new HashMap<>(partition.size()
                *(partition.size()-1)/2);
        Graph joined = new BitSetGraph();
        // row major diagonal
        for (int i = 0; i < partition.size(); i++) {
            Block source = partition.get(i);
            vertexSize.put(joined.vertex(i), source.size());
            // using list to calculate number of edges between blocks
            List<Vertex> cut = source.parallelStream().flatMap
                    (Vertex::neighbors).collect(Collectors.toList());
            for (int j = i; j < partition.size(); j++) {
                Block target = partition.get(j);
                if (i != j) {
                    int overlap = (int) source.stream()
                            .filter(target::contains).count();
                    if (overlap > 0)
                        overlaps.add(new Overlap(joined.vertex(i),
                                joined.vertex(j), overlap));
                }
                int sourceTargetDegree = (int) cut.stream()
                        .filter(target::contains)
                        .count();
                if (sourceTargetDegree > 0) {
                    joined.addEdge(i, j);
                    // divide by two because undirected
                    edgeSize.put(joined.edge(i, j), sourceTargetDegree / 2);
                }
            }
        }
        return new Join() {
            @Override
            public int size(Vertex v) {
                return vertexSize.get(v);
            }
            @Override
            public int size(Edge e) {
                Integer size = edgeSize.get(e);
                if (size != null) {
                    return size;
                }
                return 0;
            }
            @Override
            public Set<Overlap> overlap() {
                return overlaps;
            }
            @Override
            public Graph graph() {
                return joined;
            }
        };
    }

    @Override
    public Set<Vertex> vertices() {
        return vertices.keySet();
    }

    @Override
    public Edge edge(Integer source, Integer target) {
        return new Edge(vertex(source), vertex(target));
    }

    @Override
    public List<Vertex> selectSeeds(int distance) {
        if (distance < 1) throw new IllegalArgumentException("expected " +
                "integer with higher value than 0");
        List<Vertex> seeds = new ArrayList<>();
        RoaringBitmap visited = new RoaringBitmap();
        while (visited.getCardinality() < numVertices()) {
            Integer unvisitedVertex = vertices().stream()
                    .map(Vertex::index)
                    .filter( i -> !visited.contains(i) )
                    .findFirst().get();
            seeds.add(vertex(unvisitedVertex));
            RoaringBitmap visitor = new RoaringBitmap();
            visitor.add(unvisitedVertex);
            for (int d = distance; d > 0; d--) {
                // TODO: is xor approach is better? following loop
                // would be smaller.
                visitor.forEach((IntConsumer) v -> {
                    visitor.or(vertices.get(vertex(v)));
                } );
            }
            visited.or(visitor);
        }
        return seeds;
    }
}
