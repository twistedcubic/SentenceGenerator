package story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ListMultimap;

import story.Dep.DepType;
import story.Pos.PosType;
import story.Pos.PosType.PosTypeName;
import story.Story.PosPCType;
import utils.StoryUtils;
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
	private static final int TOTAL_PROB = 100;
	private static Pattern COMMA_SEP_PATTERN = Pattern.compile("\\s*, \\s*");
	//pattern used to extract parent-child relations. 2 groups.
	private static Pattern DEP_PATTERN 
		= Pattern.compile(".+en-dep/(.+)</a>(?:.+);\\s*([\\d]+)% inst.+");
	//<p><code>VERB</code> nodes are attached to their parents using 32 different relations:
	//4 groups
	private static final Pattern DEP_INTRO_PATTERN = Pattern.compile("(.+)<code>(.+)</code> nodes are attached (.+):(.+)");
	
	private static final Map<PosTypeName, List<DepTypeProbPair>> parentDepTypePairListMap;
	private static final Map<PosTypeName, List<DepTypeProbPair>> childDepTypePairListMap;	
	private static final Map<PosTypeName, Integer> rootProbMap;
	
	private PosType posType;
	
	//relation to parent for this Pos instance
	private Dep parentDep;
	/**relation to children*/
	private List<Dep> childDepList = new ArrayList<Dep>();
	
	/**distance to originator Pos (not necessarily root)
	 used for determining whether to expand tree further. */
	private int distToOrigin;
	/**the word selected for this pos. E.g. "I" for PRON*/
	private String posWord;
	
	/**phrase created from subtree*/
	private String subTreePhrase;
	
	static {
		//construct depTypeDataMap by reading data from file
		parentPosTypeDataMap = new HashMap<String, String>();
		childPosTypeDataMap = new HashMap<String, String>();
		
		parentDepTypePairListMap = new HashMap<PosTypeName, List<DepTypeProbPair>>();
		//create map		
		childDepTypePairListMap = new HashMap<PosTypeName, List<DepTypeProbPair>>();
		rootProbMap = new HashMap<PosTypeName, Integer>();
		
		String fileStr = "data/posStats.txt";
		createPosStatsMap(fileStr, parentDepTypePairListMap, childDepTypePairListMap, rootProbMap);
		
		//check parent child formts are same! <-- yep
		
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
		ADJ(PosTypeName.ADJ),
		ADP(PosTypeName.ADP),
		ADV(PosTypeName.ADV),
		AUX(PosTypeName.AUX),
		CONJ(PosTypeName.CONJ),
		DET(PosTypeName.DET),
		INTJ(PosTypeName.INTJ),			
		NOUN(PosTypeName.NOUN),
		NUM(PosTypeName.NUM),
		PART(PosTypeName.PART),
		PRON(PosTypeName.PRON),
		PROPN(PosTypeName.PROPN),
		PUNCT(PosTypeName.PUNCT),
		SCONJ(PosTypeName.SCONJ),
		SYM(PosTypeName.SYM),
		X(PosTypeName.X),
		VERB(PosTypeName.VERB), 		
		NONE(PosTypeName.NONE);
		
		/**
		 * Name strings for this PosType.
		 */
		public static enum PosTypeName{
			ADJ("ADJ"),
			ADP("ADP"),
			ADV("ADV"),
			AUX("AUX"),
			CONJ("CONJ"),
			DET("DET"),
			INTJ("INTJ"),			
			VERB("VERB"),
			NOUN("NOUN"),
			NUM("NUM"),
			PART("PART"),
			PRON("PRON"),
			PROPN("PROPN"),
			PUNCT("PUNCT"),
			SCONJ("SCONJ"),
			SYM("SYM"),
			X("X"),
			NONE("");
			
			private String nameStr;
			
			private PosTypeName(String name) {
				this.nameStr = name;
			}

			public PosType getPosType() {
				/*switch(this) {
				case VERB:
					return PosType.VERB;
				default:
					//better default!?
					return PosType.NONE;
				}*/
				return PosType.valueOf(this.name());
			}
			
			public static PosTypeName getTypeFromName(String posTypeName) {
				try{
					return PosTypeName.valueOf(posTypeName);
				}catch(IllegalArgumentException e){
					return PosTypeName.NONE;
				}
				
				/*switch(posTypeName) {
				case "VERB":
					return VERB;
				default:
					//better default!?
					return NONE;
				}*/
			}
		}/*end of PosTypeName enum*/
		
		
		//relations to parents (e.g. nsubj) and their prob (between 0 and 100)
		//private Map<DepType, Integer> parentDepTypeMap;
		//private Map<DepType, Integer> childDepTypeMap;
		private List<DepTypeProbPair> parentDepTypePairList;
		private List<DepTypeProbPair> childDepTypePairList;
		
		private int parentTotalProb;
		private int childTotalProb;
		/** probability (as percentage) for this pos being root, between 0 and 100.*/
		private int isRootProb;
		
		private PosType(PosTypeName posTypeName) {
			//parentDepTypeMap = new HashMap<DepType, Integer>();
			//childDepTypeMap = new HashMap<DepType, Integer>();		
			//parentDepTypePairList = new ArrayList<DepTypeProbPair>();
			//childDepTypePairList = new ArrayList<DepTypeProbPair>();
			
			parentDepTypePairList = parentDepTypePairListMap.get(posTypeName);
			childDepTypePairList = childDepTypePairListMap.get(posTypeName);
			isRootProb = rootProbMap.get(posTypeName);
		}
		
		
		/**
		 * Obtain a target DepType based on prob maps for given posType, get either parent or child
		 * type.
		 * @param posType
		 * @param posParentChildType Whether posType should be taken as parent or child.
		 * @return
		 */
		public static List<DepType> selectRandomDepType(Pos pos, PosPCType posParentChildType) {
			
			PosType posType = pos.posType;
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
				SearchableList<Integer> pcProbList = Story.posTypePCProbMap().get(posType.posTypeName());
				int index = pcProbList.listBinarySearch(randInt);
				numDepType = pcProbList.getTargetElem(index);
				//count number of existing children
				numDepType = numDepType - pos.childDepList.size();
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
		
		public PosTypeName posTypeName() {
			/*switch(this) {
			case VERB:
				return PosTypeName.VERB;
			default:
				return PosTypeName.NONE;
			}*/
			return PosTypeName.getTypeFromName(this.name());
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
	 * Creates map for statistics for all pos.
	 * DataString e.g. <a href="">en-dep/xcomp</a> (2502; 10% instances)
	 * @param dataString
	 * @param parentDepTypeMapList maps for relations to parent and children
	 * @param childDepTypeMapList
	 */
	private static void createPosStatsMap(String fileStr, Map<PosTypeName, List<DepTypeProbPair>> parentDepTypeMapList,
			Map<PosTypeName, List<DepTypeProbPair>> childDepTypeMapList,
			Map<PosTypeName, Integer> rootProbMap) {
		
		if(null == fileStr) {
			throw new IllegalArgumentException("fileStr for posType cannot be null.");
		}			
		List<String> lines = StoryUtils.readLinesFromFile(fileStr);
		
		Matcher introMatcher;
		for(String line : lines){
			//DEP_INTRO_PATTERN has 4 groups
			if((introMatcher=DEP_INTRO_PATTERN.matcher(line)).matches()){
				//e.g. VERB
				String posNameStr = introMatcher.group(2);
				Map<PosTypeName, List<DepTypeProbPair>> chosenMap;
				if(introMatcher.group(3).contains("parents")){
					chosenMap = parentDepTypeMapList;
				}else{
					chosenMap = childDepTypeMapList;
				}
				
				List<DepTypeProbPair> probPairList = new ArrayList<DepTypeProbPair>();
				
				String dataStr = introMatcher.group(4);
				String[] dataAr = COMMA_SEP_PATTERN.split(dataStr);			
				Matcher m;
				String depTypeStr;
				int prob;
				DepType depType;
				int totalProb = 0;
				
				for(String s : dataAr) {
					if((m=DEP_PATTERN.matcher(s)).matches()) {
						//e.g. "nsubj", or "root"
						depTypeStr = m.group(1);
						
						if((depType = Dep.DepType.getTypeFromName(depTypeStr)) != DepType.NONE) {
							prob = Integer.parseInt(m.group(2));
						
							if("root".equals(depTypeStr)) {
								rootProbMap.put(PosTypeName.getTypeFromName(posNameStr), prob);
								//this.isRootProb = prob;
							}
							//some data have 0 prob because low occurrence.
							prob = prob > 0 ? prob : 1;
							totalProb += prob;
							//depTypeMap.put(depType, totalProb);
							probPairList.add(new DepTypeProbPair(depType, totalProb));							
						}
					}
				}
				chosenMap.put(PosTypeName.getTypeFromName(posNameStr), probPairList);
			}
		}
		
		//return totalProb;
	}	
	
	/**
	 * create sentence tree given a PosType
	 * @param posType
	 */
	public static Pos createSentenceTree(PosType posType) {

		//create a pos with that Type
		Pos pos = new Pos(posType);		
		growTree(pos);
		return pos;
	}

	/**
	 * Attach additional Dep and Pos to given Pos.
	 * @param pos
	 */
	private static void growTree(Pos pos) {
		
		PosType posType = pos.posType;
		pos.posWord = Story.getRandomWord(posType);
		//not mutually exclusive!
		//PosPCType parentChildType = PosPCType.generateRandType();
		
		boolean getParentBool = whetherCreateParent(pos);
		//use prob to determine if get parent.
		if(getParentBool) {
			//create Dep with randomly generated DepType
			List<DepType> depTypeList = PosType.selectRandomDepType(pos, PosPCType.CHILD);
			
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
					parentPos.distToOrigin = pos.distToOrigin + 1;
					parentPos.posWord = Story.getRandomWord(matchingPosType);
				//}	*/
				
				Dep dep = new Dep(depType, parentPos, childPos);
				//don't always add!!! Could repeat
				childPos.addDep(dep, PosPCType.CHILD);
				parentPos.addDep(dep, PosPCType.PARENT);
				
				//grow children
				growTree(parentPos);
			}			
		}
		
		boolean getChildBool = whetherCreateChild(pos);
		//get_child takes into account e.g. how far from Pos originator. how many children already, etc
		if(getChildBool) {
			//create Dep with randomly generated DepType
			List<DepType> depTypeList = PosType.selectRandomDepType(pos, PosPCType.PARENT);
			
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
					childPos.distToOrigin = pos.distToOrigin + 1;
					
					childPos.posWord = Story.getRandomWord(matchingPosType);
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

	/**
	 * Determines if create parent based on pos being root, 
	 * and dist from originator.
	 * @param pos
	 * @return
	 */
	private static boolean whetherCreateParent(Pos pos) {
		
		if(null != pos.parentDep) {
			return false;
		}
		//threshold dist to origin
		final int PARENT_DIST_THRESHOLD = 2;
		if(pos.distToOrigin >= PARENT_DIST_THRESHOLD) {
			return false;
		}
		
		int rootProb = pos.posType.isRootProb;
		int randInt = RAND_GEN.nextInt(TOTAL_PROB)+1;
		
		if(randInt < rootProb) {
			return false;
		}else {
			return true;			
		}
	}
	
	/**
	 * Determines if create parent based on pos being root.
	 * @param pos
	 * @return
	 */
	private static boolean whetherCreateChild(Pos pos) {
		
		int numChildren = pos.childDepList.size();
		
		final int NUM_CHILDREN_THRESHOLD = 3;
		if(numChildren > NUM_CHILDREN_THRESHOLD) {
			return false;
		}		
		//threshold dist to origin
		final int CHILD_DIST_THRESHOLD = 2;
		if(pos.distToOrigin >= CHILD_DIST_THRESHOLD) {
			return false;
		}
		return true;
	}
	
	/**
	 * Creates phrase for subtree.
	 * @return
	 */
	private String createSubTreePhrase() {
		if(null != this.subTreePhrase) {
			return this.subTreePhrase;
		}
		
		if(this.childDepList.isEmpty()) {
			//leaf Pos
			return this.posWord;
		}
		
		//arrange based on dep avg dist and left-right ordering.
		List<String> leftDepList = new ArrayList<String>();
		List<String> rightDepList = new ArrayList<String>();
		
		Collections.sort(this.childDepList, 
				new Comparator<Dep>(){
					public int compare(Dep dep1, Dep dep2){
						double dep1Dist = dep1.depType().parentChildDist();
						double dep2Dist = dep2.depType().parentChildDist();
						return dep1Dist > dep2Dist ? 1 : (dep1Dist < dep2Dist ? -1 : 0);
					}
				} 
		);
		
		StringBuilder rightSb = new StringBuilder(30);
		StringBuilder leftSb = new StringBuilder(30);

		//Do insertion sort when adding, since list size <= 3, which means
		//each side has on average 1.5 elements.
		for(Dep dep : this.childDepList) {
			
			int parentFirstProb = dep.depType().parentFirstProb();
			
			int randInt = RAND_GEN.nextInt(TOTAL_PROB)+1;
			Pos childPos = dep.childPos();
			String childPosStr = childPos.createSubTreePhrase();
			
			if(randInt < parentFirstProb){
				//depList already sorted
				rightDepList.add(childPosStr);
				rightSb.append(childPosStr).append(" ");
			}else{
				leftDepList.add(0, childPosStr);
				leftSb.insert(0, " ").insert(0, childPosStr);
			}			
		}		
		return leftSb.toString() + this.posWord + " " + rightSb.toString();		
	}
	
	/**
	 * Create sentence string from pos tree, the sentence arranged
	 * based on avg distances in a Dep and left-right ordering.
	 * @param originPos
	 * @return
	 */
	public static String arrangePosStr(Pos originPos) {
		
		Pos curPos = originPos;
		Pos prevPos = curPos;
		
		while(null != curPos) {
			
			curPos.createSubTreePhrase();
			prevPos = curPos;
			Dep parentDep = curPos.parentDep;
			//get the parent
			curPos = parentDep.parentPos();
		}
		
		return prevPos.subTreePhrase;
	}
	
	public PosType posType() {
		return this.posType;
	}
}
