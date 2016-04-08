package util.graph;

import java.util.List;
import java.util.Set;

public interface Join {

    /**
     * Number of vertices joined to vertex v.
     */
    public int size(Vertex v);

    /**
     * Number of edges between blocks. Loops indicate internal degree.
     */
    public int size(Edge e);

    public Set<Overlap> overlap();

    public Graph graph();
}
