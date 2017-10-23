package story;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;

import story.Pos.PosType;
import story.Pos.PosType.PosTypeName;
import utils.ExtraLexicon;
import utils.StoryUtils;
import utils.StoryUtils.SearchableList;

/**
 * Class for generating stories
 * 
 */
public class Story {

	/**
	 * Map of pos and words. E.g. on entry for "NOUN" is "apple"
	 */
	private static final ListMultimap<PosTypeName, String> POS_WORD_MAP;
	/**
	 * Words and their part of speech.
	 */
	private static final Map<String, PosType> wordPosTypeMap;
	
	private static final Random RAND_GEN = new Random();
	//e.g. <p>1084 (4%) <code>VERB</code> nodes are leaves.</p>
		//data on the nodes parent child type stats. Generate number of
		//parent children stats.
	private static final Map<PosTypeName, SearchableList<Integer>> posTypePCProbMap; 
	private static final Pattern PC_TYPE_PATTERN = Pattern.compile("<p>.+\\((\\d+)%\\)\\s*<code>(.+)</code> nodes (.+)");
	private static final String PLACEHOLDER_WORD = "PC";
	private static final Pattern LAST_TOK_PATT = Pattern.compile("\\s+(?=([^\\s]+$))");
	
	//private static final int TOTAL_PROB = 100;
	
	static {
		POS_WORD_MAP = ArrayListMultimap.create();
		wordPosTypeMap = new HashMap<String, PosType>();
		//should create from file
		/*contains pairs of form e.g. apple noun. Note lower case pos.*/
		String lexiconPath = "data/lexicon.txt";
		//lexiconPath = "data/lexiconMedium.txt";
		createLexicon(POS_WORD_MAP, wordPosTypeMap, lexiconPath);
		
		//fill map from data sources
		
		//create data maps posTypePCProbMMap
		Map<PosTypeName, List<Integer>> preMap = new HashMap<PosTypeName, List<Integer>>();
		/*String e.g. <p>2182 (8%) <code>VERB</code> nodes have one child.</p>*/
		String pcProbFileStr = "data/pcProb.txt";
		//create map for how many children a Pos has
		createPCProbMap(pcProbFileStr, preMap);
		System.out.println("pcProbMap created!");
		
		posTypePCProbMap = processSearchablePreMap(preMap);
		System.out.println("posTypePCProbMap created!");
	}
	//given a PosType, 
	
	/**
	 * Used to identify whether supplied Pos is parent or 
	 * child in a Dep.
	 */
	public static enum PosPCType{
		PARENT, CHILD;

		public PosPCType getOtherType() {
			return this == PARENT ? CHILD : PARENT;
		}
		
		public static PosPCType generateRandType() {
			return RAND_GEN.nextBoolean() ? PARENT : CHILD;		
		}
	}
	
	private static void createLexicon(ListMultimap<PosTypeName, String> posWordLexiconMMap,
			Map<String, PosType> wordPosTypeMap, String lexiconPath){
		
		SetMultimap<PosTypeName, String> lexiconSetMMap = HashMultimap.create();
		System.out.println("Creating lexicon...");
		List<String> lines = StoryUtils.readLinesFromFile(lexiconPath);
		Matcher m;
		/*lines are of the form "apple noun", where last token indicates pos. Could
		 * be n-gram.*/
		for(String line : lines) {
			//strip surrounding spaces
			if((m = StoryUtils.SURROUNDING_SPACE_PATT.matcher(line)).matches()) {
				line = m.group(1);
			}
			String[] lineAr = LAST_TOK_PATT.split(line);
			if(lineAr.length < 2) {
				continue;
			}
			PosTypeName posTypeName = PosTypeName.getTypeFromName(lineAr[1].toUpperCase());
			
			if(posTypeName != PosTypeName.NONE) {
				String word = lineAr[0];
				lexiconSetMMap.put(posTypeName, word);	
				
				if(!wordPosTypeMap.containsKey(word)) {
					wordPosTypeMap.put(word, posTypeName.getPosType());
				}
			}else {
				throw new IllegalArgumentException(line + " classified as posTypeName.NONE");
			}
		}
		System.out.println("lexicon file read! lexiconSetMMap.keys().size() "+lexiconSetMMap.keySet().size());
		for(PosTypeName posTypeName : lexiconSetMMap.keySet()) {
			posWordLexiconMMap.putAll(posTypeName, lexiconSetMMap.get(posTypeName));
		}
		System.out.println("lexicon data put to list multimap!");
		/*posWordLexiconMMap.put(PosTypeName.VERB, "fly");
		posWordLexiconMMap.put(PosTypeName.VERB, "have");
		posWordLexiconMMap.put(PosTypeName.AUX, "be");
		posWordLexiconMMap.put(PosTypeName.AUX, "is");
		posWordLexiconMMap.put(PosTypeName.SYM, "!");
		posWordLexiconMMap.put(PosTypeName.SYM, ",");
		
		posWordLexiconMMap.put(PosTypeName.NOUN, "apple");
		posWordLexiconMMap.put(PosTypeName.PRON, "she");
		posWordLexiconMMap.put(PosTypeName.ADV, "quickly");
		
		for(PosTypeName posTypeName : PosTypeName.values()){
			posWordLexiconMMap.put(posTypeName, posTypeName.name());			
		}*/
		
	}
	
