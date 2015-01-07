/*
* PhyloTree.java
*
* Defines a phylogenetic tree, which is a strictly binary tree
* that represents inferred hierarchical relationships between species
*
* There are weights along each edge; the weight from parent to left child
* is the same as parent to right child.
*
* Jason Hack
*
*/

import java.util.*;
import java.io.*;
import java.lang.*;

public class PhyloTree {
   private PhyloTreeNode overallRoot;    // The actual root of the overall tree
   private int printingDepth;            // How many spaces to indent the deepest
                                         // node when printing
   private StringBuilder builder;        // stores results of toString() method
   private StringBuilder builderTree;    // stores results of toTreeString method
   
   // CONSTRUCTOR
   
   // PhyloTree
   // Pre-conditions:
   //        - speciesFile contains the path of a valid FASTA input file
   //        - printingDepth is a positive number
   // Post-conditions:
   //        - this.printingDepth has been set to printingDepth
   //        - A linked tree structure representing the inferred hierarchical
   //          species relationship has been created, and overallRoot points to
   //          the root of this tree
   public PhyloTree(String speciesFile,int printingDepth) {
      builder = new StringBuilder();
      builderTree = new StringBuilder();
      if(printingDepth < 0) {
         System.err.println("Error: negative printing Depth ");
         throw new IndexOutOfBoundsException();
      }
      this.printingDepth = printingDepth;
      buildTree(loadSpeciesFile(speciesFile));
      return;
   }
   
   // ACCESSORS
   
   // getOverallRoot
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the overall root
   public PhyloTreeNode getOverallRoot() {
      return overallRoot;
   }
   
   // toString
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns a string representation of the tree
   public String toString() {
      return toString(overallRoot, 0.0, weightedNodeHeight(overallRoot));
   }
   
   // toString
   // Pre-conditions:
   //    - node points to the root of a tree you intend to print
   //    - weightedDepth is the sum of the edge weights from the
   //      overall root to the current root
   //    - maxDepth is the weighted depth of the overall tree
   // Post-conditions:
   //    - Returns a string representation of the tree
   private String toString(PhyloTreeNode node, double weightedDepth, double maxDepth) {
      if(node == null) {
         return "";
      }
      toString(node.getRightChild(), weightedDepth + node.getDistanceToChild(), maxDepth);
      int k = (int) Math.ceil(printingDepth * (weightedDepth / maxDepth));
      for(int i = 0; i < k; i++) {
         builder.append(".");
      }
      builder.append(node.toString() + "\n");
      toString(node.getLeftChild(), weightedDepth + node.getDistanceToChild(), maxDepth);
      return builder.toString();
   }
   
   // toTreeString
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns a string representation in tree format
   public String toTreeString() {
      return toTreeString(overallRoot);
   }
   
   // toTreeString
   // Pre-conditions:
   //    - node points to the root of a tree you intend to print
   // Post-conditions:
   //    - Returns a string representation in tree format
   private String toTreeString(PhyloTreeNode node) {
      if(node == null) {
         return "";
      }
      if(node.isLeaf()) {
         if(node.getParent() != null)
            builderTree.append(node.getLabel() + ":" + String.format("%.5f", node.getParent().getDistanceToChild()));
         else{
            builderTree.append(node.getLabel() + ":0.0");
         }
      }
      else{
         builderTree.append("(");
         toTreeString(node.getRightChild());
         builderTree.append(",");
         toTreeString(node.getLeftChild());
         builderTree.append(")");
         if(node.getParent() != null) {
            builderTree.append(":" + String.format("%.5f", node.getParent().getDistanceToChild()));
         }
      }
      return builderTree.toString();
   }
   
   // getHeight
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the tree height as defined in class
   public int getHeight() {
      return nodeHeight(overallRoot);
   }
   
   // getWeightedHeight
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the sum of the edge weights along the
   //      "longest" (highest weight) path from the root
   //      to any leaf node.
   public double getWeightedHeight() {
      return weightedNodeHeight(overallRoot);
   }
   
   // countAllSpecies
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns the number of species in the tree
   public int countAllSpecies() {
      if(overallRoot == null)
         return 0;
      return (overallRoot.getNumLeafs());
   }
   
   // getAllSpecies
   // Pre-conditions:
   //    - None
   // Post-conditions:
   //    - Returns an ArrayList containing all species in the tree
   public java.util.ArrayList<Species> getAllSpecies() {
      ArrayList<Species> answer = new ArrayList<Species>();
      getAllDescendantSpecies(overallRoot, answer);
      return answer;
      
   }
   
