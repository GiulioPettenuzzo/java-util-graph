package util.graph;

public class Overlap extends Edge {

    int overlap;

    public int overlap() {
        return overlap;
    }

    Overlap(Vertex source, Vertex target, int overlap) {
        super(source, target);
        this.overlap = overlap;
    }

}