	public static ListMultimap<PosTypeName, String> POS_WORD_MAP(){
		return POS_WORD_MAP;
	}
	
	/**
	 * Create Searchable premap with desired ordering
	 * @param preMap
	 * @return
	 */
	private static Map<PosTypeName, SearchableList<Integer>> processSearchablePreMap(Map<PosTypeName, 
			List<Integer>> preMap) {
		
		Map<PosTypeName, SearchableList<Integer>> searchableListMap 
			= new HashMap<PosTypeName, SearchableList<Integer>>();
		
		for(Map.Entry<PosTypeName, List<Integer>> entry : preMap.entrySet()) {
			//System.out.println("Story - entry "+entry.toString());
			List<Integer> probList = entry.getValue();
			for(int i = 1; i < probList.size(); i++) {
				probList.set(i, probList.get(i-1) + probList.get(i));
			}
			searchableListMap.put(entry.getKey(), new SearchableList<Integer>(probList));
		}
		
		return searchableListMap;
	}

	/**
	 * Map of prob for how many children a Pos has.
	 * @param posTypePCProbMMap
	 * @param charset default is UTF-8 if none specified.
	 */
	private static void createPCProbMap(String fileStr, Map<PosTypeName, List<Integer>> posTypePCProbMap,
			Charset... charset) {
		//read data in from file
		Matcher m;
		int prob;
		//e.g. "VERB"
		String posTypeStr;
		String childrenCountStr;
		
		List<String> lines = StoryUtils.readLinesFromFile(fileStr, charset);
		
		for(String line : lines){
			
			/*match String such as "<p>2182 (8%) <code>VERB</code> nodes have one child.</p>"	
			 gives 8 ~~ VERB ~~ have one child.</p> for the different groups. These two lie in 
			 one string*/
			if((m=PC_TYPE_PATTERN.matcher(line)).matches()) {
				
				posTypeStr = m.group(2);
				
				PosTypeName posTypeName = PosTypeName.getTypeFromName(posTypeStr);
				if(posTypeName == PosTypeName.NONE) {
					return      /*******here  ********/;
				}
				prob = Integer.parseInt(m.group(1));
				childrenCountStr = m.group(3);
	
				List<Integer> pcProbList = posTypePCProbMap.get(posTypeName);
				
				if(null == pcProbList) {
					pcProbList = new ArrayList<Integer>();
					for(int i = 0; i < 5; i++) {
						pcProbList.add(0);					
					}
					//initial padding so binary search can return upper index.
					pcProbList.set(0, 0);					
					posTypePCProbMap.put(posTypeName, pcProbList);
				}
				
				if(childrenCountStr.contains("are leaves")) {
					pcProbList.set(1, prob);
				}else if(childrenCountStr.contains("one child")) {
					pcProbList.set(2, prob);
				}else if(childrenCountStr.contains("two children")) {
					pcProbList.set(3, prob);
				}else if(childrenCountStr.contains("three or more children")) {
					pcProbList.set(4, prob);
				}
				
			}		
		}		
	}
	