   // findTreeNodeByLabel
   // Pre-conditions:
   //    - label is the label of a tree node you intend to find
   //    - Assumes labels are unique in the tree
   // Post-conditions:
   //    - If found: returns the PhyloTreeNode with the specified label
   //    - If not found: returns null
   public PhyloTreeNode findTreeNodeByLabel(String label) {
      return findTreeNodeByLabel(overallRoot, label);
   }
   
   // findLeastCommonAncestor
   // Pre-conditions:
   //    - label1 and label2 are the labels of two species in the tree
   // Post-conditions:
   //    - If either node cannot be found: returns null
   //    - If both nodes can be found: returns the PhyloTreeNode of their
   //      common ancestor with the largest depth
   //      Put another way, the least common ancestor of nodes A and B
   //      is the only node in the tree where A is in the left tree
   //      and B is in the right tree (or vice-versa)
   public PhyloTreeNode findLeastCommonAncestor(String label1, String label2) {
      PhyloTreeNode node1 = findTreeNodeByLabel(label1);
      PhyloTreeNode node2 = findTreeNodeByLabel(label2);
      return findLeastCommonAncestor(node1, node2);
   }
   
   // findEvolutionaryDistance
   // Pre-conditions:
   //    - label1 and label2 are the labels of two species in the tree
   // Post-conditions:
   //    - If either node cannot be found: returns POSITIVE_INFINITY
   //    - If both nodes can be found: returns the sum of the weights
   //      along the paths from their least common ancestor to each of
   //      the two nodes
   public double findEvolutionaryDistance(String label1, String label2) {
      PhyloTreeNode node1 =  findTreeNodeByLabel(label1);
      PhyloTreeNode node2 = findTreeNodeByLabel(label2);
      if(node1 == null || node2 == null) {
         return java.lang.Double.POSITIVE_INFINITY;
      }
      PhyloTreeNode ancestor = findLeastCommonAncestor(node1, node2);
      return (findEvolutionaryDistanceHelper(ancestor, node1) + findEvolutionaryDistanceHelper(ancestor, node2));
   }
   
   // Recursive helper method for findEvolutionaryDistance
   private double findEvolutionaryDistanceHelper(PhyloTreeNode ancestor, PhyloTreeNode node) {
      if(ancestor == null || ancestor == node) {
         return 0.0;
      }
      if(ancestor.getLeftChild() != null && ancestor.getLeftChild().getLabel().contains(node.getLabel())) {
         return (ancestor.getDistanceToChild() + findEvolutionaryDistanceHelper(ancestor.getLeftChild(), node));
      }
      else if (ancestor.getRightChild() != null && ancestor.getRightChild().getLabel().contains(node.getLabel())) {
         return (ancestor.getDistanceToChild() + findEvolutionaryDistanceHelper(ancestor.getRightChild(), node));
      }
      else{
         return 0.0;
      }
   }
   
   // MODIFIER
   
   // buildTree
   // Pre-conditions:
   //    - species contains the set of species for which you want to infer
   //      a phylogenetic tree
   // Post-conditions:
   //    - A linked tree structure representing the inferred hierarchical
   //      species relationship has been created, and overallRoot points to
   //      the root of said tree
   private void buildTree(Species[] species) {
      List<PhyloTreeNode> treeList = new ArrayList<PhyloTreeNode>();
      MultiKeyMap<Double> speciesMap = new MultiKeyMap<Double>();
      for(Species s : species) {
         treeList.add(new PhyloTreeNode(null, s));
      }
      for(int i = 0; i < species.length - 1; i++) {
         for(int j = i + 1; j < species.length; j++) {
            speciesMap.put(species[i].getName(), species[j].getName(), Species.distance(species[i], species[j]));
         }
      }
      while(treeList.size() > 1) {
         double minValue = java.lang.Double.POSITIVE_INFINITY;
         String currentString = "";
         for (Map.Entry<String, Double> entry : speciesMap.entrySet()) {
            double value = entry.getValue();
            if(value < minValue ) {
               minValue = value;
            }
         }
         for (Map.Entry<String, Double> entry : speciesMap.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();
            String[] wordArray = compareKeys(key);
            String newString = (wordArray[0] + "|" + wordArray[1]);
            if(value == minValue && (newString.compareTo(currentString) < 1 || currentString == "" )) {
               currentString = newString;
            }
         }
         PhyloTreeNode less = null;
         PhyloTreeNode great = null;
         String[] twoWords = compareKeys(currentString);
         for(int i = 0; i < treeList.size(); i++) {
            if(treeList.get(i).getLabel().equals(twoWords[0]))
               less = treeList.get(i);
            if(treeList.get(i).getLabel().equals(twoWords[1]))
               great = treeList.get(i);
         }
         String lesser = less.getLabel();
         String greater = great.getLabel();
         for(PhyloTreeNode s : treeList) {
            if(!s.getLabel().equals(lesser) && !s.getLabel().equals(greater)) {
               double numLessLeaf = less.getNumLeafs();
               double numGreatLeaf = great.getNumLeafs();
               double answer = ( ((numLessLeaf /(numLessLeaf + numGreatLeaf)) * speciesMap.get(lesser, s.getLabel())) +
               ((numGreatLeaf /(numLessLeaf + numGreatLeaf)) * speciesMap.get(greater, s.getLabel())));
               speciesMap.remove(lesser, s.getLabel());
               speciesMap.remove(greater, s.getLabel());
               speciesMap.put(lesser + "+" + greater, s.getLabel(), answer);
            }
         }
         PhyloTreeNode combined =  new PhyloTreeNode(lesser + "+" + greater, null, less, great, speciesMap.get(lesser, greater) / 2.0);
         combined.getLeftChild().setParent(combined);
         combined.getRightChild().setParent(combined);
         treeList.add(combined);
         treeList.remove(less);
         treeList.remove(great);
         speciesMap.remove(lesser, greater);
      }
      if(treeList.size() != 0)
         overallRoot = treeList.get(0);
      return;
   }
   
