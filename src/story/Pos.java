package story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ListMultimap;

import story.Dep.DepType;
import story.Pos.PosType;
import story.Story.PosPCType;
import utils.StoryUtils.SearchableList;

/**
 * part of speech.
 * @author yihed
 *
 */
public class Pos {
	
	//<a href="">en-dep/xcomp</a> (2502; 10% instances)
	/**map to lines such as "<a href="">en-pos/VERB</a>-<a href="">en-pos/PROPN</a> (1372; 8% instances)"
	 used for constructing maps for a DepType. These lines are curated data. Keys are names*/
	private static final Map<String, String> parentPosTypeDataMap;
	private static final Map<String, String> childPosTypeDataMap;
	private static final Random RAND_GEN = new Random();
	
	private PosType posType;
	
	//relation to parent for this Pos instance
	private Dep parentDep;
	//relation to children
	private List<Dep> childDepList = new ArrayList<Dep>();
	
	/**distance to originator Pos (not necessarily root)
	 used for determining whether to expand tree further. */
	private int distToOrigin;
	
	static {
		//construct depTypeDataMap by reading data from file
		parentPosTypeDataMap = new HashMap<String, String>();
		childPosTypeDataMap = new HashMap<String, String>();
		
		/////////
	}
	Pos(PosType posType_) {
		this.posType = posType_;		
	}
	
	public void addDep(Dep dep, PosPCType posParentChildType) {
		if(posParentChildType == PosPCType.PARENT) {
			this.parentDep = dep;
		}else {
			this.childDepList.add(dep);
		}		
	}
	
	/**
	 * A label for a Pos, e.g. VERB,
	 * along with probability maps for possible
	 * parent-child relations (Dep's).
	 */
	public static enum PosType{
		VERB(parentPosTypeDataMap.get("verb") , childPosTypeDataMap.get("verb")), 
		NONE("", "");
		
		
		private static Pattern COMMA_SEP_PATTERN = Pattern.compile("\\s*, \\s*");
		//pattern used to extract parent-child relations. 2 groups.
		private static Pattern DEP_PATTERN 
			= Pattern.compile(".+en-dep/(.+)</a>(?:.+);\\s*([\\d]+)% inst.+");
		//relations to parents (e.g. nsubj) and their prob (between 0 and 100)
		private Map<DepType, Integer> parentDepTypeMap;
		private Map<DepType, Integer> childDepTypeMap;
		private List<DepTypeProbPair> parentDepTypePairList;
		private List<DepTypeProbPair> childDepTypePairList;
		
		private int parentTotalProb;
		private int childTotalProb;
		
		private PosType(String parentDataString, String childDataString) {
			parentDepTypeMap = new HashMap<DepType, Integer>();
			childDepTypeMap = new HashMap<DepType, Integer>();		
			parentDepTypePairList = new ArrayList<DepTypeProbPair>();
			childDepTypePairList = new ArrayList<DepTypeProbPair>();
			parentTotalProb = createMap(parentDataString, parentDepTypeMap, parentDepTypePairList);
			childTotalProb = createMap(childDataString, childDepTypeMap, childDepTypePairList);			
		}
		
		/**
		 * DataString e.g. <a href="">en-dep/xcomp</a> (2502; 10% instances)
		 * @param dataString
		 * @param parentDepTypeMap
		 * @param childDepTypeMap
		 */
		private static int createMap(String dataString, Map<DepType, Integer> depTypeMap,
				List<DepTypeProbPair> depTypePairList) {
			
			if(null == dataString) {
				throw new IllegalArgumentException("data string for posType cannot be null.");
			}			
			String[] dataAr = COMMA_SEP_PATTERN.split(dataString);			
			Matcher m;
			String depTypeStr;
			int prob;
			DepType depType;
			int totalProb = 0;
			
			for(String s : dataAr) {
				if((m=DEP_PATTERN.matcher(s)).matches()) {
					depTypeStr = m.group(1);
					
					if((depType = Dep.DepType.getTypeFromName(depTypeStr)) != DepType.NONE) {
						prob = Integer.parseInt(m.group(2));
						//some data have 0 prob because low occurrence.
						prob = prob > 0 ? prob : 1;
						totalProb += prob;
						depTypeMap.put(depType, totalProb);
						depTypePairList.add(new DepTypeProbPair(depType, totalProb));
					}					
				}
			}			
			return totalProb;
		}
		
		public static PosType getTypeFromName(String posTypeName) {
			switch(posTypeName) {
			case "VERB":
				return VERB;
			default:
				//better default!?
				return NONE;
			}
		}
				
