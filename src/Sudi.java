import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds a Hidden Markov Model from training data, then can tag any new sentence given
 * Stores the model as two Maps, and implements the Viterbi Algorithm
 *
 * @author Jake Markus, Dartmouth CS10 Fall
 */
public class Sudi {
    public Map<String, Map<String, Double>> emissionMap = new HashMap<>(); //maps a tag to the normalized/ln() frequencies of a word
    public Map<String, Map<String, Double>> transmissionMap = new HashMap<>(); //maps the normalized/ln() probability of tag1->tag2

    private Map<String, Map<String, Integer>> countedEmissionMap = new HashMap<>(); //tallies of each word in a tag
    private Map<String, Map<String, Integer>> countedTransmissionMap = new HashMap<>();//tallies of transitions

    /**
     * Trains Sudi by building the Markov model, stored in emissionMap/transissionMap
     * @param trainingSentancesPath where to find the training sentences in a txt file.
     * @param trainingTagsPath where to find the cooresponding tags in a txt file.
     */
    public void buildModel(String trainingSentancesPath, String trainingTagsPath) throws IOException {
        BufferedReader sentancesData = new BufferedReader(new FileReader(trainingSentancesPath)); //make the buffered readers for both
        BufferedReader tagsData = new BufferedReader(new FileReader(trainingTagsPath));

        for (String line = sentancesData.readLine(); line != null; line = sentancesData.readLine()) {
            String[] tags = tagsData.readLine().split(" ");
            String[] sentance = line.split(" "); //split each line's words/tags into arrays

            for (int i = 0; i < tags.length; i++) {
                observeWord(tags[i], sentance[i].toLowerCase()); //adds to the emissionMap
                if (i != 0)
                    observeConnection(tags[i - 1], tags[i]); //adds to the transmissionMap
                else
                    observeConnection("#", tags[i]); //only the first word connects to start (#)
            }
        }

        emissionMap = scorify(countedEmissionMap); //normalize/ln() the maps and store them
        transmissionMap = scorify(countedTransmissionMap);

        sentancesData.close(); tagsData.close(); //close the files

    }

    /**
     * updates the countedEmissionMap for a word/tag pair
     * @param tag the word's PoS tag (ADJ, N, etc.)
     * @param word the actual word (cat, ran, etc.)
     */
    private void observeWord(String tag, String word) {
        if (!countedEmissionMap.containsKey(tag))
            countedEmissionMap.put(tag, new HashMap<>()); //adds the tag if missing

        Map<String, Integer> wordCount = countedEmissionMap.get(tag);

        if (!wordCount.containsKey(word))
            wordCount.put(word, 1); //adds the word if missing
        else
            wordCount.put(word, wordCount.get(word) + 1); //otherwise incriments the existing count

    }

    /**
     * updates the countedTransmissionMap for two sequential tags
     * @param tag1 the first PoS
     * @param tag2 the second PoS
     */
    private void observeConnection(String tag1, String tag2) {
        if (!countedTransmissionMap.containsKey(tag1)) {
            countedTransmissionMap.put(tag1, new HashMap<>()); //adds this starting tag if missing
        }

        Map<String, Integer> connections = countedTransmissionMap.get(tag1);

        if (!connections.containsKey(tag2))
            connections.put(tag2, 1); //adds this connection if missing
        else
            connections.put(tag2, connections.get(tag2) + 1); //otherwise incriments it
    }
    /**
     * normalizes and takes the ln() of all numbers in a Map of String -> (Map of String -> Double)
     * @param items the map to normalize and ln(), a Map of String -> (Map of String -> Double)
     */
    private static Map<String, Map<String, Double>> scorify(Map<String, Map<String, Integer>> items) {
        Map<String, Map<String, Double>> output = new HashMap<>();

        for (String key : items.keySet()) { //runs thru every key

            int total = 0; //calculate the number to normalize by
            for (int freq : items.get(key).values())
                total += freq;

            Map<String, Double> result = new HashMap<>();
            //normalize/ln() every number in the key map
            for (String identifier : items.get(key).keySet()) {
                result.put(identifier, Math.log((double) items.get(key).get(identifier) / total));
            }

            output.put(key, result);
        }
        return output;
    }

    /**
     * Implements the Viterbi Algorithm to find the most likely path in our Hidden Markov Model
     * @param sentance the sentance (with each word/mark seperated by a space) to analyze
     * @return ArrayList of Strings, the tag for each word in the sentance
     */
    public ArrayList<String> viterbiSolve(String sentance) {
        String[] words = sentance.toLowerCase().split(" ");
        ArrayList<Map<String, String>> table = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            table.add(new HashMap<>());
        }

        ArrayList<String> currStates = new ArrayList<>(); //all the possible current states. Only start at the beginning
        currStates.add("#");
        Map<String, Double> currScores = new HashMap<>();
        currScores.put("#", 0.0d); //we are 100% sure this is the start

        for (int i = 0; i < words.length; i++) { //run through every word but the last one
            ArrayList<String> nextStates = new ArrayList<>(); //reset the information about the next state, we are at a new word
            Map<String, Double> nextScores = new HashMap<>();

            for (String currState : currStates) {

                for (String nextState : transmissionMap.get(currState).keySet()) {
                    if(!nextStates.contains(nextState))
                        nextStates.add(nextState); //adds all the possible states this tag could lead into

                    //This ternary punishes an unseen word with a negative value, or finds a seen words frequency in a tag
                    double nextEmmisionState = emissionMap.get(nextState).get(words[i]) == null ? -20 : emissionMap.get(nextState).get(words[i]);

                    //calculates the next score: curr + transit + obs
                    double nextScore = currScores.get(currState) + transmissionMap.get(currState).get(nextState) + nextEmmisionState;

                    if (!nextScores.containsKey(nextState) || nextScore > nextScores.get(nextState)) {
                        nextScores.put(nextState, nextScore); //only stores the score if best available
                        table.get(i).put(nextState, currState);
                    }
                }
            }
            currStates = nextStates; //at the next observation our future states become present
            currScores = nextScores;
        }

        String bestTag = "";
        double bestScore = -1000.0;

        //finds our best guess for the final observation
        for(String tag: currScores.keySet()) {
            if(currScores.get(tag) > bestScore) {
                bestTag = tag;
                bestScore = currScores.get(tag);
            }
        }

        //runs a backtrace on the final table, returns the result
        return backtrace(table, bestTag);
    }

    /**
     * Finishes the Viterbi algorithm. If we believe we are at tag X, what is the best tag Y in the previous layer that most likely led us here? Repeat.
     * @param table The viterbi table,
     * @return ArrayList of Strings, the tag for each word in the sentance
     */
    private ArrayList<String> backtrace(ArrayList<Map<String, String>> table, String lastTag)
    {
        ArrayList<String> solution = new ArrayList<>(); //keeps track of the result

        for(int i = 0; i < table.size()-1; i++) { //initializes everything to 0. Just easier
            solution.add("");
        }

        solution.add(lastTag); //appends the last tag, which we know

        for(int i = table.size()-1; i != 0; i --) {
            solution.set(i-1, table.get(i).get(solution.get(i))); //starting with our last tag, work backwards, inserting the result
        }
        return solution;
    }
}