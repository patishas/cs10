import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * A Hidden Markov Model (HMM) that parses a tagged corpus to infer the parts of speech of an input string.
 * Made for COSC 10, Fall 2020
 * @author Shashank Patil (shashank.s.patil.22@dartmouth.edu)
 * @author Isaac Feldman (isaac.c.feldman.23@dartmouth.edu)
 */
public class HiddenMarkovModel {
    private HashMap<String, HashMap<String, Double>> stateModel; // { CURRENT STATE : { NEXT STATE : LN(PROB) } }
    private HashMap<String, HashMap<String, Double>> observationModel; // { CURRENT STATE : { OBSERVATION : LN(PROB) }
    private BufferedReader tagReader, sentReader;

    private final double unkWeight = -100.0;

    public HiddenMarkovModel(){
          stateModel = new HashMap<>();
          observationModel = new HashMap<>();
    }

    /**
     * Load in the tags and tokens, populate the model with observations from the training data. Then, process the
     * counts into probabilities
     * @param sentPath the string path the the training sentences file
     * @param tagPath the string path to the training tags file
     */
    public void train(String sentPath, String tagPath){
        stateModel = new HashMap<>();
        observationModel = new HashMap<>();
        try {
            tagReader = new BufferedReader(new FileReader(tagPath));
            sentReader = new BufferedReader(new FileReader(sentPath));
        } catch (FileNotFoundException e){
            System.err.println("File not found." + e);
            return;
        }

        // read in the data
        try {
            String tagLine, sentence;
            while ((tagLine = tagReader.readLine()) != null && (sentence = sentReader.readLine()) != null) {
                sentence = sentence.toLowerCase();
                // add start tokens to the sentences for modeling purposes
                String[] tagsTemp = tagLine.split(" ");
                String[] tags = new String[tagsTemp.length+1];
                String[] tokensTemp = sentence.split(" ");
                String[] tokens = new String[tokensTemp.length+1];
                tokens[0]="#";
                System.arraycopy(tokensTemp, 0, tokens,1,tokensTemp.length);
                tags[0]="#";
                System.arraycopy(tagsTemp, 0, tags,1,tagsTemp.length);

                // read the tags into the model
                for (int i = 0; i < tags.length - 1; i++) {
                    if (!stateModel.containsKey(tags[i])) stateModel.put(tags[i], new HashMap<String, Double>());
                    if (!stateModel.get(tags[i]).containsKey(tags[i + 1]))
                        stateModel.get(tags[i]).put(tags[i + 1], 0.0);
                    stateModel.get(tags[i]).put(tags[i + 1], stateModel.get(tags[i]).get(tags[i + 1]) + 1.0);
                }

                // read the observations into the model
                for (int i = 0; i < tags.length; i++) {
                    // if the model does not currently have the part of speech tag, add it and an empty entry
                    if (!observationModel.containsKey(tags[i]))
                        observationModel.put(tags[i], new HashMap<String, Double>());
                    // if the key's value has an empty entry, fill it upon the first observation of the word
                    if (!observationModel.get(tags[i]).containsKey(tokens[i]))
                        observationModel.get(tags[i]).put(tokens[i], 0.0);
                    // if the entry for an observation under a POS does exist, increment the entry
                    observationModel.get(tags[i]).put(tokens[i], observationModel.get(tags[i]).get(tokens[i]) + 1.0);
                }
            }

            //Turn counts into natural log probabilities
            for (String POS : stateModel.keySet()) {
                int numObservations = observationModel.get(POS).size();
                for (String observation : observationModel.get(POS).keySet()) {
                    // calculate the log probability of an observation given the POS tag from the count for that token
                    double val = Math.log(observationModel.get(POS).get(observation) / (double) numObservations);
                    observationModel.get(POS).put(observation, val);
                }

                for (String transition : stateModel.get(POS).keySet()) {
                    // calculate the log probability of a state transition given the POS tag from the count for that state
                    stateModel.get(POS).put(transition, Math.log(stateModel.get(POS).get(transition) / (double) numObservations));
                }


            }
        } catch (IOException e){
            System.err.println("IOException Occurred"+e);
        }
    }

