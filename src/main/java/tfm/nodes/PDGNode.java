package tfm.nodes;

import com.github.javaparser.ast.stmt.Statement;
import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.graphs.Graph;
import tfm.graphs.PDGGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PDGNode extends Node {

    public PDGNode(Graph.NodeId id, String data, Statement statement) {
        super(id, data, statement);
    }

    public String toString() {
        List<Integer> dataFrom = new ArrayList<>();
        List<Integer> dataTo = new ArrayList<>();
        List<Integer> controlFrom = new ArrayList<>();
        List<Integer> controlTo = new ArrayList<>();

        getIncomingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;
            Node from = (Node) arc.getFrom();

            if (arc.isDataDependencyArrow()) {
                dataFrom.add(from.getId());
            } else if (arc.isControlDependencyArrow()) {
                controlFrom.add(from.getId());
            }

        });

        getOutgoingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;
            Node to = (Node) arc.getTo();

            if (arc.isDataDependencyArrow()) {
                dataTo.add(to.getId());
            } else if (arc.isControlDependencyArrow()) {
                controlTo.add(to.getId());
            }

        });

        return String.format("PDGNode{id: %s, data: %s, dataFrom: %s, dataTo: %s, controlFrom: %s, controlTo: %s}",
                getId(),
                getData(),
                dataFrom,
                dataTo,
                controlFrom,
                controlTo
        );
    }

    public List<ControlDependencyArc> getControlDependencies() {
        return getIncomingArrows().stream()
                .filter(arrow -> ((Arc) arrow).isControlDependencyArrow())
                .map(arc -> (ControlDependencyArc) arc)
                .collect(Collectors.toList());
    }

    public int getLevel() {
        return getLevel(this);
    }

    private int getLevel(PDGNode node) {
        List<ControlDependencyArc> dependencies = node.getControlDependencies();

        if (dependencies.isEmpty())
            return 0;

        return 1 + getLevel((PDGNode) dependencies.get(0).getFrom());
    }
}