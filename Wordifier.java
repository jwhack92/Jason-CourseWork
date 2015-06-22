/*
 * Wordifier.java
 *
 * Implements methods for iteratively learning words from a
 * character-segmented text file, and then evaluating how good they are
 *
 * April 2014
 *
 * Authors:   Jason Hack
 *            Omar Juma
 */

import java.util.LinkedList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Math;

public class Wordifier {

   static private int totalBigramCount = 0;       // keeps track of the total numbers of bigrams
   static private LinkedList<String> initialData; // keeps track of the set of unigrams
  

   // Helper method for reading input from files
   private static LinkedList<String> loaderUtil(LinkedList<String> list, String path) {
      BufferedReader buffer;
      try{
         String input;
         buffer = new BufferedReader(new FileReader(path));

         while ((input = buffer.readLine()) != null){
            String[] parts = input.split("\\s");
            list.addAll(Arrays.asList(parts));
         }
      }
      catch(IOException e) {
         System.out.println(e.getMessage());
         System.out.println("Error: Unable to open file " + path);
         System.exit(1);
      }
		return list;
	}

    // loadSentences
    // Preconditions:
    //    - textFilename is the name of a plaintext input file
    // Postconditions:
    //  - A LinkedList<String> object is returned that contains
    //    all of the words in the input file, in order
   public static LinkedList<String> loadSentences( String textFilename ) {
      LinkedList<String> data = new LinkedList<String>();
      data = loaderUtil(data, textFilename);
      return data;
	}

    // findNewWords
    // Preconditions:
    //    - bigramCounts maps bigrams* to the number of times the bigram appears in the data
    //    - scores maps bigrams to its bigram product score
    //    - countThreshold is a threshold on the counts
    //    - probabilityThreshold is a threshold on the bigram product score
    // Postconditions:
    //    - A HashSet is created and returned, containing all bigrams that meet the following criteria
    //        1) the bigram is a key in bigramCounts
    //        2) the count of the bigram is >= countThreshold
    //        3) the score of the bigram is >= probabilityThreshold
	public static HashSet<String> findNewWords( HashMap<String,Integer> bigramCounts, HashMap<String,Double> scores, int countThreshold, double probabilityThreshold ) {
		HashSet<String> answer = new HashSet<String>();
      for (Entry<String,Integer> entry : bigramCounts.entrySet()) {
         if( entry.getValue() >= countThreshold && scores.get(entry.getKey()) >= probabilityThreshold) {
            String key = entry.getKey().replaceAll("\\s+","");
            answer.add(key);
         }
      }
      return answer;
	}

    // resegment
    // Preconditions:
    //    - previousData is the LinkedList representation of the data
    //    - newWords is the HashSet containing the new words (after merging)
    // Postconditions:
    //    - A new LinkedList is returned, which contains the same information as
    //      previousData, but any pairs of words in the newWords set have been merged
    //      to a single entry (merge from left to right)
    //
    //      For example, if the previous linked list contained the following items:
    //         A B C D E F G H I
    //      and the newWords contained the entries "B C" and "G H", then the returned list would have
    //         A BC D E F GH I
	public static LinkedList<String> resegment( LinkedList<String> previousData, HashSet<String> newWords ) {
      String current = null;
      LinkedList<String> newData = new LinkedList<String>();
      String token = "";
      while (previousData.peekFirst() != null) {
         token = previousData.removeFirst();
         if (current != null){
             String combined = current + token;
             if (newWords.contains(combined)) {
                newData.addLast(combined);
                if (previousData.peekFirst() != null){
                   token = previousData.removeFirst();
                }
             }
             else{
                newData.addLast(current);
             }
        }
         current = token;
      }
      newData.addLast(token);
      return newData;
	}

    // computeCounts
    // Preconditions:
    //    - data is the LinkedList representation of the data
    //    - bigramCounts is an empty HashMap that has already been created
    // Postconditions:
    //    - bigramCounts maps each bigram appearing in the data to the number of times it appears
	public static void computeCounts(LinkedList<String> data, HashMap<String,Integer> bigramCounts ) {
      String var;
      String current = null;
      for (String token : data) {
         if(current != null) {
            var = current + " " + token;
            incrementHashMap(bigramCounts, var, 1);
            totalBigramCount++;
         }
         current = token;
      }
      initialData = data;
		return;
	}

