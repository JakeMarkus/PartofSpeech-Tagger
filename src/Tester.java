import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Tests our Hidden Markov Model "AI" (called Sudi) for tagging PoS in unseen sentances
 *
 * @author Jake Markus, Dartmouth CS10 Fall
 */
public class Tester
{
    public static void main(String[] args) throws IOException
    {
        Sudi assistant = new Sudi(); //train the model on the brown training data
        assistant.buildModel("texts/brown-train-sentences.txt", "texts/brown-train-tags.txt");

        //compile the testing data
        Map<String, String[]> testData = buildTestData("texts/brown-test-sentences.txt", "texts/brown-test-tags.txt");

//        System.out.println(assistant.viterbiSolve("there is a house on the hill , which is very big , and I am very happy"));
        int numberRight = 0; int numberWrong = 0; //keep track of our results

        for(String sentance : testData.keySet()) //for each sentance
        {
            ArrayList<String> sudiSolution = assistant.viterbiSolve(sentance); //solve it

            boolean right = true;
            //compare every key
            for(int i = 0; i < sudiSolution.size(); i ++)
            {
                if(sudiSolution.get(i).equals(testData.get(sentance)[i]))
                {
                    numberRight ++;
                }
                else
                {
                    numberWrong ++;
                    right = false;
//                    System.out.println("Sudi got a word wrong in: " + sentance);
//                    System.out.println(sentance.split(" ")[i] + " should be: " + testData.get(sentance)[i] + " but Sudi said: " + sudiSolution.get(i));
                }
            }
            if(right)
            {
               // System.out.println("Sudi got right: " + sentance);
            }
        }
        //print results
        System.out.println("BROWN DATA TEST: ");
        System.out.println("NUMBER RIGHT: " + numberRight);
        System.out.println("NUMBER WRONG: " + numberWrong);
        System.out.println("-----------\n");

        //LET THE USER ENTER SENTANCES
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your sentance (q to quit): ");

        //keep on petitioning the user until they enter q
        for(String input = sc.nextLine(); !input.equals("q"); input = sc.nextLine()) {
            System.out.println("Sudi thinks this is the structure of your sentance:");
            System.out.println(assistant.viterbiSolve(input)); //solve it
            System.out.println("Enter your sentance (q to quit): ");
        }
    }

    /**
     * Implements the Viterbi Algorithm to find the most likely path in our Hidden Markov Model
     * @param testSentancesPath where to find the tester sentences in a txt file.
     * @param testTagsPath where to find the cooresponding tags in a txt file.
     * @return A map of each sentance to its solution
     */
    public static Map<String, String[]> buildTestData(String testSentancesPath, String testTagsPath) throws IOException {

        Map<String, String[]> testData = new HashMap<>();

        BufferedReader sentancesData = new BufferedReader(new FileReader(testSentancesPath));
        BufferedReader tagsData = new BufferedReader(new FileReader(testTagsPath));

        for (String line = sentancesData.readLine(); line != null; line = sentancesData.readLine()) {
            String[] tags = tagsData.readLine().split(" ");
            String sentance = line;

            testData.put(sentance, tags); //goes through every line and puts in the data
        }
        sentancesData.close(); tagsData.close(); //close the files cause we good like that
        return testData;

    }
}
