package util.graph;

import java.util.stream.Stream;

public class Vertex implements Comparable<Vertex> {

    private Graph host;
    private int index;

    public Vertex(int index, Graph host) {
        this.index = index;
        this.host = host;
    }

    @Override
    public int compareTo(Vertex o) {
        return Integer.compare(index, o.index);
    }

    public Stream<Vertex> neighbors() {
        return host.neighborsOf(this);
    }

    public int index() { return index; }

    public int degree() {
        return host.degreeOf(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return index;
    }

//    @Override
//    public String toString() {
//        return String.valueOf(index);
//    }

}