    /**
     * Use the trained model to infer the parts of speech tags associated with an input sentence.
     * Uses an implementation of the Viterbi algorithm.
     * @param inputSent Sentence to be tagged
     * @return a list of tags indexed to the tokens in the sentence
     */
    public List<String> inferTags(String inputSent){
        String[] tokens=inputSent.split(" ");

        // Set of current states
        Set<String> currentStates = new HashSet<>();
        // Maps from likely next state to current state indexed to each observation.
        List<Map<String,String>> nexttoCurr = new ArrayList<Map<String,String>>();
        // Maps to store the scores from the current and previous iterations
        HashMap<String, Double> currentScores = new HashMap<String, Double>();
        HashMap<String, Double> previousScores = new HashMap<>();


        // Prepare the start state
        currentStates.add("#");
        previousScores.put("#", 0.0);


        for (int i=0; i<tokens.length; i++){
            // reset the scores
            currentScores = new HashMap<String, Double>();
            nexttoCurr.add(new HashMap<String,String>());

            for (String currentState : currentStates) { // select a state from the current states
                if (stateModel.get(currentState)==null) continue; // null check

                for (String nextState : stateModel.get(currentState).keySet()) { // select a possible next state from the current
                    // Gets observation probability if available, otherwise returns unknown penalty
                    double obsWeight = observationModel.get(nextState).getOrDefault(tokens[i], unkWeight);
                    //Calculate the score: currScore + transitionScore + obsScore
                    double score = previousScores.get(currentState) + stateModel.get(currentState).get(nextState) + obsWeight;

                    nexttoCurr.get(i).putIfAbsent(nextState,currentState);

                    // save the score if it is:
                    // (1) the first score associated with a given next state
                    // OR
                    // (2) greater than the current score associated in the map for the given next state
                    if (currentScores.putIfAbsent(nextState, score)!=null && currentScores.get(nextState)<score){
                        currentScores.put(nextState, score);
                        // make sure the transition map matches
                        nexttoCurr.get(i).put(nextState,currentState);
                    }
                }

            }
            // save the current scores to access next iteration
            previousScores=currentScores;
            // update the current states
            currentStates=currentScores.keySet();
        }


        // find the key associated with the max value in the map, from the last observation
        double maxValue = Double.NEGATIVE_INFINITY;
        String maxKey = "";
        for (String s : currentScores.keySet()) {
            if (currentScores.get(s) > maxValue) {
                maxValue = currentScores.get(s);
                maxKey = s;
            }
        }
        // assemble the backtrace list
        String location = maxKey;
        List<String> backTrace = new LinkedList<String>();
        for (int i=tokens.length-1; i>-1; i--){
            backTrace.add(0,location);
            location=nexttoCurr.get(i).get(location);
        }
        return backTrace;
    }

    /**
     * Set up a simple test HMM. This model is identical to the one reviewed in recitation.
     */
    public void testModel(){
        HashMap<String, Double> temp = new HashMap<>();
        // Hard code a test model states and transitions
        temp.put("NP", 3.0);
        temp.put("N", 7.0);
        stateModel.put("#", temp);
        temp = new HashMap<>();
        temp.put("CNJ", 2.0);
        temp.put("V", 8.0);
        stateModel.put("NP", temp);
        temp = new HashMap<>();
        temp.put("NP", 2.0);
        temp.put("N", 4.0);
        temp.put("V", 4.0);
        stateModel.put("CNJ", temp);
        temp = new HashMap<>();
        temp.put("CNJ", 2.0);
        temp.put("V", 8.0);
        stateModel.put("N", temp);
        temp = new HashMap<>();
        temp.put("N",4.0);
        temp.put("NP",4.0);
        temp.put("CNJ",2.0);
        stateModel.put("V", temp);

        // Hard code model observations
        temp = new HashMap<>();
        temp.put("chase", 10.0);
        observationModel.put("NP", temp);
        temp = new HashMap<>();
        temp.put("cat", 4.0);
        temp.put("dog", 4.0);
        temp.put("watch", 2.0);
        observationModel.put("N", temp);
        temp = new HashMap<>();
        temp.put("and", 10.0);
        observationModel.put("CNJ", temp);
        temp = new HashMap<>();
        temp.put("get",1.0);
        temp.put("chase",3.0);
        temp.put("watch",6.0);
        observationModel.put("V",temp);

        System.out.println(stateModel);
        System.out.println(observationModel);
    }

