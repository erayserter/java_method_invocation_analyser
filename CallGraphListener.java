import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.misc.*;
import java.util.*;

class Node {
    String packageName;
    String className;
    String methodName;

    public Node(String packageName, String className, String methodName) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;

        Node node = (Node) o;
        return node.packageName.equals(this.packageName)
                && node.className.equals(this.className)
                && node.methodName.equals(this.methodName);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public String toString() {
        return "\"" + packageName + "/" + className + "/" + methodName + "\"";
    }
}

public class CallGraphListener extends Java8BaseListener {

    Map<Node, List<Node>> graph = new HashMap<>();
    Set<Node> declaredNodes = new HashSet<>();

    String currentPackage;
    String currentClass;
    String currentMethod;

    @Override
    public void enterPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {
        String packageName = "";
        List<TerminalNode> nodeList = ctx.Identifier();

        for (TerminalNode node : nodeList)
            packageName += ("." + node.getSymbol().getText());

        this.currentPackage = packageName.substring(1);
    }

    @Override
    public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
        this.currentClass = ctx.Identifier().getSymbol().getText();
    }

    @Override
    public void enterMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {
        this.currentMethod = ctx.Identifier().getText();
        Node encounteredMethod = new Node(currentPackage, currentClass, currentMethod);

        if (!graph.containsKey(encounteredMethod)) {
            graph.put(encounteredMethod, new ArrayList<>());
        }

        declaredNodes.add(encounteredMethod);

    }

    @Override
    public void enterA(Java8Parser.AContext ctx) {
        methodInvoked(currentClass, ctx.methodName().getText());
    }

    @Override
    public void enterB(Java8Parser.BContext ctx) {
        methodInvoked(ctx.typeName().getText(), ctx.Identifier().getText());
    }

    public void methodInvoked(String className, String methodName) {
        Node sourceNode = new Node(currentPackage, currentClass, currentMethod);
        Node destinationNode = new Node(currentPackage, className, methodName);

        if (!graph.containsKey(destinationNode))
            graph.put(destinationNode, new ArrayList<>());

        List<Node> sourceNodeList = graph.get(sourceNode);

        sourceNodeList.add(destinationNode);
    }

    public static void main(String[] args) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(System.in);
        Java8Lexer lexer = new Java8Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java8Parser parser = new Java8Parser(tokens);
        ParseTree tree = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        CallGraphListener listener = new CallGraphListener();
        // This is where we trigger the walk of the tree using our listener.
        walker.walk(listener, tree);

        Map<Node, List<Node>> graph = listener.graph;
        Set<Node> declaredNodes = listener.declaredNodes;
        StringBuilder buf = new StringBuilder();
        buf.append("digraph G {\n");
        // ..
        for (Node n : graph.keySet()) {
            buf.append("\t" + n + "[");
            if (declaredNodes.contains(n)) {
                buf.append("color=green,fillcolor=green,style=filled,");
            }
            buf.append("shape=circle]\n");
        }

        for (Node n1 : graph.keySet()) {
            List<Node> values = graph.get(n1);
            for (Node n2 : values) {
                buf.append("\t" + n1 + " -> " + n2 + "\n");
            }
        }
        buf.append("}");

        System.out.println(buf.toString());
    }
}