		/**
		 * Obtain a target DepType based on prob maps for given posType, get either parent or child
		 * type.
		 * @param posType
		 * @param posParentChildType Whether posType should be taken as parent or child.
		 * @return
		 */
		public static List<DepType> selectRandomDepType(PosType posType, PosPCType posParentChildType) {
			
			int totalProb = posParentChildType == PosPCType.PARENT ? posType.childTotalProb : posType.parentTotalProb;
			//get the range over all possible pos value Map<DepType, Integer> parentDepTypeMap
			List<DepTypeProbPair> depTypeList = posParentChildType == PosPCType.PARENT ? posType.parentDepTypePairList
					: posType.childDepTypePairList;
			
			List<DepType> dTList = new ArrayList<DepType>();
			
			
			int numDepType;
			if(posParentChildType == PosPCType.CHILD) {
				numDepType = 1;
			}else {
				int randInt = RAND_GEN.nextInt(100)+1;
				//generate based on stats
				SearchableList<Integer> pcProbList = Story.posTypePCProbMap().get(posType);
				int index = pcProbList.listBinarySearch(randInt);
				numDepType = pcProbList.getTargetElem(index);
			}
			
			for(int i = 0; i < numDepType; i++) {
				int randInt = RAND_GEN.nextInt(totalProb)+1;		
				//use binary search to find the right interval,
				//map already sorted according to 
				
				int targetIndex = selectRandomDepTypeSearch(randInt, 0, depTypeList.size()-1, depTypeList);
				dTList.add(depTypeList.get(targetIndex).depType);
			}
			
			return dTList;
			
		}
		
		public static int selectRandomDepTypeSearch(int targetProb, int lowerIndex, int upperIndex, 
				List<DepTypeProbPair> depTypePairList) {
			
			if(lowerIndex + 1 == upperIndex){
				return upperIndex;
			}
			int midIndex = (lowerIndex + upperIndex)/2;
			int midIndexProb = depTypePairList.get(midIndex).prob;
			if(targetProb > midIndexProb){
				return selectRandomDepTypeSearch(targetProb, midIndex, upperIndex, depTypePairList);
			}else if(targetProb < midIndexProb){
				return selectRandomDepTypeSearch(targetProb, lowerIndex, midIndex, depTypePairList);
			}else{
				//since prob starts at 0, so 50% prob occupy 0 through 49 (say).
				return upperIndex;
			}
			
		}
		
	}/*End of PosType enum*/
	
	public static class DepTypeProbPair {
		DepType depType; 
		int prob;
		
		public DepTypeProbPair(DepType depType_, int prob_) {
			this.depType = depType_;
			this.prob = prob_;
		}
	}/**/
	
	
	/**
	 * create sentence tree given a PosType
	 * @param posType
	 */
	public static void createSentenceTree(PosType posType) {

		//create a pos with that Type
		Pos pos = new Pos(posType);
		
		growTree(pos);		
		
	}

	private static void growTree(Pos pos) {
		
		PosType posType = pos.posType;
		//not mutually exclusive!
		PosPCType parentChildType = PosPCType.generateRandType();
		//use prob to determine if get parent.
		if(null == pos.parentDep && get_parent ) {
			//create Dep with randomly generated DepType
			List<DepType> depTypeList = PosType.selectRandomDepType(posType, PosPCType.CHILD);
			
			if(!depTypeList.isEmpty()) {
				
				DepType depType = depTypeList.get(0);
				//this is for child
				PosType matchingPosType = depType.selectRandomMatchingPos(posType, PosPCType.CHILD);		
				//create Dep from DepType
				//Pos parentPos_, Pos childPos_, DepType depType_
				Pos childPos;
				Pos parentPos;
				/*if(parentChildType == PosPCType.PARENT) {
					parentPos = pos;
					childPos = new Pos(matchingPosType);
					childPos.distToOrigin = 1;
					*/
				///*else {
					parentPos = new Pos(matchingPosType);			
					childPos = pos;	
					parentPos.distToOrigin = 1;
				//}	*/
				
				Dep dep = new Dep(depType, parentPos, childPos);
				//don't always add!!! Could repeat
				childPos.addDep(dep, PosPCType.CHILD);
				parentPos.addDep(dep, PosPCType.PARENT);
				
				//grow children
				growTree(parentPos);
			}
			
		}
		
		//get_child takes into account e.g. how far from Pos originator. how many children already, etc
		if( get_child ) {
			//create Dep with randomly generated DepType
			List<DepType> depTypeList = PosType.selectRandomDepType(posType, PosPCType.PARENT);
			
			//get avg number of children nodes!!
			
			for(DepType depType : depTypeList) {
				//this is for child
				PosType matchingPosType = depType.selectRandomMatchingPos(posType, PosPCType.PARENT);		
				//create Dep from DepType
				//Pos parentPos_, Pos childPos_, DepType depType_
				Pos childPos;
				Pos parentPos;
				//if(parentChildType == PosPCType.PARENT) {
					parentPos = pos;
					childPos = new Pos(matchingPosType);
					childPos.distToOrigin = 1;
				/*}/*else {
					parentPos = new Pos(matchingPosType);			
					childPos = pos;	
					parentPos.distToOrigin = 1;
				}	*/
				
				Dep dep = new Dep(depType, parentPos, childPos);
				childPos.addDep(dep, PosPCType.CHILD);
				parentPos.addDep(dep, PosPCType.PARENT);
				
				//grow children
				growTree(childPos);
				
			}
		}
	}
	
	
}
