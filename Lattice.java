/*
* Lattice.java
*
* Defines a new "Lattice" type, which is a directed acyclic graph that
* compactly represents a very large space of speech recognition hypotheses
*
*  Authors  
*
* Jason Hack
* Connor Richardson
*
*/

import java.util.*;
import java.io.*;
import java.lang.*;

public class Lattice {
   private String utteranceID;       // A unique ID for the sentence
   private int startIdx, endIdx;     // Indices of the special start and end tokens
   private int numNodes, numEdges;   // The number of nodes and edges, respectively
   private Edge[][] adjMatrix;       // Adjacency matrix representing the lattice
   //   Two dimensional array of Edge objects
   //   adjMatrix[i][j] == null means no edge (i,j)
   private double[] nodeTimes;       // Stores the timestamp for each node
   private int nonSilenceWords;
   
   // Constructor
   
   // Lattice
   // Preconditions:
   //     - latticeFilename contains the path of a valid lattice file
   // Post-conditions
   //     - Field id is set to the lattice's ID
   //     - Field startIdx contains the node number for the start node
   //     - Field endIdx contains the node number for the end node
   //     - Field numNodes contains the number of nodes in the lattice
   //     - Field numEdges contains the number of edges in the lattice
   //     - Field adjMatrix encodes the edges in the lattice:
   //        If an edge exists from node i to node j, adjMatrix[i][j] contains
   //        the address of an Edge object, which itself contains
   //           1) The edge's label (word)
   //           2) The edge's acoustic model score (amScore)
   //           3) The edge's language model score (lmScore)
   //        If no edge exists from node i to node j, adjMatrix[i][j] == null
   public Lattice(String latticeFilename) {
      
      Scanner input = null;
      try {
         input = new Scanner(new File(latticeFilename));
      } catch( FileNotFoundException e ) {
         System.err.println("Error: Unable to open file " + latticeFilename);
         System.exit(1);
      }
      try {        
         if (input.next().equals("id"))
            utteranceID = input.next();
         if (input.next().equals("start"))
            startIdx = Integer.parseInt(input.next());
         if (input.next().equals("end"))
            endIdx = Integer.parseInt(input.next());
         if (input.next().equals("numNodes"))
            numNodes = Integer.parseInt(input.next());
         if (input.next().equals("numEdges"))
            numEdges = Integer.parseInt(input.next());
      } catch (NoSuchElementException e){
         System.err.println("Error: Not able to parse file " + latticeFilename);
         System.exit(1);
      }
      nodeTimes = new double[numNodes];
       nonSilenceWords = 0;
      adjMatrix = new Edge[numNodes][numNodes];
      while (input.hasNext()) {
         String type = input.next();
         if (type.equals("node")) {
            int aNode = Integer.parseInt(input.next());
            nodeTimes[aNode]= Double.parseDouble(input.next());
         }
         if (type.equals("edge")) {
            int node1 = Integer.parseInt(input.next());
            int node2 = Integer.parseInt(input.next());
            String label = input.next();
            if(!label.equals("-silence-"))
                  nonSilenceWords++;
            int amScore = Integer.parseInt(input.next());
            int lmScore = Integer.parseInt(input.next());
            adjMatrix[node1][node2] = new Edge(label, amScore, lmScore);
         }
      }
   }
   
   // Accessors
   
   // getUtteranceID
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the utterance ID
   public String getUtteranceID() {
      return utteranceID;
   }
   
   // getNumNodes
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the number of nodes in the lattice
   public int getNumNodes() {
      return numNodes;
   }
   
   // getNumEdges
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the number of edges in the lattice
   public int getNumEdges() {
      return numEdges;
   }
   
