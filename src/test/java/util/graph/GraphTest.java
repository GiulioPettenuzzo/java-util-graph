package util.graph;

import org.junit.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class GraphTest {

    public File resourceToFile(String resource) {
        return new File(ClassLoader.getSystemResource(resource).getFile());
    }

    @Test
    public void testCommunityDetection() throws Exception {

        // test graph without self loops
        Graph graph = new BitSetGraph().loadFrom(resourceToFile("pprcd.py.graph"));
        Block community = graph.communityDetectionPpr(graph.vertex(1));
        assertTrue(community.size() == 9);

        // testmethod translated from NetworkKit
        graph = new BitSetGraph().loadFrom(resourceToFile("hep-th.graph"));
        community = graph.communityDetectionPpr(graph.vertex(50), 0.1, 1e-5);
        assertTrue(graph.conductance(community) < 0.5); // originally 0.4,
        // but this returns 0.466...
    }

    @Test
    public void testParallelCommunityDetection() throws Exception {

        // test graph without self loops
        Graph graph = new BitSetGraph().loadFrom(resourceToFile("pprcd.py.graph"));
        List<Vertex> seeds = Arrays.asList(graph.vertex(1), graph
                .vertex(2));
        Partition community = graph.communityDetectionPpr(seeds, 0.99, 0.01);
        assertThat(community.get(0).size(), is(9));
        assertThat(community.get(0), is(community.get(1)));

    }


        @Test
    public void testVertex() throws Exception {
        Graph g = new BitSetGraph();
        assertEquals(g.vertex(1), g.vertex(1));
        g.addEdge(1, 2);
        g.addEdge(2, 1);
        assertThat(g.vertex(1).degree(), is(1));
    }

    @Test
    public void testRead() throws Exception {
        Graph graph = new BitSetGraph().loadFrom(resourceToFile("tiny_01.graph"));
        assertThat(graph.numVertices(), is(7));
        assertThat(graph.numEdges(), is(11));
        Vertex v1 = graph.vertex(1);
        assertTrue(v1.neighbors().collect(Collectors.toList())
                .containsAll(Arrays
                .asList(graph.vertex(5),
                        graph.vertex(3),
                        graph.vertex(2))));
    }

    @Test
    public void testJoin() throws Exception {
        Graph g = new BitSetGraph();
        g.addEdge(1, 2);
        g.addEdge(1, 3);
        Partition p = new Partition();
        Block b1 = new Block(), b2 = new Block();
        b1.add(g.vertex(1)); b1.add(g.vertex(2));
        b2.add(g.vertex(1)); b2.add(g.vertex(3));
        p.add(b1); p.add(b2);
        Join j = g.join(p);
        Graph jg = j.graph();
        assertThat(jg.numVertices(), is(2));
        assertThat(jg.numEdges(), is(2));
        assertThat(j.size(jg.vertex(0)), is(2));
        assertThat(j.size(jg.vertex(1)), is(2));
        assertThat(j.overlap().size(), is(1));
        assertThat(j.size(jg.edge(0, 0)), is(1));
    }

    @Test
    public void testSelectSeeds() throws Exception {
        Graph graph = new BitSetGraph().loadFrom(resourceToFile("tiny_01.graph"));
        List<Vertex> seeds = graph.selectSeeds(2);
        assertThat(seeds.size(), is(2));
        seeds = graph.selectSeeds(3);
        assertThat(seeds.size(), is(1));
    }

}