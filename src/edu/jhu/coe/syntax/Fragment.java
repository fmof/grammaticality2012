package edu.jhu.coe.syntax;

import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.syntax.Trees;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.LinkedList;
import java.util.Iterator;

/**
   @author Matt Post
   @author Frank Ferraro
*/
public class Fragment implements Iterable<Fragment.Rule> {
    public Tree<Integer> tree_;
    public int numRules_;
    protected List<Tree<Integer>> frontier_;
    //private Tree<Integer> parent_;

    /**
     * Create a fragment from a string.  Note that the string must
     * denote internal nodes with asterisks, or the fragment will
     * parse as just the top level.  For example, the following two
     * lines will create identical fragments:
     *
     * <code>Fragment f = new Fragment("(S NP VP)");</code>
     * <code>Fragment f = new Fragment("(S NP (VP (VBD said) NP))");</code>
     *
     * The following line gives a large fragment with the expected
     * predicate argument structure:
     *
     * <code>Fragment f = new Fragment("(S NP (*VP (*VBD said) NP))");</code>
     */
    public Fragment(String line) {
        numRules_ = 0;
	frontier_ = new LinkedList<Tree<Integer>>();
        tree_ = extractFragment(Trees.stringTreeToIntegerTree(Trees.PennTreeReader.parseEasy(line)));
    }

    public Fragment(Tree<Integer> node) {
        numRules_ = 0;
	frontier_ = new LinkedList<Tree<Integer>>();
        tree_ = extractFragment(node);
    }

    public Tree<Integer> getTree() {
        return tree_;
    }

    public int getLabel() {
        return tree_.getLabel();
    }
    
    public int getNumRules() {
        if (numRules_ == 0) {
            for (Rule rule : this) 
                numRules_++;
        }

        // System.err.println("  getNumRules(" + this.toString() + ") = " + numRules_);
        return numRules_;
    }

    public boolean equals(Object that) {
        if (this == that)  return true;
        if ( !(that instanceof Fragment) ) return false;
        Fragment otherFragment = (Fragment) that;
        return getTree().equals(otherFragment.getTree());
    }

    public int hashCode() {
        return getTree().hashCode();
    }

    /**
     * Extracts the fragment rooted at <code>tree</code>.  A fragment
     * is a piece of a larger (complete) tree whose root node and leaf
     * nodes are not marked as internal nodes (see
     * <code>isInternal()</code>) in the larger tree.  This function
     * works by calling by calling
     * <code>extractFragmentChildren()</code> recursively on its
     * children nodes (two functions are needed because the top-level
     * tree's children are always added, whereas subsequent children
     * are added only for internal nodes).
     *
     * @param tree A tree node
     * @return The fragment rooted at tree
     */
    private Tree<Integer> extractFragment(Tree<Integer> tree) {
        Tree<Integer> newTree = new Tree<Integer>(tree.getLabel());
        newTree.setInternal(tree.isInternal());
        // assume the tree we're copying from is a complete tree, i.e.,
        // one for which isLeaf() == isTerminal()
        newTree.setTerminal(tree.isLeaf());
        ArrayList<Tree<Integer>> children = new ArrayList<Tree<Integer>>();
        for (Tree<Integer> child : tree.getChildren()) {
            // System.out.println("  adding children of " + tree.getLabel());
            Tree<Integer> newChild = extractFragmentChildren(child);
            children.add(newChild);
        }
        newTree.setChildren(children);
        return newTree;
    }

    private Tree<Integer> extractFragmentChildren(Tree<Integer> tree) {
        Tree<Integer> newTree = new Tree<Integer>(tree.getLabel());
        newTree.setInternal(tree.isInternal());
        newTree.setTerminal(tree.isLeaf());

        if (tree.isInternal()) {
            ArrayList<Tree<Integer>> children = new ArrayList<Tree<Integer>>();
            for (Tree<Integer> child : tree.getChildren()) {
                Tree<Integer> newChild = extractFragmentChildren(child);
                children.add(newChild);
            }
            newTree.setChildren(children);
        } 
        return newTree;
    }

    private static String getHumanTree(Tree<Integer> T){
	return Trees.integerTreeToStringTree(T).toTerminalMarkedString();
    }