	public static String getRandomWord(PosType posType) {
		
		PosTypeName posTypeName = posType.posTypeName();
		
		List<String> posTypeWordList = null;
		int randInt = RAND_GEN.nextInt(100);
		
		if(randInt < 85) {
		//if(posTypeName == PosTypeName.NOUN || posTypeName == PosTypeName.VERB) {
			posTypeWordList = ExtraLexicon.commonPosWordListMultimap().get(posTypeName);		
		}
		
		if(null == posTypeWordList || posTypeWordList.isEmpty()){
			posTypeWordList = Story.POS_WORD_MAP().get(posTypeName);
		}
		
		//List<String> posTypeWordList = Story.POS_WORD_MAP().get(posTypeName);
		if(posTypeWordList.isEmpty()) {
			System.out.println("Story - no vocab word for PosType " + posTypeName);
			return PLACEHOLDER_WORD;
		}
		int posTypeWordListSz = posTypeWordList.size();
		String word = posTypeWordList.get(RAND_GEN.nextInt(posTypeWordListSz));
		
		/*int maxIter = 15;
		//prototype slow!!
		while('s' != word.charAt(0)) {
			if(--maxIter < 1) {
				break;
			}
			word = posTypeWordList.get(RAND_GEN.nextInt(posTypeWordListSz));
		}*/
		return word;
	}
	
	public static Map<PosTypeName, SearchableList<Integer>> posTypePCProbMap(){
		return posTypePCProbMap;
	}

	/**
	 * Map of words (current count: 41k) and their part of speech.
	 */
	public static Map<String, PosType> wordPosTypeMap(){
		return wordPosTypeMap;
	}
	
	//create story, connecting input words and prob
	public static void main(String[] args) {
		//guess pos for the input words using pos tagger, 
		
		Scanner sc = new Scanner(System.in);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] lineAr = StoryUtils.WHITE_NON_EMPTY_SPACE_PATT.split(line);
			if(lineAr.length == 0) {
				continue;
			}
			String type = lineAr[0];
			if("quit".equals(type)) {
				break;
			}
			PosType posType = PosTypeName.getTypeFromName(type.toUpperCase()).getPosType();
			if(posType == PosType.NONE) {
				System.out.println("Please enter a valid PosType");
				continue;
			}
			
			//treemap to keep track of scores of various pos.
			TreeMap<Double, Pos> scorePosTMap = new TreeMap<Double, Pos>();
			//List<String> posStringList = new ArrayList<String>();
			double topScore = 0.;
			
			int maxIter = 10;
			while(--maxIter > 0 || scorePosTMap.isEmpty() || topScore < 0.9) {	
				//PosType posType = PosType.VERB;
				//origin of tree, the supplied entry point, *not* root
				Pos originPos = Pos.createSentenceTree(posType);
				double initialScore = ScoreTree.MAX_TREE_SCORE;
				
				if(!Pos.treeContainsVerb(originPos)) {
					System.out.println("~~~~~~~ ++++ NO VERB ++++ ");
					continue;
					//initialScore = .6;					
					//System.out.println("*** Trying again to find a verb!");					
				}
				//arrange tree into a sentence based on 		
				String sentence = Pos.arrangePosStr(originPos);
				double score = ScoreTree.computeTreeScore(originPos, initialScore);
				topScore = score > topScore ? score : topScore;
				
				System.out.println("current sentence: " + sentence);
				System.out.println("score: " + score);
				
				System.out.println(" ~~~~~~~~~~~~~~~~~~~~~~ ");
				scorePosTMap.put(score, originPos);
			}
			/**
			 * if(!Pos.treeContainsVerb(originPos)) {
					if(--maxIter < 0) {
						break;
					}
					System.out.println("*** Trying again to find a verb!");
					originPos = Pos.createSentenceTree(posType);
				}
			 */
			
			Map.Entry<Double, Pos> mapEntry = scorePosTMap.floorEntry(ScoreTree.MAX_TREE_SCORE);
			Pos winningRootPos = mapEntry.getValue();
			String sentence = winningRootPos.subTreePhrase();
			List<PosType> posTypeList = winningRootPos.subTreePosList();
			//e.g. "is verboten divine "
			if(posTypeList.get(0) == PosType.AUX) {
				sentence += "?";
			}
			
			System.out.println("top sentence: " + sentence);
			System.out.println("posTypeList: " + posTypeList);
			System.out.println("score: " + mapEntry.getKey());
			System.out.println(" ~~~~~~~~~~~~~~~~~~~~~~ ");
		}
		sc.close();
	}
	
}
