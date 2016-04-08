package util.graph;

import java.util.Arrays;
import java.util.Objects;

/**
 * Undirected edge
 */
class Edge {

    private Vertex source, target;

    /**
     * Construct undirected edge, using source and target as endpoints.
     * The vertex with the minimum index will be set as source.
     */
    Edge(final Vertex source, final Vertex target) {
        if (source.compareTo(target) == 1) {
            this.source = source;
            this.target = target;
        } else {
            this.source = target;
            this.target = source;
        }
    }

    public Vertex source() {
        return source;
    }

    public Vertex target() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Edge) {
            Edge o = (Edge) obj;
            Vertex os = o.source(), ot = o.target(),
                    s = this.source(), t = this.target();
            return s.equals(t);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

}
