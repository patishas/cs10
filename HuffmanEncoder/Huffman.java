import com.sun.source.tree.CatchTree;
import org.bytedeco.javacv.FrameFilter;

import java.io.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * A Huffman encoder that compresses and decompresses text files
 *
 * @author Shashank S. Patil (shashank.s.patil.22@dartmouth.edu) and Isaac Feldman (isaac.c.feldman.23@dartmouth.edu)
 * created Fall 2020 for COSC 10
 */
public class Huffman {

    private HashMap<Character, Integer> frequencyTable; //holds characters and character frequencies
    private BinaryTree<CharKey> codeTree; //holds Huffman code tree
    private HashMap<Character, ArrayList<Boolean>> codeMap; // holds characters and their binary codes

    //filenames
    private final String fileName;
    private final String compressedFileName;
    private final String decompressedFileName;

    //objects that allow reading/writing to/from files
    private BufferedReader input;
    private BufferedWriter output;
    private BufferedBitWriter bitWriter;
    private BufferedBitReader bitReader;

    /**
     * Initializes the Huffman encoder, holding the filenames
     */
    public Huffman(String filename) {
        this.fileName = filename;
        this.compressedFileName = fileName.substring(0, fileName.indexOf('.')) + "_compressed";
        this.decompressedFileName = fileName.substring(0, fileName.indexOf('.')) + "_decompressed.txt";
    }

    /**
     * Reads in the text file
     * Stores a HashMap of characters and character frequencies
     */
    public void readFile() {
        try {
            input = new BufferedReader(new FileReader(fileName)); //opens a file, saves info about current position in file
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found");
            return;
        }
        try {
            frequencyTable = new HashMap<Character, Integer>(); //stores characters and character frequencies
            while (input.ready()) { //reads until end of file
                char currentChar = (char) input.read(); //reads next character in file
                if (frequencyTable.containsKey(currentChar)) { //if character already seen
                    frequencyTable.put(currentChar, frequencyTable.get(currentChar) + 1); //increment its count
                } else {
                    frequencyTable.put(currentChar, 1); //else add character to frequencyTable w/ a count of 1
                }
            }
        } catch (IOException e) {
            System.out.println("IO Exception Occurred" + e.getMessage());
        }
        try {
            input.close(); //closes file
        } catch (IOException e) {
            System.out.println("Error Closing File");
        }
    }

    /**
     * Returns hashmap frequency table of characters (keys) and character frequencies (values)
     */
    public HashMap<Character, Integer> getFrequencyTable() {
        return frequencyTable;
    }

    /**
     * Encapsulates characters and their frequencies in objects
     */
    private class CharKey {
        protected char character;
        protected int frequency;

        public CharKey(char character, int frequency) {
            this.character = character;
            this.frequency = frequency;
        }

        public char getCharacter() {
            return character;
        }

        public int getFrequency() {
            return frequency;
        }

        @Override
        public String toString() {
            return character + ": " + frequency;
        }
    }

    /**
     * Creates the Huffman code tree (organized so highest frequency characters near top of tree)
     */
    public void buildCodeTree() {
        //creates initial tree for each character, adds into min priority queue
        //holds priority queue of binary character/character frequency trees (to build Huffman code tree)
        PriorityQueue<BinaryTree<CharKey>> queue = new PriorityQueue<BinaryTree<CharKey>>((o1, o2) -> o1.getData().getFrequency() - o2.getData().getFrequency());
        for (Character key : frequencyTable.keySet()) {
            queue.add(new BinaryTree<CharKey>(new CharKey(key, frequencyTable.get(key))));
        }

        while (queue.size() > 1) {
            //extract the two lowest frequency trees from the priority queue
            BinaryTree<CharKey> T1 = queue.poll();
            BinaryTree<CharKey> T2 = queue.poll();

            //create a new tree with the root node holding the sum of the 2 lowest frequencies
            //assign it the two lowest frequency trees as children
            BinaryTree<CharKey> r = new BinaryTree<CharKey>(new CharKey(Character.MIN_VALUE, T1.data.frequency + T2.data.frequency), T1, T2);

            queue.add(r); //inserts the new tree into the priority queue
        }
        codeTree = queue.poll(); // save the current tree

        if (codeTree != null && codeTree.size() == 1) { // handle the boundary case where the tree only has a root node.
            codeTree = new BinaryTree<CharKey>(new CharKey(Character.MIN_VALUE, codeTree.data.frequency), codeTree, null);
        }

    }

    /**
     * returns the Huffman code tree
     */
    public BinaryTree<CharKey> getCodeTree() {
        return codeTree;
    }