    public static List<Tree<Integer>> getFrontierFragments(Tree<Integer> F, Tree<Integer> T){
	List<Tree<Integer>> pfrontier = new LinkedList<Tree<Integer>>();
	//simultaneously descend on both trees until the fragment's current
	//head is a leaf, and add that corresponding subtree from T's
	//current head
	Tree<Integer> currTree = T.shallowClone();
	Tree<Integer> origFragToTree = F.shallowClone();
	ConcurrentLinkedQueue<Tree<Integer>> q = new ConcurrentLinkedQueue<Tree<Integer>>();
	ConcurrentLinkedQueue<Tree<Integer>> q2 = new ConcurrentLinkedQueue<Tree<Integer>>();
	q.add(origFragToTree);
	q2.add(currTree);
	while(!q.isEmpty()){
	    Tree<Integer> cft = q.poll();
	    Tree<Integer> ct = q2.poll();
	    //it's in the frontier of X if it's a leaf in X
	    if(cft.getChildren() != null && cft.getChildren().size() > 0){
		//currFragToTree.setChildren(cft.getChildren());
		for(Tree<Integer> cftc : cft.getChildren()){
		    q.add(cftc);
		}
		for(Tree<Integer> ctc : ct.getChildren())
		    q2.add(ctc);
	    } else{
		if(!ct.isLeaf()){
		    cft.setChildren((new Fragment(ct)).getTree().getChildren());
		    Tree<Integer> ttc = (Fragment.getUpperMostRoot(cft)).shallowClone();
		    ttc.calculateNumberOfExpansions();
		    pfrontier.add(ttc);
		    cft.getChildren().clear();	    
		}
	    }
	}
	currTree = null;
	origFragToTree = null;
	q=null; q2=null;
	return pfrontier;
    } 

    private static Tree<Integer> getUpperMostRoot(Tree<Integer> T){
	if(T.getParent() == null){
	    return T;
	}
	return Fragment.getUpperMostRoot(T.getParent());
    }


    public Tree<Integer> getUpperMostRoot(){
	Tree<Integer> t = this.getTree().getParent();
	if(t == null){
	    return this.getTree();
	}
	return (new Fragment(t)).getUpperMostRoot();
    }

    public String toString() {
        return Trees.integerTreeToStringTree(getTree()).toTerminalMarkedString();
    }

    public List<Tree<Integer>> getFrontier() {
	return frontier_;
    }

    public String toIntString() {
	return getTree().toString();
    }
    
    public void printIntString() {
	System.out.println(toIntString());
    }

    public static class Rule implements java.io.Serializable {
        protected int label_;
        protected ArrayList<Integer> children_;

        public Rule(Tree<Integer> tree) {
            label_    = tree.getLabel();
            children_ = new ArrayList<Integer>();
            for (Tree<Integer> child : tree.getChildren())
                children_.add(child.getLabel());
        }

        public Rule(int label, ArrayList<Integer> children) {
            label_    = label;
            children_ = children;
        }

        public String toString() {
            String str = Trees.labelIntToString.get(getLabel()) + " ->";
            for (Integer kid : getChildren())
                str += " " + Trees.labelIntToString.get(kid);
            return str;
        }

        public int getLabel() {
            return label_;
        }


        public ArrayList<Integer> getChildren() {
            return children_;
        }

        public int hashCode() {
            return getLabel() + getChildren().hashCode();
        }

        public boolean equals(Object that) {
            if (this == that)  return true;
            if ( !(that instanceof Rule) ) return false;
            Rule otherRule = (Rule) that;
            if (getLabel() != otherRule.getLabel())
                return false;
            if (getChildren().size() != otherRule.getChildren().size())
                return false;
            for (int i = 0; i < getChildren().size(); i++)
                if (getChildren().get(i) != otherRule.getChildren().get(i))
                    return false;
            return true;
        }
    }


    public Iterator<Fragment.Rule> iterator() {
        return new FragmentIterator();
    }

    private class FragmentIterator implements Iterator {

        private List<Tree<Integer>> treeStack;

        private FragmentIterator() {
            treeStack = new ArrayList<Tree<Integer>>();
            treeStack.add(Fragment.this.getTree());
        }

        public boolean hasNext() {
            return (!treeStack.isEmpty());
        }

        public Object next() {
            int lastIndex = treeStack.size() - 1;
            Tree<Integer> tree = treeStack.remove(lastIndex);
            List<Tree<Integer>> kids = tree.getChildren();
            // so that we can efficiently use one List, we reverse them
            for (int i = kids.size() - 1; i >= 0; i--) {
                if (kids.get(i).getChildren().size() > 0)
                    treeStack.add(kids.get(i));
            }
            
            return new Rule(tree);
        }

        /**
         * Not supported
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static void main(String args[]) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String treeStr1 = "(S (*NP DT NN) (VP (VBD was)))";
        String treeStr2 = "(S (*NP DT NN) VP)";

        Fragment f1 = new Fragment(treeStr1);
        System.out.println("string:           " + treeStr1);
        System.out.println("string from tree: " + f1.toString());



        //Fragment f2 = new Fragment(Trees.stringTreeToIntegerTree(Trees.PennTreeReader.readTreeFromLine(treeStr2)));
        //System.out.println(f2);

        Fragment f2 = new Fragment(f1.toString());
	System.out.println(f2.toString());
        System.out.println("equal: " + f2.equals(f1));
        System.out.println("hashes equal: " + (f1.hashCode() == f2.hashCode()));
    }
}