    /**
     * Another simple test HMM. Like testModel() but with different probabilities.
     */
    public void testModel2(){
        HashMap<String, Double> temp = new HashMap<>();
        // Hard code a test model states and transitions
        temp.put("NP", 1.0);
        temp.put("N", 1.0);
        stateModel.put("#", temp);
        temp = new HashMap<>();
        temp.put("CNJ", 2.0);
        temp.put("V", 2.0);
        stateModel.put("NP", temp);
        temp = new HashMap<>();
        temp.put("NP", 3.0);
        temp.put("N", 3.0);
        temp.put("V", 3.0);
        stateModel.put("CNJ", temp);
        temp = new HashMap<>();
        temp.put("CNJ", 4.0);
        temp.put("V", 4.0);
        stateModel.put("N", temp);
        temp = new HashMap<>();
        temp.put("N",5.0);
        temp.put("NP",5.0);
        temp.put("CNJ",5.0);
        stateModel.put("V", temp);

        // Hard code model observations
        temp = new HashMap<>();
        temp.put("chase", 1.0);
        observationModel.put("NP", temp);
        temp = new HashMap<>();
        temp.put("cat", 1.0);
        temp.put("dog", 1.0);
        temp.put("watch", 1.0);
        observationModel.put("N", temp);
        temp = new HashMap<>();
        temp.put("and", 1.0);
        observationModel.put("CNJ", temp);
        temp = new HashMap<>();
        temp.put("get",1.0);
        temp.put("chase",1.0);
        temp.put("watch",1.0);
        observationModel.put("V",temp);
    }

    /**
     * Test the tagging of sentences entered into the console
     */
    public void consoleEval(){
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.println("Enter a sentence to be tagged:");
            String sent = in.nextLine();
            System.out.println(inferTags(sent.toLowerCase()));
        }
    }

    /**
     * Tag and evaluate tagging performance on a pair of input files
     * @param sentPath path to a file containing sentences
     * @param tagPath path to a file containing those sentences' respective part of speech tags
     */
    public void fileEval(String sentPath, String tagPath){
        try {
            tagReader = new BufferedReader(new FileReader(tagPath));
            sentReader = new BufferedReader(new FileReader(sentPath));
        } catch (FileNotFoundException e){
            System.err.println("File not found." + e);
            return;
        }

        int correctTags = 0 , incorrectTags = 0, totalTags = 0;
        try {
            String tagLine, sentence;
            while ((tagLine = tagReader.readLine()) != null && (sentence = sentReader.readLine()) != null) {
                sentence = sentence.toLowerCase();
                String[] tags = tagLine.split(" ");
                for (int i =0; i < tags.length; i++){
                    List<String> guess = inferTags(sentence);
                    if (tags[i].equals(guess.get(i))) correctTags++;
                    else{
                        incorrectTags++;
                        // The lines below allow for a closer look into the incorrect tags
//                        System.out.println(guess);
//                        System.out.println(Arrays.asList(tags));
//                        System.out.println(sentence);
//                        System.out.println("\n");
                    }
                    totalTags++;
                }
            }
        }catch(IOException e){
            System.out.println("Error occurred" + e);
        }
        System.out.println("Correct Tags: "+correctTags+" Incorrect Tags: "+incorrectTags+" Total: "+totalTags);
    }


    public static void main(String[] args) {
        HiddenMarkovModel h = new HiddenMarkovModel();
        //Test 1
        h.testModel();
        System.out.println(h.inferTags("chase watch dog chase watch"));

        // Test 2
        h.testModel2();
        System.out.println(h.inferTags("chase watch dog chase watch"));

        // Test on simple files
        h.train("HiddenMarkovModel/simple-train-sentences.txt", "HiddenMarkovModel/simple-train-tags.txt");
        h.fileEval("HiddenMarkovModel/simple-test-sentences.txt","HiddenMarkovModel/simple-test-tags.txt");

        // Test on Brown files
        h.train("HiddenMarkovModel/brown-train-sentences.txt", "HiddenMarkovModel/brown-train-tags.txt");
        h.fileEval("HiddenMarkovModel/brown-test-sentences.txt","HiddenMarkovModel/brown-test-tags.txt");

        // Input your own sentences to test their tagging against the Brown model
        h.consoleEval();
    }
}
