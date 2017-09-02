package story;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import story.Pos.PosType;
import story.Story.PosParentChildType;

/**
 * Possible parent-child relation between parent
 * and child.
 * Convention: pos names are all upper case, e.g. VERB. Dep names
 * all lower, e.g. nsubj.
 * @author yihed
 *
 */
public class Dep {

	/**map to lines such as "<a href="">en-pos/VERB</a>-<a href="">en-pos/PROPN</a> (1372; 8% instances)"
	 used for constructing maps for a DepType. These lines are curated data. Keys are names*/
	private static final Map<String, String> depTypeDataMap;
	private static final Random RAND_GEN = new Random();
	
	//dependencies
	
	//type of relation for this instance.
	private DepType depType;
	//whether parent or child 
	private Pos parentPos;
	private Pos childPos;
	
	static {
		//construct depTypeDataMap by reading data from file
		depTypeDataMap = new HashMap<String, String>();
		/////////
		
	}
	
	public Dep(Pos parentPos_, Pos childPos_, DepType depType_) {
		this.parentPos = parentPos_;
		this.childPos = childPos_;
		this.depType = depType_;
	}
	
	/**
	 * A label for a relation, e.g. NSUBJ,
	 * along with probability maps for possible
	 * parent-child pairs.
	 */
	public static enum DepType{
		
		NSUBJ(depTypeDataMap.get("nsubj"), 2, 1),
		NONE("", 0, 0);
		
		//<a href="">en-pos/VERB</a>-<a href="">en-pos/PROPN</a> (1372; 8% instances).
		private static Pattern COMMA_SEP_PATTERN = Pattern.compile("\\s*, \\s*");
		//pattern used to extract parent-child relations. 3 groups.
		private static Pattern DEP_PATTERN 
			= Pattern.compile(".+en-pos/(.+)</a>(?:.+)pos/(.+)</a>(?:.+);\\s*([\\d]+)% inst.+");
		
		//probability map for parent-child relations
		//where parent pos are keys.
		private ListMultimap<PosType, PosProbPair> parentChildMMap;
		//probability map for parent-child relations
		//where child pos are keys.
		private ListMultimap<PosType, PosProbPair> childParentMMap;
		private Map<PosType, Integer> parentChildTotalProbMap;
		//probability map for parent-child relations
		//where child pos are keys.
		private Map<PosType, Integer> childParentTotalProbMap;
		
		//avg dist between parent and child 
		private double parentChildDist;
		//probability for left-right ordering, int between 0 and 100.
		//left-right means parent preceeds child, right-left is otherway.
		private int leftRightProb;
		
		private DepType(String dataString,
				double parentChildDist_, int leftRightProb_) {
			
			if(null == dataString) {
				throw new IllegalArgumentException("data string for DepType cannot be null.");
			}
			parentChildMMap = ArrayListMultimap.create();
			childParentMMap = ArrayListMultimap.create();
			
			parentChildTotalProbMap = new HashMap<PosType, Integer>();
			childParentTotalProbMap = new HashMap<PosType, Integer>();;
			createDepMMaps(dataString, parentChildMMap, childParentMMap,
					parentChildTotalProbMap, childParentTotalProbMap);
			
			this.parentChildDist = parentChildDist_;
			this.leftRightProb = leftRightProb_;
		}
		
		public static DepType getTypeFromName(String depTypeName) {
			switch(depTypeName) {
			case "nsubj":
				return NSUBJ;
			default:
				//better default!?
				return NONE;
			}
		}
		
