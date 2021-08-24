/**
 * A simple library that implements BFS, comparing graphs and subgraphs, and average path length.
 * Created Fall 2020 for COSC 10
 * @author Shashank Patil (Shashank.S.Patil.22@dartmouth.edu)
 * @author Isaac Feldman (Isaac.C.Feldman.23@dartmouth.edu)
 */

import java.util.*;

public class GraphLibrary<V,E> {

    /**
     * Perform Breadth First Search on a graph to create a tree of the shortest connected paths to the source vertex.
     * @param g a graph
     * @param source a vertex in graph g to begin BFS
     * @param <V>
     * @param <E>
     * @return a directed tree pointing to the root with the shortest paths between all reachable vertices from source in g
     */
    public static <V,E>Graph<V,E> BFS(Graph<V,E> g, V source) {
        AdjacencyMapGraph<V,E> backTrack =  new AdjacencyMapGraph<V,E>();

        backTrack.insertVertex(source); //load start vertex with null parent
        Set<V> visited = new HashSet<V>(); //Set to track which vertices have already been visited
        Queue<V> queue = new LinkedList<V>(); //queue to implement BFS

        queue.add(source); //enqueue start vertex
        visited.add(source); //add start to visited Set
        while (!queue.isEmpty()) { //loop until no more vertices
            V u = queue.remove(); //dequeue
            for (V v : g.outNeighbors(u)) { //loop over out neighbors
                if (!visited.contains(v)) { //if neighbor not visited, then neighbor is discovered from this vertex
                    visited.add(v); //add neighbor to visited Set
                    queue.add(v); //enqueue neighbor
                    backTrack.insertVertex(v);
                    backTrack.insertDirected(v, u, g.getLabel(u,v)); //save that this vertex was discovered from prior vertex
                }
            }
        }
        return backTrack;
    }

    /**
     * Get a path between a non-root vertex and the root in a polytree
     * @param tree a tree with directed edges pointed towards the root
     * @param v any vertex in that tree
     * @param <V>
     * @param <E>
     * @return a list of vertices starting at v and ending at the root node
     */
    public static <V,E> List<V> getPath(Graph<V,E> tree, V v){
        List<V> path = new ArrayList<V>();
        if (!tree.hasVertex(v)){
            return path;
        }
        path.add(v);
        V location = v;
        while (tree.outDegree(location)!=0){
            V next = tree.outNeighbors(location).iterator().next();
            path.add(next);
            location=next;
        }
        return path;
    }

    /**
     * Obtain a set of all vertices not connected to the shortest path tree.
     * Runtime: O(n) where n is the total number of vertices.
     * @param graph a graph containing all vertices
     * @param subgraph a subgraph (in most cases a tree) of the main graph
     * @param <V>
     * @param <E>
     * @return A set containing all vertices not connected to the subgraph that are in the main graph.
     */
    public static <V,E> Set<V> missingVertices(Graph<V,E> graph, Graph<V,E> subgraph){
        Set<V> res = new HashSet<V>();
        for (V vertex: graph.vertices()){       //grab a vertex from the first graph
            if (!subgraph.hasVertex(vertex)){  //call the subgraph's hasVertex on that random vertex
                res.add(vertex);               //if it returns false, add it to the set we're gonna return
            }
        }
        return res;
    }

    /**
     * Calculate the average path length to all connected nodes
     * @param tree the shortest path tree
     * @param root the root vertex of that shortest path tree
     * @param <V>
     * @param <E>
     * @return the average path length from the root node
     */
    public static <V,E> double averageSeparation(Graph<V,E> tree, V root){
        // avgSep = totalPathLength / numberofVertices
        return (double) averageSeparationHelper(tree, root, 0)/tree.numVertices();
     }


    private static <V,E> int averageSeparationHelper(Graph<V,E> tree, V currentVertex, int level){
        int sepChildren = 0;
        for (V vertex : tree.inNeighbors(currentVertex)){
            sepChildren += averageSeparationHelper(tree, vertex, level + 1);
        }
        return level + sepChildren;
    }


}