    /**
     * Builds a map of characters and their binary string codes in the Huffman code tree
     * Accumulator Pattern
     */
    public void buildCodeMap() {
        codeMap = new HashMap<Character, ArrayList<Boolean>>();
        if (codeTree != null) codeMapHelper("", codeTree);
    }

    private void codeMapHelper(String path, BinaryTree<CharKey> tree) {
        //postorder traversal
        if (tree.hasLeft()) { //go left and add 0 to path
            codeMapHelper(path + 0, tree.getLeft());
        }
        if (tree.hasRight()) { //go right and add 1 to path
            codeMapHelper(path + 1, tree.getRight());
        }
        if (tree.isLeaf()) { //put character and its binary string path in map
            codeMap.put(tree.data.getCharacter(), binaryStringtoBooleans(path));
        }
    }

    /**
     * Helper function to convert the binary string paths to lists of booleans for the BufferedBitWriter
     *
     * @param in a binary string to convert
     * @returns the corresponsing list of booleans (e.g. "1010" --> [true, false, true, false])
     */
    private ArrayList<Boolean> binaryStringtoBooleans(String in) {
        ArrayList<Boolean> res = new ArrayList<Boolean>();
        for (int i = 0; i < in.length(); i++) {
            if (in.charAt(i) == '0') {
                res.add(false);
            } else {
                res.add(true);
            }
        }

        return res;
    }

    /**
     * returns a map of chars to their respective Huffman codes.
     * Huffman codes are represented as lists of booleans
     * (e.g. [false, true, false, true] = 0101)
     */
    public HashMap<Character, ArrayList<Boolean>> getCodeMap() {
        return codeMap;
    }


    /**
     * Compresses the text file into a series of bits
     */
    public void compressFile() {

        try {
            input = new BufferedReader(new FileReader(fileName)); //opens file
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found");
        }
        try {
            bitWriter = new BufferedBitWriter(compressedFileName); //object for writing bits to file
        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e.getMessage());
        }

        try {
            while (input.ready()) { //reads until end of file
                char currentChar = (char) input.read(); //reads next character in file
                for (int i = 0; i < codeMap.get(currentChar).size(); i++) {
                    bitWriter.writeBit(codeMap.get(currentChar).get(i)); //look up the character's binary string code in map, write the code to a file
                }
            }
        } catch (IOException e) {
            System.out.println("IOException occurred" + e.getMessage());
        }

        try {
            //closes files
            input.close();
            bitWriter.close();
        } catch (IOException e) {
            System.out.println("Could not close files" + e.getMessage());
        }
    }

    /**
     * Decompresses the series of bits back into a text file
     */
    public void decompressFile() {
        try {
            bitReader = new BufferedBitReader(compressedFileName); //object for reading bits from file
        } catch (IOException e) {
            System.out.println("IOException occurred" + e.getMessage());
        }
        try {
            output = new BufferedWriter(new FileWriter(decompressedFileName)); //object for writing text to file
        } catch (IOException e) {
            System.out.println("IOException occurred" + e.getMessage());
        }
        try {
            BinaryTree<CharKey> location = codeTree;

            while (bitReader.hasNext()) {
                if (location.isLeaf()) { //leaf indicates we have found the character given by the code
                    output.write(location.getData().getCharacter());
                    location = codeTree; //return to root
                } else { // traverse to a leaf of the code tree
                    if (bitReader.readBit()) { // if true, go right
                        if (location.hasRight()) {
                            location = location.getRight();
                        }
                    } else { //if false, go left
                        if (location.hasLeft()) {
                            location = location.getLeft();
                        }
                    }
                }
            }
            if (codeTree != null && location.isLeaf()) {
                // the bitReader will exit before the last char is written to the output file, write that lat char...
                output.write(location.getData().getCharacter());
                location = codeTree;
            }
        } catch (IOException e) {
            System.out.println("IOException occurred" + e.getMessage());
        }

        try { //closes files
            bitReader.close();
            output.close();
        } catch (IOException e) {
            System.out.println("Could not close files" + e.getMessage());
        }


    }

    public static void main(String[] args) {
        Huffman test = new Huffman("HuffmanEncoder/WarAndPeace.txt");//loads in file
        test.readFile(); //reads file, creates frequency table

        System.out.println(test.getFrequencyTable());//prints table
        test.buildCodeTree(); //creates code tree
        System.out.println(test.getCodeTree()); //prints tree
        test.buildCodeMap(); //creates code map
        System.out.println(test.getCodeMap()); //prints map

        //creates compressed and decompressed files
        test.compressFile();
        test.decompressFile();
    }
}
