package util.graph;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by joelmo on 19/04/16.
 */
public class Block extends HashSet<Vertex> {

    public Block() {
        super();
    }

    public Block(Block b) {
        super(b);
    }

}