		/**
		 * Parse dataString to create maps. Data such as
		 * <a href="">en-pos/VERB</a>-<a href="">en-pos/PROPN</a> (1372; 8% instances).
		 * @param dataString
		 * @param parentChildMMap
		 * @param childParentMMap
		 */
		private void createDepMMaps(String dataString, Multimap<PosType, PosProbPair> parentChildMMap, 
				Multimap<PosType, PosProbPair> childParentMMap, Map<PosType, Integer> parentChildTotalProbMap, 
				Map<PosType, Integer> childParentTotalProbMap) {
			//separate by comma 
			String[] dataStringAr = COMMA_SEP_PATTERN.split(dataString);
			String parent;
			String child;
			int prob;
			Matcher m;
			
			for(String s : dataStringAr) {
				if( (m = DEP_PATTERN.matcher(s)).matches() ) {
					parent = m.group(1);
					child = m.group(2);
					
					PosType parentType;
					PosType childType;
					if((parentType=Pos.PosType.getTypeFromName(parent)) != PosType.NONE 
							&& (childType=Pos.PosType.getTypeFromName(child)) != PosType.NONE) {
						
						prob = Integer.parseInt(m.group(3));
						
						int parentTotalSoFar;
						int childTotalSoFar;
						Integer parentTotal = parentChildTotalProbMap.get(parentType);
						if(null != parentTotal) {
							parentTotalSoFar = parentTotal + prob;							
						}else {
							parentTotalSoFar = prob;
						}
						parentChildTotalProbMap.put(parentType, parentTotalSoFar);
						
						Integer childTotal = childParentTotalProbMap.get(childType);
						if(null != childTotal) {
							childTotalSoFar = parentTotal+prob;
						}else {
							childTotalSoFar = prob;							
						}
						childParentTotalProbMap.put(childType, childTotalSoFar);
						
						//the prob in input dataStrings are already sorted.
						parentChildMMap.put(parentType, new PosProbPair(childType, childTotalSoFar));
						childParentMMap.put(childType, new PosProbPair(parentType, parentTotalSoFar));						
					}
					
				}
			}
			
		}
		
		/**
		 * Obtain a target PosType based on prob maps for given posType.
		 * @param posType
		 * @param posParentChildType
		 * @return
		 */
		public PosType selectRandomMatchingPos(PosType posType, PosParentChildType posParentChildType) {
			//get the range over all possible pos value 
			ListMultimap<PosType, PosProbPair> mMap 
				= posParentChildType == PosParentChildType.PARENT ? parentChildMMap : childParentMMap;
			Map<PosType, Integer> totalProbMap 
				= posParentChildType == PosParentChildType.PARENT ? parentChildTotalProbMap : childParentTotalProbMap;
			
			List<PosProbPair> posProbPairList = mMap.get(posType);	
			if(posProbPairList.isEmpty()){
				return PosType.NONE;
			}
			
			int totalProb = totalProbMap.get(posType);
			
			int randInt = RAND_GEN.nextInt(totalProb);
			
			//use binary search to find the right interval,
			//map already sorted according to 
			
			int targetIndex = selectRandomMatchingPosSearch(randInt, 0, posProbPairList.size()-1, posProbPairList);
			return posProbPairList.get(targetIndex).posType;
			
		}
		
		private int selectRandomMatchingPosSearch(int targetProb, int lowerIndex, int upperIndex,
				List<PosProbPair> posProbPairList){
			
			if(lowerIndex + 1 == upperIndex){
				return upperIndex;
			}
			int midIndex = (lowerIndex + upperIndex)/2;
			int midIndexProb = posProbPairList.get(midIndex).prob;
			if(targetProb > midIndexProb){
				return selectRandomMatchingPosSearch(targetProb, midIndex, upperIndex, posProbPairList);
			}else if(targetProb < midIndexProb){
				return selectRandomMatchingPosSearch(targetProb, lowerIndex, midIndex, posProbPairList);
			}else{
				//since prob starts at 0, so 50% prob occupy 0 through 49 (say).
				return upperIndex;
			}
		}		
		
	}/*end of DepType enum*/
	
	
	/**
	 * Pos and probability pair, used as value in 
	 * childParentMap or parentChildMap.
	 */
	private static class PosProbPair{
		//the pos in the value of the map, could be parent
		//or child.
		PosType posType;
		//probability as a number between 0 and 100.
		int prob;
		
		PosProbPair(PosType posType_, int prob_){
			this.posType = posType_;
			this.prob = prob_;
		}
		
	}
	
	
}
