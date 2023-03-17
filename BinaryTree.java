
import java.util.*;

/**
 * 
 * @author me
 * a node in a binary search tree
 */
class BTNode{
	BTNode left, right;
	String term;
	ArrayList<Integer> docLists;
	
	/**
	 * Create a tree node using a term and a document list
	 * @param term the term in the node
	 * @param docList the ids of the documents that contain the term
	 */
	public BTNode(String term, ArrayList<Integer> docList)
	{
		this.term = term;
		this.docLists = docList;
	}
	
}

/**
 * 
 * Binary search tree structure to store the term dictionary
 */
public class BinaryTree {

	/**
	 * insert a node to a subtree 
	 * @param node root node of a subtree
	 * @param iNode the node to be inserted into the subtree
	 */
	public void add(BTNode node, BTNode iNode)
	{
		//TO BE COMPLETED

		BTNode x = node;
		BTNode y = null;

		while (x != null) {
			y = x;
			if (iNode.term.compareTo(x.term) < 0) {
				x = x.left;
			} else {
				x = x.right;
			}
		}

		if (y == null) {
			return ;
		}

		else if (iNode.term.compareTo(y.term) < 0) {
			y.left = iNode;
		}

		else {
			y.right = iNode;
		}
	}
	
	/**
	 * Search a term in a subtree
	 * @param n root node of a subtree
	 * @param key a query term
	 * @return tree nodes with term that match the query term or null if no match
	 */
	public BTNode search(BTNode n, String key)
	{
		//TO BE COMPLETED
		BTNode x = n;
		while (x != null) {
			if (x.term.equals(key)) {
				return x;
			}
			else {
				if (x.term.compareTo(key) > 0) {
					x = x.left;
				}
				else {
					x = x.right;
				}
			}
		}
		return null;
	}
	
	/**
	 * Implement a wildcard search in a subtree
	 * @param n the root node of a subtree
	 * @param key a wild card term, e.g., ho (terms like home will be returned)
	 * @return tree nodes that match the wild card
	 */
	public ArrayList<BTNode> wildCardSearch(BTNode n, String key)
	{
		//TO BE COMPLETED
		{
			ArrayList<BTNode> reqterms = new ArrayList<BTNode>();
			if(n==null)
				return null;
			ArrayList<BTNode> x ;
			if(n.term.startsWith(key)){
				reqterms.add(n);
				x = wildCardSearch(n.left, key);
				if(x!=null)
					reqterms.addAll(x);
				x=wildCardSearch(n.right, key);
				if(x!=null)
					reqterms.addAll(x);
				return reqterms;
			}else if (n.term.compareTo(key) < 0)
				return wildCardSearch(n.right, key);
			else
				return wildCardSearch(n.left, key);

		}



	}

	/**
	 * Print the inverted index based on the increasing order of the terms in a subtree
	 * @param node the root node of the subtree
	 */

	public void printInOrder(BTNode node)
	{

		//TO BE COMPLETED
		if (node == null) {
			return ;
		}
		printInOrder(node.left);
		System.out.println(node.term + node.docLists);
		printInOrder(node.right);
	}
	}

