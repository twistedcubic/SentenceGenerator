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
import story.Pos.PosType.PosTypeName;
import story.Story.PosPCType;

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
	
	public Dep(DepType depType_, Pos pos_, PosPCType parentChildType) {
		
		if(parentChildType == PosPCType.PARENT) {
			this.parentPos = pos_;
		}else {
			this.childPos = pos_;
			this.depType = depType_;			
		}
		this.depType = depType_;	
	}
	
	public Dep(DepType depType_, Pos parentPos_, Pos childPos_) {
		
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
		ROOT(depTypeDataMap.get("root"), 2, 1),
		NONE("", 0, 0);
		
		//18641 instances of case (96%) are right-to-left (child precedes parent). 
		//Average distance between parent and child is 2.0255896408201.
		//used to determine ordering and relation.
		
		
		//<a href="">en-pos/VERB</a>-<a href="">en-pos/PROPN</a> (1372; 8% instances).
		private static Pattern COMMA_SEP_PATTERN = Pattern.compile("\\s*, \\s*");
		//pattern used to extract parent-child relations. 3 groups.
		private static Pattern DEP_PATTERN 
			= Pattern.compile(".+en-pos/(.+)</a>(?:.+)pos/(.+)</a>(?:.+);\\s*([\\d]+)% inst.+");
		
		//probability map for parent-child relations
		//where parent pos are keys.
		private ListMultimap<PosTypeName, PosProbPair> parentChildMMap;
		//probability map for parent-child relations
		//where child pos are keys.
		private ListMultimap<PosTypeName, PosProbPair> childParentMMap;
		private Map<PosTypeName, Integer> parentChildTotalProbMap;
		//probability map for parent-child relations
		//where child pos are keys.
		private Map<PosTypeName, Integer> childParentTotalProbMap;
		
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
			
			parentChildTotalProbMap = new HashMap<PosTypeName, Integer>();
			childParentTotalProbMap = new HashMap<PosTypeName, Integer>();;
			createDepMMaps(dataString, parentChildMMap, childParentMMap,
					parentChildTotalProbMap, childParentTotalProbMap);
			
			this.parentChildDist = parentChildDist_;
			this.leftRightProb = leftRightProb_;
		}
		
		public static DepType getTypeFromName(String depTypeName) {
			switch(depTypeName) {
			case "nsubj":
				return NSUBJ;
			case "root":
				return ROOT;
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
		private void createDepMMaps(String dataString, Multimap<PosTypeName, PosProbPair> parentChildMMap, 
				Multimap<PosTypeName, PosProbPair> childParentMMap, Map<PosTypeName, Integer> parentChildTotalProbMap, 
				Map<PosTypeName, Integer> childParentTotalProbMap) {
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
					
					PosTypeName parentTypeName;
					PosTypeName childTypeName;
					if((parentTypeName=PosTypeName.getTypeFromName(parent)) != PosTypeName.NONE 
							&& (childTypeName=PosTypeName.getTypeFromName(child)) != PosTypeName.NONE) {
						
						prob = Integer.parseInt(m.group(3));
						
						int parentTotalSoFar;
						int childTotalSoFar;
						Integer parentTotal = parentChildTotalProbMap.get(parentTypeName);
						if(null != parentTotal) {
							parentTotalSoFar = parentTotal + prob;							
						}else {
							parentTotalSoFar = prob;
						}
						parentChildTotalProbMap.put(parentTypeName, parentTotalSoFar);
						
						Integer childTotal = childParentTotalProbMap.get(childTypeName);
						if(null != childTotal) {
							childTotalSoFar = parentTotal+prob;
						}else {
							childTotalSoFar = prob;							
						}
						childParentTotalProbMap.put(childTypeName, childTotalSoFar);
						
						//the prob in input dataStrings are already sorted.
						parentChildMMap.put(parentTypeName, new PosProbPair(childTypeName, childTotalSoFar));
						childParentMMap.put(childTypeName, new PosProbPair(parentTypeName, parentTotalSoFar));						
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
		public PosType selectRandomMatchingPos(PosType posType, PosPCType posParentChildType) {
			
			//get the range over all possible pos value 
			ListMultimap<PosTypeName, PosProbPair> mMap 
				= posParentChildType == PosPCType.PARENT ? parentChildMMap : childParentMMap;
			Map<PosTypeName, Integer> totalProbMap 
				= posParentChildType == PosPCType.PARENT ? parentChildTotalProbMap : childParentTotalProbMap;
			
			List<PosProbPair> posProbPairList = mMap.get(posType.posTypeName());
			if(posProbPairList.isEmpty()){
				return PosType.NONE;
			}
			
			int totalProb = totalProbMap.get(posType.posTypeName());
			//+1 since nextInt excludes last number. make into constant.
			int randInt = RAND_GEN.nextInt(totalProb)+1;
			
			//use binary search to find the right interval,
			//map already sorted according to 
			
			int targetIndex = selectRandomMatchingPosSearch(randInt, 0, posProbPairList.size()-1, posProbPairList);
			return posProbPairList.get(targetIndex).posTypeName.getPosType();
			
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
	
	public Pos parentPos() {
		return parentPos;
	}

	public Pos childPos() {
		return childPos;
	}

	/**
	 * Pos and probability pair, used as value in 
	 * childParentMap or parentChildMap.
	 */
	private static class PosProbPair{
		//the pos in the value of the map, could be parent
		//or child.
		PosTypeName posTypeName;
		//probability as a number between 0 and 100.
		int prob;
		
		PosProbPair(PosTypeName posTypeN_, int prob_){
			this.posTypeName = posTypeN_;
			this.prob = prob_;
		}
		
	}
	
	
}