   // helper method that given an entrySet key input, extracts the two words, then
   // returns them in an array with the alphebetically lesser string at index 0 and the
   // alphebetically greater string at index 1
   private String[] compareKeys(String currentString) {
      String s1 = "";
      String s2 = "";
      String[] answer = new String[2];
      for(int i = 0; i < currentString.length(); i++) {
         if(currentString.charAt(i) == '|') {
            s1 = currentString.substring(0,i);
            s2 = currentString.substring(i + 1);
         }
      }
      int compare = s1.compareTo(s2);
      String lesser = "";
      String greater = "";
      if(compare < 1) {
         lesser = s1;
         greater = s2;
      }
      else{
         lesser = s2;
         greater = s1;
      }
      answer[0] = lesser;
      answer[1] = greater;
      return answer;
   }
   
   // STATIC
   
   // nodeDepth
   // Pre-conditions:
   //    - node is null or the root of tree (possibly subtree)
   // Post-conditions:
   //    - If null: returns -1
   //    - Else: returns the depth of the node within the overall tree
   public static int nodeDepth(PhyloTreeNode node) {
      if(node == null) {
         return -1;
      }
      else if(node.getParent() == null) {
         return 0;
      }
      else{
         return 1 + nodeDepth(node.getParent());
      }
   }
   
   // nodeHeight
   // Pre-conditions:
   //    - node is null or the root of tree (possibly subtree)
   // Post-conditions:
   //    - If null: returns -1
   //    - Else: returns the height subtree rooted at node
   public static int nodeHeight(PhyloTreeNode node) {
      if(node == null) {
         return -1;
      }
      else{
         return 1 + Math.max(nodeHeight(node.getLeftChild()), nodeHeight(node.getRightChild()));
      }
   }
   
   
   // weightedNodeHeight
   // Pre-conditions:
   //    - node is null or the root of tree (possibly subtree)
   // Post-conditions:
   //    - If null: returns NEGATIVE_INFINITY
   //    - Else: returns the weighted height subtree rooted at node
   //     (i.e. the sum of the largest weight path from node
   //     to a leaf; this might NOT be the same as the sum of the weights
   //     along the longest path from the node to a leaf)
   public static double weightedNodeHeight(PhyloTreeNode node) {
      if(node == null) {
         return java.lang.Double.NEGATIVE_INFINITY;
      }
      else{
         return weightedNodeHeightHelper(node);
      }
   }
   
   // Recursive helper method for weightedNodeHeight
   private static double weightedNodeHeightHelper(PhyloTreeNode node) {
      if(node == null) {
         return 0;
      }
      else{
         return Math.max(node.getDistanceToChild() + weightedNodeHeightHelper(node.getLeftChild()),
         node.getDistanceToChild() + weightedNodeHeightHelper(node.getRightChild()));
      }
   }
   
