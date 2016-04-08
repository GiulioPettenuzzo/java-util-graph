import org.docopt.Docopt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import util.graph.*;

import static java.util.stream.Collectors.*;

public class Pprcd {

    public static void main(String[] args) throws IOException {
        args = new String[]{
                "src/test/resources/pprcd.py.graph",
                "-s", "1,10",
                "--stats"
        };
        Map<String, Object> opts = new Docopt(ClassLoader
                .getSystemResourceAsStream("README"))
                .withVersion("pprcd 0.1").parse(args);

        final String FILE = (String) opts.get("FILE");
        Graph g = new BitSetGraph().loadFrom(
                new File(FILE));

        List<Vertex> seeds = Arrays.stream(((String) opts.get("-s")).split(","))
                .map(Integer::valueOf)
                .map(g::vertex)
                .collect(toList());

        Partition communities = g.communityDetectionPpr(
                seeds,
                Double.valueOf((String) opts.get("-a")),
                Double.valueOf((String) opts.get("-t")));

        communities.forEach( c -> {
            String members = c.stream()
                    .map(Vertex::index)
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "));
            System.out.println(members);
        });

        if ((Boolean) opts.get("--stats")) {
            PrintWriter matrixWriter = new PrintWriter(
                    FILE + "-block-matrix.csv","UTF-8"),
                    sizeWriter = new PrintWriter(FILE + "-block-size.csv", "UTF-8");
            Join join = g.join(communities);
            Graph jg = join.graph();
            List<Vertex> jvertices = new ArrayList<>(jg.vertices());
            for (int i = 0; i < jg.numVertices(); i++) {
                if (i > 0) sizeWriter.print(",");
                sizeWriter.print(join.size(jg.vertex(i)));
                for (int j = 0; j < jg.numVertices(); j++) {
                    if (j > 0) matrixWriter.print(",");
                    matrixWriter.print(join.size(jg.edge(i, j)));
                }
                matrixWriter.println();
            }
            sizeWriter.println();
            matrixWriter.close();
            sizeWriter.close();
            // TODO write out overlap

        }

    }
}

