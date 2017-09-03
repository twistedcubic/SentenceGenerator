package story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import story.Pos.PosType;
import utils.StoryUtils.SearchableList;

/**
 * Class for generating stories
 * 
 */
public class Story {

	/**
	 * Map of pos and words. E.g. on entry for "NOUN" is "apple"
	 */
	private static final Map<PosType, String> POS_WORD_MAP;
	private static final Random RAND_GEN = new Random();
	//e.g. <p>1084 (4%) <code>VERB</code> nodes are leaves.</p>
		//data on the nodes parent child type stats. Generate number of
		//parent children stats.
	private static final Map<PosType, SearchableList<Integer>> posTypePCProbMap; 
	private static final Pattern PC_TYPE_PATTERN = Pattern.compile("<p>.+\\((\\d+)%\\)\\s*<code>(.+)</code> nodes (.+)");
	
	static {
		POS_WORD_MAP = new HashMap<PosType, String>();
		
		//fill map from data sources
		
		//create posTypePCProbMMap
		Map<PosType, List<Integer>> preMap = new HashMap<PosType, List<Integer>>();
		createPCProbMap("", preMap);
		
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
	
	public static void createTree(PosType posType) {
		
	}
	
	private static Map<PosType, SearchableList<Integer>> processSearchablePreMap(Map<PosType, List<Integer>> preMap) {
	
		Map<PosType, SearchableList<Integer>> searchableListMap = new HashMap<PosType, SearchableList<Integer>>();
		
		for(Map.Entry<PosType, List<Integer>> entry : preMap.entrySet()) {
			List<Integer> probList = entry.getValue();
			for(int i = 1; i < probList.size(); i++) {
				probList.set(i, probList.get(i-1) + probList.get(i));
			}
			searchableListMap.put(entry.getKey(), new SearchableList<Integer>(probList));
		}
		
		return searchableListMap;
	}

	/**
	 * 
	 * @param posTypePCProbMMap
	 */
	private static void createPCProbMap(String s  , Map<PosType, List<Integer>> posTypePCProbMap) {
		//read data in from file
		Matcher m;
		int prob;
		//e.g. "VERB"
		String posTypeStr;
		String childrenCountStr;
		
		//match String such as "<p>2182 (8%) <code>VERB</code> nodes have one child.</p>"	
		//gives 8 ~~ VERB ~~ have one child.</p> for the different groups.
		if((m=PC_TYPE_PATTERN.matcher(s)).matches()) {
			
			posTypeStr = m.group(2);
			
			PosType posType = PosType.getTypeFromName(posTypeStr);
			if(posType == PosType.NONE) {
				return      /***************/;
			}
			prob = Integer.parseInt(m.group(1));
			childrenCountStr = m.group(3);

			List<Integer> pcProbList = posTypePCProbMap.get(posType);
			
			if(null == pcProbList) {
				pcProbList = new ArrayList<Integer>();
				for(int i = 0; i < 4; i++) {
					pcProbList.add(0);					
				}
				posTypePCProbMap.put(posType, pcProbList);
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
	
	public static Map<PosType, SearchableList<Integer>> posTypePCProbMap(){
		return posTypePCProbMap;
	}

	//create story, connecting input words and prob
	public static void main(String[] args) {
		//guess pos for the input words
		
		
		
	}
	
}