   // loadSpeciesFile
   // Pre-conditions:
   //    - filename contains the path of a valid FASTA input file
   // Post-conditions:
   //    - Creates and returns an array of species objects representing
   //      all valid species in the input file
   public static Species[] loadSpeciesFile(String filename) {
      java.util.Scanner input = null;
      java.io.File inputFile = new java.io.File(filename);
      try {
         input = new java.util.Scanner(inputFile);
      } catch( java.io.FileNotFoundException e ) {
         System.err.println("Error: Unable to open file " + filename);
         System.exit(1);
      }
      
      String current = "";
      if(input.hasNext())
         current = input.next();
      List<Species> speciesList = new ArrayList<Species>();
      while(input.hasNext()) {
         int count = 0;
         int i = 0;
         StringBuilder sequence = new StringBuilder();
         String speciesName = "";
         while(count < 6 && i < current.length() && current.substring(0,1).equals(">") ) {
            if(current.charAt(i) == '|')
               count++;
            if(count == 6 && !current.substring(i + 1).contains("|")) {
               speciesName = current.substring(i + 1);
            }
            i++;
         }
         if(count == 6) {
            current = input.next();
            boolean next = true;
            while (next == true && !current.substring(0,1).equals(">")) {
               sequence.append(current);
               if(input.hasNext())
                  current = input.next();
               else
                  next = false;
            }
            String[] sequenceArray = new String[sequence.length()];
            for(int j = 0; j < sequence.length(); j++) {
               sequenceArray[j] = Character.toString(sequence.charAt(j));
            }
            Species currSpecies = new Species(speciesName, sequenceArray);
            speciesList.add(currSpecies);
         }
         else
            current = input.next();
      }
      Species[] arraySpecies = new Species[speciesList.size()];
      for (int i = 0; i < speciesList.size(); i++) {
         arraySpecies[i] = speciesList.get(i);
      }
      return arraySpecies;
   }
   
   // getAllDescendantSpecies
   // Pre-conditions:
   //    - node points to a node in a phylogenetic tree structure
   //    - descendants is a non-null reference variable to an empty arraylist object
   // Post-conditions:
   //    - descendants is populated with all species in the subtree rooted at node
   //      in in-/pre-/post-order (they are equivalent here)
   private static void getAllDescendantSpecies(PhyloTreeNode node,java.util.ArrayList<Species> descendants) {
      getAllDescendantSpeciesHelper(node, descendants);
   }
   
   // Recursive helper method for getAllDescendantSpecies
   private static ArrayList<Species> getAllDescendantSpeciesHelper(PhyloTreeNode node,java.util.ArrayList<Species> descendants) {
      if(node == null)
         return descendants;
      if(node.isLeaf()) {
         descendants.add(node.getSpecies());
         return descendants;
      }
      else{
         if(node.getLeftChild() != null) {
            descendants = getAllDescendantSpeciesHelper(node.getLeftChild(), descendants);
         }
         if(node.getRightChild() != null) {
            descendants = getAllDescendantSpeciesHelper(node.getRightChild(), descendants);
         }
      }
      return descendants;
   }
   
   // findTreeNodeByLabel
   // Pre-conditions:
   //    - node points to a node in a phylogenetic tree structure
   //    - label is the label of a tree node that you intend to locate
   // Post-conditions:
   //    - If no node with the label exists in the subtree, return null
   //    - Else: return the PhyloTreeNode with the specified label
   
   //    - Assumes labels are unique in the tree
   private static PhyloTreeNode findTreeNodeByLabel(PhyloTreeNode node,String label) {
      if(node == null)
         return null;
      if(node.getLabel().equals(label))
         return node;
      if(node.getLeftChild() != null && node.getLeftChild().getLabel().contains(label))
         return findTreeNodeByLabel(node.getLeftChild(), label);
      if(node.getRightChild() != null && node.getRightChild().getLabel().contains(label))
         return findTreeNodeByLabel(node.getRightChild(), label);
      return null;
   }
   
   // findLeastCommonAncestor
   // Pre-conditions:
   //    - node1 and node2 point to nodes in the phylogenetic tree
   // Post-conditions:
   //    - If node1 or node2 are null, return null
   //    - Else: returns the PhyloTreeNode of their common ancestor
   //      with the largest depth
   private static PhyloTreeNode findLeastCommonAncestor(PhyloTreeNode node1, PhyloTreeNode node2) {
      if(node1 == null || node2 == null) {
         return null;
      }
      else{
         String label1 = node1.getLabel();
         String label2 = node2.getLabel();
         if(label1.contains(label2) && (label2.contains("+") || label1.equals(label2)))
            return node1;
         else if(label2.contains(label1) && (label2.contains("+") || label2.equals(label1)))
            return node2;
         else{
            return findLeastCommonAncestor(node1.getParent(), node2.getParent());
         }
      }
   }
}