    // convertCountsToProbabilities
    // Preconditions:
    //    - bigramCounts maps each bigram appearing in the data to the number of times it appears
    //    - bigramProbs is an empty HashMap that has already been created
    //    - unigramProbs is an empty HashMap that has already been created
    // Postconditions:
    //    - bigramProbs maps bigrams to their joint probability
    //        (where the joint probability of a bigram is the # times it appears over the total # bigrams)
    //    - unigramProbs maps words to their "marginal probability"
    //        (i.e. the frequency of each word over the total # bigrams)
	public static void convertCountsToProbabilities(HashMap<String,Integer> bigramCounts, HashMap<String,Double> bigramProbs, HashMap<String,Double> unigramProbs ) {
		Set<String> bigramKeySet = new HashSet<String>();
      bigramKeySet = bigramCounts.keySet();
      double probability;
      for(String s : bigramKeySet) {
         probability = bigramCounts.get(s);
         bigramProbs.put(s, probability / totalBigramCount);
      }
      for(String s : initialData) {
         if(!unigramProbs.containsKey(s)) {
            unigramProbs.put(s, 1.0 / totalBigramCount);
         }
         else{
            double temp2 = (unigramProbs.get(s) * totalBigramCount) + 1.0;
            unigramProbs.put(s, temp2 / totalBigramCount);
         }
      }
      return;
	}

    // getScores
    // Preconditions:
    //    - bigramProbs maps bigrams to to their joint probability
    //    - unigramProbs maps words to their "marginal probability"
    // Postconditions:
    //    - A new HashMap is created and returned that maps bigrams to
    //      their "bigram product scores", defined to be P(w1|w2)P(w2|w1)
    //      The above product is equal to P(w1,w2)/sqrt(P(w1)*P(w2)), which
    //      is the form you will want to use
	public static HashMap<String,Double> getScores( HashMap<String,Double> bigramProbs, HashMap<String,Double> unigramProbs ) {
      HashMap<String, Double> scores = new HashMap<String, Double>();
      Set<String> bigramKeySet = new HashSet<String>();
      bigramKeySet = bigramProbs.keySet();
      String[] splited;;

      for (String token : bigramKeySet) {
         splited = token.split("\\s");
         double numerator = bigramProbs.get(token);
         double unigram1 = unigramProbs.get(splited[0]);
         double unigram2 = unigramProbs.get(splited[1]);
         double denominator = Math.sqrt(unigram1 * unigram2);
         double productScore = numerator / denominator;
         scores.put(token, productScore);
      }
      return scores;
	}

    // getVocabulary
    // Preconditions:
    //    - data is a LinkedList representation of the data
    // Postconditions:
    //    - A new HashMap is created and returned that maps words
    //      to the number of times they appear in the data
	public static HashMap<String,Integer> getVocabulary( LinkedList<String> data ) {
      HashMap<String,Integer> vocabulary = new HashMap<String, Integer>();
      for (String token: data){
         incrementHashMap(vocabulary, token, 1);
      }
		return vocabulary;
	}

    // loadDictionary
    // Preconditions:
    //    - dictionaryFilename is the name of a dictionary file
    //      the dictionary has one word per line
    // Postconditions:
    //    - A new HashSet is created and returned that contains
    //      all unique words appearing in the dictionary
	public static HashSet<String> loadDictionary( String dictionaryFilename ) {
		HashSet<String> dictionaryWords = new HashSet<String>();
      try{
         String input;
         BufferedReader buffer = new BufferedReader(new FileReader(dictionaryFilename));

         while ((input = buffer.readLine()) != null){
            String[] parts = input.split("\\s");
            dictionaryWords.addAll(Arrays.asList(parts));
         }
      }
      catch(IOException e) {
         System.out.println(e.getMessage());
         System.out.println("Error: Unable to open file " + dictionaryFilename);
         System.exit(1);
      }
		return dictionaryWords;
   }

    // incrementHashMap
    // Preconditions:
    //  - map is a non-null HashMap
    //  - key is a key that may or may not be in map
    //  - amount is the amount that you would like to increment key's value by
    // Postconditions:
    //  - If key was already in map, map.get(key) returns amount more than it did before
    //  - If key was not in map, map.get(key) returns amount
	private static void incrementHashMap(HashMap<String,Integer> map,String key,int amount) {
		if( map.containsKey(key) ) {
			map.put(key,map.get(key)+amount);
		} else {
			map.put(key,amount);
		}
		return;
	}

    // printNumWordsDiscovered
    // Preconditions:
    //    - vocab maps words to the number of times they appear in the data
    //    - dictionary contains the words in the dictionary
    // Postconditions:
    //    - Prints each word in vocab that is also in dictionary, in sorted order (alphabetical, ascending)
    //        Also prints the counts for how many times each such word occurs
    //    - Prints the number of unique words in vocab that are also in dictionary
    //    - Prints the total of words in vocab (weighted by their count) that are also in dictionary
	public static void printNumWordsDiscovered( HashMap<String,Integer> vocab, HashSet<String> dictionary ) {
		TreeMap<String, Integer> words = new TreeMap<String, Integer>();
      words.putAll(vocab);
      int uniqueWords = 0;
      int totalWords = 0;
      for (Entry<String,Integer> entry : words.entrySet()) {
         if (dictionary.contains(entry.getKey())){
            uniqueWords++;
            totalWords += (entry.getValue());
            System.out.println("Discovered " + entry.getKey() + " (count " + entry.getValue() + ")");
         }
      }
      System.out.println("Number of unique words discovered: " + uniqueWords);
      System.out.println("Total number words discovered: " + totalWords);
      return;
	}
}
