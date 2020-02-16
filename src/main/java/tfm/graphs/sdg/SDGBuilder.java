package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.arcs.Arc;
import tfm.graphs.pdg.PDG;
import tfm.nodes.GraphNode;
import tfm.utils.Context;

import java.util.Map;
import java.util.Optional;

class SDGBuilder extends VoidVisitorAdapter<Context> {

    SDG sdg;

    public SDGBuilder(SDG sdg) {
        this.sdg = sdg;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Context context) {
        if (!methodDeclaration.getBody().isPresent()) {
            return;
        }

        context.setCurrentMethod(methodDeclaration);

        // Build PDG and add to SDGGraph
        PDG pdg = new PDG();
        pdg.build(methodDeclaration);

        for (GraphNode<?> node : pdg.vertexSet()) {
            sdg.addNode(node, false);
        }

        for (Arc arc : pdg.edgeSet()) {
            GraphNode<?> from = pdg.getEdgeSource(arc);
            GraphNode<?> to = pdg.getEdgeTarget(arc);

            if (arc.isControlDependencyArc()) {
                sdg.addControlDependencyArc(from, to);
            } else {
                sdg.addDataDependencyArc(from, to, arc.getLabel());
            }
        }

        Optional<GraphNode<MethodDeclaration>> optionalMethodDeclarationNode = pdg.getRootNode();

        if (!optionalMethodDeclarationNode.isPresent()) {
            return; // Should not happen
        }

        GraphNode<MethodDeclaration> methodDeclarationNode = optionalMethodDeclarationNode.get();

        for (Parameter parameter : methodDeclaration.getParameters()) {
            // In node
            AssignExpr inAssignExpr = new AssignExpr();
            ExpressionStmt inExprStmt = new ExpressionStmt(inAssignExpr);

            inAssignExpr.setTarget(
                    new VariableDeclarationExpr(
                            parameter.getType(),
                            parameter.getNameAsString()
                    )
            );

            inAssignExpr.setOperator(AssignExpr.Operator.ASSIGN);

            inAssignExpr.setValue(new NameExpr(parameter.getNameAsString() + "_in"));

            GraphNode<ExpressionStmt> inNode = sdg.addNode(inAssignExpr.toString(), inExprStmt);

            sdg.addControlDependencyArc(methodDeclarationNode, inNode);

            // Out node
            AssignExpr outAssignExpr = new AssignExpr();
            ExpressionStmt outExprStmt = new ExpressionStmt(outAssignExpr);

            outAssignExpr.setTarget(
                    new VariableDeclarationExpr(
                            parameter.getType(),
                            parameter.getNameAsString() + "_out"
                    )
            );

            outAssignExpr.setOperator(AssignExpr.Operator.ASSIGN);

            outAssignExpr.setValue(new NameExpr(parameter.getNameAsString()));

            GraphNode<ExpressionStmt> outNode = sdg.addNode(outAssignExpr.toString(), outExprStmt);

            sdg.addControlDependencyArc(methodDeclarationNode, outNode);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Context context) {
//        if (sdgGraph.getRootNode() != null) {
//            throw new IllegalStateException("¡Solo podemos procesar una clase por el momento!");
//        }

        if (classOrInterfaceDeclaration.isInterface()) {
            throw new IllegalArgumentException("¡Las interfaces no estan permitidas!");
        }

        context.setCurrentClass(classOrInterfaceDeclaration);

        classOrInterfaceDeclaration.getMembers().accept(this, context);

        // Once every PDG is built, expand method declaration nodes of each one
        // todo methodDeclaration replacer


        // Once every PDG is built, expand method call nodes of each one
        // and link them to the corresponding method declaration node
        MethodCallReplacer methodCallReplacer = new MethodCallReplacer(sdg);
        methodCallReplacer.replace();



        // 3. Build summary arcs
    }

    @Override
    public void visit(CompilationUnit compilationUnit, Context context) {
        context.setCurrentCU(compilationUnit);

        super.visit(compilationUnit, context);
    }
}
