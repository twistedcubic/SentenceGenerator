package story;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;

import story.Pos.PosType;
import story.Pos.PosType.PosTypeName;
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
	private static final Random RAND_GEN = new Random();
	//e.g. <p>1084 (4%) <code>VERB</code> nodes are leaves.</p>
		//data on the nodes parent child type stats. Generate number of
		//parent children stats.
	private static final Map<PosTypeName, SearchableList<Integer>> posTypePCProbMap; 
	private static final Pattern PC_TYPE_PATTERN = Pattern.compile("<p>.+\\((\\d+)%\\)\\s*<code>(.+)</code> nodes (.+)");
	private static final String PLACEHOLDER_WORD = "PC";
	private static final Pattern LAST_TOK_PATT = Pattern.compile("\\s+(?=([^\\s]+$))");
	
	private static final int TOTAL_PROB = 100;
	
	static {
		POS_WORD_MAP = ArrayListMultimap.create();
		//should create from file
		/*contains pairs of form e.g. apple noun. Note lower case pos.*/
		String lexiconPath = "data/lexicon.txt";
		createLexicon(POS_WORD_MAP, lexiconPath);
		
		//fill map from data sources
		
		//create data maps posTypePCProbMMap
		Map<PosTypeName, List<Integer>> preMap = new HashMap<PosTypeName, List<Integer>>();
		/*String e.g. <p>2182 (8%) <code>VERB</code> nodes have one child.</p>*/
		String pcProbFileStr = "data/pcProb.txt";
		//create map for how many children a Pos has
		createPCProbMap(pcProbFileStr, preMap);
		
		posTypePCProbMap = processSearchablePreMap(preMap);
		
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
			String lexiconPath){
		
		SetMultimap<PosTypeName, String> lexiconSetMMap = HashMultimap.create();
		
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
				lexiconSetMMap.put(posTypeName, lineAr[0]);				
			}else {
				throw new IllegalArgumentException(line + " classified as posTypeName.NONE");
			}
		}
		for(PosTypeName posTypeName : lexiconSetMMap.keys()) {
			posWordLexiconMMap.putAll(posTypeName, lexiconSetMMap.get(posTypeName));
		}
		
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
					for(int i = 0; i < 4; i++) {
						pcProbList.add(0);					
					}
					posTypePCProbMap.put(posTypeName, pcProbList);
				}
				
				if(childrenCountStr.contains("are leaves")) {
					pcProbList.set(0, prob);
				}else if(childrenCountStr.contains("one child")) {
					pcProbList.set(1, prob);
				}else if(childrenCountStr.contains("two children")) {
					pcProbList.set(2, prob);
				}else if(childrenCountStr.contains("three or more children")) {
					pcProbList.set(3, prob);
				}
				
			}		
		}
		
	}
	
	public static String getRandomWord(PosType posType) {
		
		List<String> posTypeWordList = Story.POS_WORD_MAP().get(posType.posTypeName());
		if(null == posTypeWordList) {
			return PLACEHOLDER_WORD;
		}
		return posTypeWordList.get(RAND_GEN.nextInt(posTypeWordList.size()));		
	}
	
	public static Map<PosTypeName, SearchableList<Integer>> posTypePCProbMap(){
		return posTypePCProbMap;
	}

	//create story, connecting input words and prob
	public static void main(String[] args) {
		//guess pos for the input words using pos tagger, 
		
		PosType posType = PosType.NOUN;
		//origin of tree, the supplied entry point, *not* root
		Pos originPos = Pos.createSentenceTree(posType);
		//arrange tree into a sentence based on 		
		String sentence = Pos.arrangePosStr(originPos);
				
		System.out.println("sentence: " + sentence);
		
	}
	
}
