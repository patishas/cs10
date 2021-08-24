/**
 * An implementation of the Kevin Bacon game.
 * Created Fall 2020 for COSC 10
 * @author Shashank Patil (Shashank.S.Patil.22@dartmouth.edu)
 * @author Isaac Feldman (Isaac.C.Feldman.23@dartmouth.edu)
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class KevinBaconGame {

    private Map<String,String> actors;
    private Map<String,String> movies;
    private Map<String,HashSet<String>> movieActors;//movie IDs, sets of actor IDs
    private Graph<String, HashSet<String>> graph, bfsTree;
    private String center;

    //objects that allow reading from files
    private BufferedReader input;

    private String actorFile,movieFile,movieActorFile;


    public KevinBaconGame(String actorFile, String movieFile, String movieActorFile){
        this.actorFile=actorFile;
        this.movieFile=movieFile;
        this.movieActorFile=movieActorFile;
        actors = new HashMap<String,String>();
        movies = new HashMap<String,String>();
        movieActors = new HashMap<String,HashSet<String>>();
        graph = new AdjacencyMapGraph<String, HashSet<String>>();
        bfsTree = new AdjacencyMapGraph<String, HashSet<String>>();
    }

    public void readActors(){
        // Open the file, if possible
        try {
            input = new BufferedReader(new FileReader(actorFile));
        }
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
        }

        try {
            // Line by line
            String line;
            while ((line = input.readLine()) != null) {
                //Extract actor IDs and actors, separated by "|"
                String[] pieces = line.split("\\|");
                actors.put(pieces[0],pieces[1]);
            }
        }
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }

        // Close the file, if possible
        try {
            input.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }
    }

    public void readMovies(){
        // Open the file, if possible
        try {
            input = new BufferedReader(new FileReader(movieFile));
        }
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
        }

        try {
            // Line by line
            String line;
            while ((line = input.readLine()) != null) {
                //Extract movie IDs and actors, separated by "|"
                String[] pieces = line.split("\\|");
                movies.put(pieces[0],pieces[1]);
            }
        }
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }

        // Close the file, if possible
        try {
            input.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }
    }

    public void readMovieActors(){
        // Open the file, if possible
        try {
            input = new BufferedReader(new FileReader(movieActorFile));
        }
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
        }

        try {
            // Line by line
            String line;
            while ((line = input.readLine()) != null) {
                //Extract movie IDs and actor IDs, separated by "|"
                String[] pieces = line.split("\\|");
                if (movieActors.containsKey(pieces[0])){
                    movieActors.get(pieces[0]).add(pieces[1]);
                } else{
                    movieActors.put(pieces[0], new HashSet<String>());
                    movieActors.get(pieces[0]).add(pieces[1]);

                }
            }
        }
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }

        // Close the file, if possible
        try {
            input.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }
    }

    public void buildGraph(){
        for (String name:actors.values()){
            graph.insertVertex(name);
        }
        for (String movie_id : movieActors.keySet()){ // select a random movie
            for (String actor_id: movieActors.get(movie_id)){ // select an actor in that movie
                for (String other_actor_id : movieActors.get(movie_id)){ // select any other actor
                    if(!actor_id.equals(other_actor_id)){
                        if (graph.hasEdge(actors.get(actor_id), actors.get(other_actor_id))) {
                            graph.getLabel(actors.get(actor_id), actors.get(other_actor_id)).add(movies.get(movie_id));
                        }else  {
                            HashSet<String> set = new HashSet<String>();
                            set.add(movies.get(movie_id));
                            graph.insertUndirected(actors.get(actor_id), actors.get(other_actor_id), set);
                        }

                    }
                }
            }

        }
    }

    public void runGame(KevinBaconGame game){
        Scanner in = new Scanner(System.in);
        game.readActors();
        game.readMovies();
        game.readMovieActors();

        game.buildGraph();
        center="Kevin Bacon";
        bfsTree = GraphLibrary.BFS(game.graph, center); // tree with kev at the root

        System.out.println("Commands:\n" +
                "c : list top centers of the universe, sorted by average separation\n" +
                "d : list actors sorted by degree\n" +
                "i: find the average path length over all actors who are connected by some path to the current center\n" +
                "p <name>: find path from <name> to current center of the universe\n" +
                "s : find the number of actors with a path to the center\n" +
                "u <name>: make <name> the center of the universe\n" +
                "? : for command help\n" +
                "q: quit game");

        // GAME LOOP
        while (true){
            char input =in.nextLine().charAt(0);

            if (input=='c'){
                Map<String,Double> avgPathMap = new HashMap<String,Double>();
                for (String name : game.graph.vertices()){
                    Graph<String, HashSet<String>> bfsTemp = new AdjacencyMapGraph<String, HashSet<String>>();
                    bfsTemp = GraphLibrary.BFS(game.graph, name);
                    double avgPathLength=GraphLibrary.averageSeparation(bfsTemp, name);
                    avgPathMap.put(name,avgPathLength);
                }
                List<String> vertSort= new ArrayList<String>();
                for (String v: graph.vertices()){
                    vertSort.add(v);
                }
                vertSort.sort((String v1, String v2)-> (avgPathMap.get(v2).intValue())-(avgPathMap.get(v1).intValue()));
                for (String s: vertSort){
                    System.out.println(s);
                }
            }

            else if (input=='d'){
                List<String> vertSort= new ArrayList<String>();
                for (String v: graph.vertices()){
                    vertSort.add(v);
                }
                vertSort.sort((String v1, String v2)-> graph.inDegree(v2)-graph.inDegree(v1));
                for (String s: vertSort){
                    System.out.println(s+ " has appeared in movies with " + graph.inDegree(s) + " different actors");
                }
            }
            else if (input=='i'){
                System.out.println(GraphLibrary.averageSeparation(bfsTree, center));
            }
            else if (input=='p'){
                System.out.println("Enter name:");
                String name=in.nextLine().strip();
                System.out.println("\n");
                try{
                    List<String> path = GraphLibrary.getPath(bfsTree, name);
                    if (path.size()==1) System.out.println(name+" is "+name+"!");
                    for (int i=0;i<path.size()-1;i++){
                        System.out.println(path.get(i)+" appeared in "+bfsTree.getLabel(path.get(i),path.get(i+1)) + " with "+ path.get(i+1));
                    }
                    System.out.println(name+ "'s " + center + " number is " + (path.size()-1));
                } catch (Exception e){
                    System.out.println("Name invalid");
                }
            }
            else if (input=='s'){
                System.out.println(bfsTree.numVertices());
            }
            else if (input=='u'){
                System.out.println("Enter name:");
                String name=in.nextLine().strip();
                try{
                    bfsTree = GraphLibrary.BFS(game.graph, name); // tree with kev at the root
                    center=name;
                } catch (Exception e) {
                    System.out.println("Name invalid");
                }
            }
            else if (input=='q'){
                break;
            }
            else if (input=='?'){
                System.out.println("Commands:\n" +
                        "c : list top centers of the universe, sorted by average separation\n" +
                        "d : list actors sorted by degree\n" +
                        "i: find the average path length over all actors who are connected by some path to the current center\n" +
                        "p <name>: find path from <name> to current center of the universe\n" +
                        "s : find the number of actors with a path to the center\n" +
                        "u <name>: make <name> the center of the universe\n" +
                        "? : for command help\n" +
                        "q: quit game");
            }

            else
                {
                System.err.println("Print valid character");
            }
        }

        /*change the center of the acting universe to a valid actor
        find the shortest path to an actor from the current center of the universe
        find the number of actors who have a path (connected by some number of steps) to the current center
        find the average path length over all actors who are connected by some path to the current center*/

    }

    public void runTest1(KevinBaconGame game){
        game.actors.put("1", "Isaac");
        game.actors.put("2", "Hank");
        game.actors.put("3", "Your mom");

        HashSet<String> dartMovie = new HashSet<>();
        dartMovie.add("1");
        dartMovie.add("2");
        game.movieActors.put("100", dartMovie);

        HashSet<String> momMovie = new HashSet<>();
        momMovie.add("3");
        momMovie.add("2");

        game.movieActors.put("200", momMovie);

        game.movies.put("100", "CS10: The movie");
        game.movies.put("200", "Hank's Mom 2: Electric Bugaloo");

        game.buildGraph();

        game.bfsTree = GraphLibrary.BFS(game.graph, "Hank");
        System.out.println(game.bfsTree);
        System.out.println(GraphLibrary.getPath(game.bfsTree, "Isaac"));
        System.out.println("\n");
    }

    public void runTest2(KevinBaconGame game){
        game.readActors();
        game.readMovies();
        game.readMovieActors();

        game.buildGraph();
        System.out.println(game.movies);
        System.out.println(game.actors);
        System.out.println("\n");
        System.out.println(game.movieActors);
        System.out.println(game.graph);
        System.out.println("\n");
        Graph<String,HashSet<String>> bfs = GraphLibrary.BFS(game.graph, "Kevin Bacon"); // tree with kev at the root
        System.out.println(GraphLibrary.getPath(bfs, "Charlie"));
        System.out.println(GraphLibrary.getPath(bfs, "Nobody"));
        System.out.println(GraphLibrary.missingVertices(game.graph, bfs));
        System.out.println(bfs);
        System.out.println(GraphLibrary.averageSeparation(bfs, "Kevin Bacon"));
    }

    public static void main(String[] args) {
        // run some tests
        KevinBaconGame testGame = new KevinBaconGame("PS-4/actorsTest.txt", "PS-4/moviesTest.txt", "PS-4/movie-actorsTest.txt");
        testGame.runTest1(testGame);
        // reset the game object
        testGame = new KevinBaconGame("PS-4/actorsTest.txt", "PS-4/moviesTest.txt", "PS-4/movie-actorsTest.txt");
        testGame.runTest2(testGame);

        // run a real game
        KevinBaconGame game = new KevinBaconGame("PS-4/actors.txt", "PS-4/movies.txt", "PS-4/movie-actors.txt");
        game.runGame(game);
    }
}
