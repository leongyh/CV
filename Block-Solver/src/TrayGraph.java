import java.util.Stack;
import java.util.HashMap;
import java.util.ArrayList;

public class TrayGraph {

    private Vertex startVertex;
    private Vertex currentVertex;
    private HashMap<Integer, Vertex> map;

    public TrayGraph(Tray1 t) {
        startVertex = new Vertex(t);
        currentVertex = startVertex;
        map = 
    }
    
    public void generate(){
        
    }

    public Tray1 getTray() {
        return currentVertex.getTray();
    }
    
    private static void addVertex(Vertex v){
        
    }

    public class Vertex {

        private Tray1 state;
        private ArrayList<Vertex> neighbors = new ArrayList<Vertex>();
        private boolean hasVisited;

        public Vertex(Tray1 t) {
            this.state = t;
        }

        public void addNeighbor(Vertex v) {
            this.neighbors.add(v);
        }

        public Tray1 getTray() {
            return this.state;
        }
        
        public int hashCode(){
            return this.state.hashCode();
        }
    }
}