   // toString
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Constructs and returns a string that is identical to the contents
   //      of the lattice file used in the constructor
   // Notes:
   //    - Do not store the input string verbatim: reconstruct it on they fly
   //      from the class's fields
   //    - toString simply returns a string, it should not print anything itself
   public String toString() {
      StringBuilder answer = new StringBuilder(numNodes*2);
       answer.append("id " + utteranceID + "\n" + "start " + startIdx + "\n" + "end " + endIdx + "\n"
      + "numNodes " + numNodes + "\n" + "numEdges " + numEdges + "\n");
      
      for (int i=0; i < numNodes; i++) {
         String nodeTime = String.format("%.2f", nodeTimes[i]);
         answer.append("node " + i + " " + nodeTime + "\n");
      }
      
      for (int i=0; i < numNodes; i++){
         for (int j=0; j < numNodes; j++){
            Edge anEdge = adjMatrix[i][j];
            if (anEdge != null){           
               answer.append("edge " + i + " " + j + " " + anEdge.getLabel() + " " + anEdge.getAmScore() + " " + anEdge.getLmScore() + "\n");
            }
         }
      }
      return answer.toString();
   }
   
   // decode
   // Pre-conditions:
   //    - lmScale specifies how much lmScore should be weighted
   //        the overall weight for an edge is amScore + lmScale * lmScore
   // Post-conditions:
   //    - A new Hypothesis object is returned that contains the shortest path
   //      (aka most probable path) from the startIdx to the endIdx
   public Hypothesis decode(double lmScale) {
      Hypothesis answer = new Hypothesis();
      double[] cost = new double[numNodes];
      int[] parent = new int[numNodes];
      LinkedList<Integer> results = new LinkedList<Integer>();
      
      for (int i=0; i<numNodes; i++) {
         cost[i] = Double.POSITIVE_INFINITY;
      }
      cost[startIdx] = 0;
      int[] topSorted = this.topologicalSort();
      
      for (int N: topSorted) {
         for (int I: topSorted) {
            if( adjMatrix[I][N] != null) {   
               Edge anEdge = adjMatrix[I][N];      
               double score = anEdge.getAmScore() + (anEdge.getLmScore() * lmScale);
               if ( (score + cost[I]) < cost[N]) {
                  cost[N] = score + cost[I];
                  parent[N] = I;
               }        
            }
         }
      }
      int node = endIdx;
      while (node != startIdx) {
         results.addFirst(node);
         node = parent[node]; 
      } 
      results.addFirst(startIdx);  
      int previous = -1;
      for (int i: results) {    
         int current = i;
         if (previous != -1) {
            Edge anEdge = adjMatrix[previous][current];
            String word = anEdge.getLabel();
            double score = anEdge.getAmScore() + (anEdge.getLmScore() * lmScale);
            answer.addWord(word, score);
         }
         previous = current;
      }
      return answer;
   }
    
   
   // topologicalSort
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - A new int[] is returned with a topological sort of the nodes
   //      For example, the 0'th element of the returned array has no
   //      incoming edges.  More generally, the node in the i'th element
   //      has no incoming edges from nodes in the i+1'th or later elements
   public int[] topologicalSort() {
      int[] results = new int[numNodes];
      int[] inDegree = inDegreeMaker();
      Queue<Integer> s = new LinkedList<Integer>();
      queueMaker(inDegree, s);
      int count = 0;
      while(!s.isEmpty()) {
         int n = s.remove();
         results[count] = n;
         for(int i = 0; i < numNodes; i++) {
            if(adjMatrix[n][i] != null) {
               inDegree[i]--;
               if(inDegree[i] == 0)
                  s.add(i);
            }
         }
         count++;
      }
      return results;
   }
   
   private Queue<Integer> queueMaker(int[] inDegree, Queue<Integer> s) {
      for(int i = 0; i < inDegree.length; i++) {
         if(inDegree[i] == 0) 
            s.add(i); 
      }
      return s;
   }
         
   private int[] inDegreeMaker() {
      int[] answer = new int[numNodes];
      for(int i = 0; i < numNodes; i++) {
         for(int j = 0; j < numNodes; j++) {
            if(adjMatrix[i][j] != null) {
               answer[j]++;
            }
         }
      }
      return answer;
   }
                  
   // countAllPaths
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the total number of distinct paths from startIdx to endIdx
   public java.math.BigInteger countAllPaths() {
     java.math.BigInteger[] numParents = new java.math.BigInteger[numNodes]; 
      int[] topSorted = this.topologicalSort();
      for (int i = 0; i < numNodes; i++) 
         numParents[i] = java.math.BigInteger.ZERO;
      numParents[startIdx] = java.math.BigInteger.ONE;
      for (int N : topSorted) {
         for (int I : topSorted) {
            if( adjMatrix[N][I] != null) {
               numParents[I] = numParents[I].add(numParents[N]);                             
            }
         }
      }
      return numParents[endIdx];
   }
       
