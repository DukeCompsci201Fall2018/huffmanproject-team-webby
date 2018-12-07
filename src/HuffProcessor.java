import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		
		in.reset();
		writeCompressedBits(codings,in,out);
		out.close();
	}
				
		public int[]  readForCounts(BitInputStream in) {
			int[] counts = new int[ALPH_SIZE +1];
			//int val = in.readBits(BITS_PER_WORD);
			while(true) {
				int val = in.readBits(BITS_PER_WORD);
				if(val == -1) break; // if out of bits to read
				else {
					counts[val] +=1;  // indexing frequency by one 
				}
			}
			counts[PSEUDO_EOF] = 1;
			return counts;  // check to makes sure it is right
		}
		
		
		public HuffNode makeTreeFromCounts( int[] counts) {
			PriorityQueue<HuffNode> pq = new PriorityQueue<>();
			
			for(int k = 0; k < counts.length; k+=1) { // for every index such that freq[index]
			    if(counts[k]>0) {
				pq.add(new HuffNode(k,counts[k],null,null)); //index, freq index, null, null
			    }
			}

			while (pq.size() > 1) {
			    HuffNode left = pq.remove();
			    HuffNode right = pq.remove();
			     HuffNode t = new HuffNode(0,left.myWeight+right.myWeight, left, right);// create new HuffNode t with weight from
			    // left.weight+right.weight and left, right subtrees
			    pq.add(t);
			}
			HuffNode root = pq.remove();
			return root;
		}
		
		public String[] makeCodingsFromTree(HuffNode root) {
			String[] encodings = new String[ALPH_SIZE + 1];
		    codingHelper(root,"",encodings);
		    return encodings; // check to make sure this is right

		}
		public void codingHelper(HuffNode root, String path, String[] encodings) {
			 if (root.myLeft == null && root.myRight == null) {
			        encodings[root.myValue] = path;
			        return;
			   }
			 else {
				 codingHelper(root.myLeft, path +"0", encodings); // if not a 
				 codingHelper(root.myRight, path + "1", encodings);
			 }

		}
		
		public void writeHeader(HuffNode root, BitOutputStream out) {
			if(root == null) {
				return;
			}
			if(root.myLeft != null || root.myRight != null) { // if not a leaf
				out.writeBits(1, 0); // write a single bit with value 0
				writeHeader(root.myLeft,out); // recursive calls for left and right trees, internal node bc has children
				writeHeader(root.myRight,out)
;			}
		
			else {
				out.writeBits(1, 1); // write a single bit with value 1
				out.writeBits(BITS_PER_WORD+1, root.myValue); //nine bits stored in the leaf
			}
		}
		public void writeCompressedBits(String [] codings, BitInputStream in, BitOutputStream out) {
			while(true) {
				int val = in.readBits(BITS_PER_WORD);
				if(val == -1) break; // if out of bits to read
				
			
			String code = codings[val];
			//System.out.println(out); 
			//System.out.println(code);
			if (code == null) System.out.println(val);
			out.writeBits(code.length(), Integer.parseInt(code,2));
			}
			String code = codings[PSEUDO_EOF];
				    out.writeBits(code.length(), Integer.parseInt(code,2));

		}
		
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
//	}
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		

		
		
	

		int bits = in.readBits(BITS_PER_INT);
		if(bits != HUFF_TREE || bits == -1) {
		throw new HuffException("illegal header starts with "+bits);
		}
		
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root,in,out);
		out.close();
	}
		// Tree helper method
	public HuffNode readTreeHeader(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1) {
			throw new HuffException("illegal header starts with "+bit);
		}
		if (bit == 0) { // if zero, it an internal node
			HuffNode left = readTreeHeader(in); // recursive call
			HuffNode right = readTreeHeader(in); // recursive call
			return new HuffNode(0,0,left,right);
		}
		else { // knows this is a leaf
		 int value =  in.readBits(BITS_PER_WORD+1);//  read nine bits from input
			return new HuffNode(value,0,null,null);
		}
	}
		// compressed bits method
		public void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		HuffNode current = root; 
		   while (true) {
		       int bits = in.readBits(1);
		       if (bits == -1) {
		           throw new HuffException("bad input, no PSEUDO_EOF");
		       }
		       else { 
		           if (bits == 0) {
		        	   current = current.myLeft;
		           }
		           else {
		        	   current = current.myRight;
		           }
		       }
		       if (current.myLeft == null && current.myRight == null ) { // if current is a leaf node
	               if (current.myValue == PSEUDO_EOF) {// current value equals pseudo_eof
	                   break;   // out of loop
	               }
	               else {
                       out.writeBits(BITS_PER_WORD, current.myValue);      // write bits for current.value;
	                   current = root; // start back after leaf
	               }
		       }
	       }
		}  
}
		
		
		
		
		