   // getLatticeDensity
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the lattice density, which is defined to be:
   //      (# of non -silence- words) / (# seconds from start to end index)
   //      Note that multiwords (e.g. to_the) count as a single non-silence word
   public double getLatticeDensity() {
      double answer = nonSilenceWords / nodeTimes[endIdx];
      return answer;
   }
   
   // writeAsDot - write lattice in dot format
   // Pre-conditions:
   //    - dotFilename is the name of the intended output file
   // Post-conditions:
   //    - The lattice is written in the specified dot format to dotFilename
   public void writeAsDot(String dotFilename) {
      java.io.PrintStream output = null;
        try {
            output = new java.io.PrintStream(dotFilename);
        } catch( java.io.FileNotFoundException e ) {
            System.err.println("Error: Unable to open file " + dotFilename + " for writing");
            System.exit(1);
        }
        output.println("digraph g {" + "\n"+ "   " + "rankdir=\"LR\"");
        for (int i=0; i < numNodes; i++){
          for (int j=0; j < numNodes; j++){
            Edge anEdge = adjMatrix[i][j];
            if (anEdge != null){
               output.println("   " + i + " -> " + j + " [label = \"" + anEdge.getLabel() + "\"]");    
            }
          }
        }   
        output.println("}");
   }
   
   // saveAsFile - write in the simplified lattice format (same as input format)
   // Pre-conditions:
   //    - latticeOutputFilename is the name of the intended output file
   // Post-conditions:
   //    - The lattice's toString() representation is written to the output file
   public void saveAsFile(String latticeOutputFilename) {
        java.io.PrintStream output = null;
        try {
            output = new java.io.PrintStream(latticeOutputFilename);
        } catch( java.io.FileNotFoundException e ) {
            System.err.println("Error: Unable to open file " + latticeOutputFilename + " for writing");
            System.exit(1);
        }
        output.println(this.toString());    
   }  
   
   // uniqueWordsAtTime - find all words at a certain point in time
   // Pre-conditions:
   //    - time is the time you want to query
   // Post-conditions:
   //    - A HashSet is returned containing all unique words that overlap
   //      with the specified time
   //     (If the time is not within the time range of the lattice, the Hashset should be empty)
   public java.util.HashSet<String> uniqueWordsAtTime(double time) {
      HashSet<String> words = new HashSet<String>();
      for (int i=0; nodeTimes[i] <= time; i++) {
         for (int j=0; j < numNodes; j++) {
            if (adjMatrix[i][j] != null  &&  nodeTimes[j] >= time) {
               Edge anEdge = adjMatrix[i][j];
               String aWord = anEdge.getLabel();
               words.add(aWord);
            }  
         }    
      }      
      return words;
   }
   
   // printSortedHits - print in sorted order all times where a given token appears
   // Pre-conditions:
   //    - word is the word (or multiword) that you want to find in the lattice
   // Post-conditions:
   //    - The midpoint (halfway between start and end time) for each instance of word
   //      in the lattice is printed to two decimal places in sorted (ascending) order
   //      All times should be printed on the same line, separated by a single space character
   //      (If no instances appear, nothing is printed)
   public void printSortedHits(String word) {
      List<String> times = new ArrayList<String>();
      
      for (int i=0; i<numNodes;i++ ) {
         for (int j=0; j<numNodes; j++) {
            if (adjMatrix[i][j] != null) {
               Edge anEdge = adjMatrix[i][j];
               String aWord = anEdge.getLabel();
                  if (aWord.equals(word)){
                     double start = nodeTimes[i];
                     double end = nodeTimes[j];
                     String num = String.format("%.2f", (start+end)/2);
                     times.add(num);
                  }        
            }
         }      
      }
      Collections.sort(times);
      for (String t: times) {
         System.out.print(t + " ");
      }
   }
}
