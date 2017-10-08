package story;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.HashMultimap;

import story.Dep.DepType;
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
	private static final int TOTAL_PROB_1000 = 1000;
	private static final int TOTAL_PROB_100 = 100;
	private static final Pattern COMMA_SEP_PATTERN = Pattern.compile("\\s*, \\s*");
	/** max pos count per sentence before stop generating additional dep relations*/
	private static final int MAX_POS_COUNT = 7;
	//pattern used to extract parent-child relations. 3 groups. Don't count those
	//that occur less than 10 times overall. e.g.
	// <a href="">en-dep/flat</a> (1; 0% instances)
	private static final Pattern DEP_PATTERN 
		= Pattern.compile(".+en-dep/(.+)</a>(?:.+?)(\\d+);\\s*([\\d]+)% inst.+");
	//<p><code>VERB</code> nodes are attached to their parents using 32 different relations:
	//4 groups
	private static final Pattern DEP_INTRO_PATTERN = Pattern.compile("(.+)<code>(.+)</code> nodes are attached (.+?):(.+)");
	/**multimap of DepType and their incompatible DepType's*/
	private static final HashMultimap<DepType, DepType> INCOMPATIBLE_PAIRS = HashMultimap.create();
	
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
		
		String[][] incompatiblePairsAr = new String[][] {
			//e.g. "any for baron"
			{"pre", "det"},
			//e.g. "PART AUX" - "to would"
			{"mark", "aux"}
		};
		for(String[] depTypeStrAr : incompatiblePairsAr) {
			Set<DepType> curSet = new HashSet<DepType>();
			for(String depTypeStr : depTypeStrAr) {
				curSet.add(DepType.getTypeFromName(depTypeStr));
			}
			
			for(String depTypeStr : depTypeStrAr) {
				//add whole set, as each dep is incompatible with itself, to avoid duplicates.
				INCOMPATIBLE_PAIRS.putAll(DepType.getTypeFromName(depTypeStr), curSet);				
			}			
		}
	}
	
	Pos(PosType posType_) {
		this.posType = posType_;		
	}
	
	/**
	 * posParentChildType is the role of *this* pos.
	 * @param dep
	 * @param posParentChildType
	 */
	public void addDep(Dep dep, PosPCType posParentChildType) {
		if(posParentChildType == PosPCType.CHILD) {
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
		CCONJ(PosTypeName.CCONJ),
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
			CCONJ("CCONJ"),
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
			
			if(posTypeName == PosTypeName.NONE){
				
				parentDepTypePairList = Collections.emptyList();
				childDepTypePairList = Collections.emptyList();
				parentTotalProb = TOTAL_PROB_1000;
				childTotalProb = TOTAL_PROB_1000;
				return;
			}
			
			//System.out.println("constructing for posTypeName "+posTypeName);
			parentDepTypePairList = parentDepTypePairListMap.get(posTypeName);
			childDepTypePairList = childDepTypePairListMap.get(posTypeName);
			//need to add up total prob from parentDepTypePairListMap!
			//chosenMap.put(PosTypeName.getTypeFromName(posNameStr), probPairList);
			//int parentProb = 0;
			/*for(DepTypeProbPair pair : parentDepTypePairList){
				parentTotalProb += pair.prob;
			}*/
			/*These prob are arranged in increasing order, ie cumulative probabilities*/
			parentTotalProb = parentDepTypePairList.get(parentDepTypePairList.size()-1).prob;
			
			/*for(DepTypeProbPair pair : childDepTypePairList){
				childTotalProb += pair.prob;
			}*/
			childTotalProb = childDepTypePairList.get(childDepTypePairList.size()-1).prob;
			
			//System.out.println("rootProbMap "+rootProbMap);
			Integer rootProb = rootProbMap.get(posTypeName);
			if(null == rootProb){
				isRootProb = 0;
			}else{
				//out of 100 currently
				isRootProb = rootProb;
			}			
			
		}
		
		/**
		 * Obtain a target DepType based on prob maps for given posType, get either parent or child
		 * type.
		 * @param posType
		 * @param posParentChildType Whether supplied posType *should be taken* as parent or child.
		 * @param posCount number of pos already in tree.
		 * @return
		 */
		public static List<DepType> selectRandomDepType(Pos pos, PosPCType posParentChildType, int posCount) {
			
			PosType posType = pos.posType;
			int totalProb = posParentChildType == PosPCType.PARENT ? posType.childTotalProb : posType.parentTotalProb;
			System.out.println("Pos totalProb "+pos + " "+totalProb);
			//get the range over all possible pos value Map<DepType, Integer> parentDepTypeMap
			List<DepTypeProbPair> depTypeList = posParentChildType == PosPCType.PARENT ?  posType.childDepTypePairList
					: posType.parentDepTypePairList;
			
			List<DepType> dTList = new ArrayList<DepType>();			
			
			int numDepType;
			if(posCount > MAX_POS_COUNT) {
				numDepType = 0;
			}else if(posParentChildType == PosPCType.CHILD) {
				numDepType = 1;
			}else {
				// PCProbMap goes from 0 to 100. 
				int randInt = RAND_GEN.nextInt(TOTAL_PROB_100)+1;
				//generate based on stats
				SearchableList<Integer> pcProbList = Story.posTypePCProbMap().get(posType.posTypeName());
				/*index is the bracket for number of children: 0 means leaves (0 child)
				  1 means 1 child, 2 means 2, 3 means 3 children. */
				int index = pcProbList.listBinarySearch(randInt);
				//subtract 1 because padding of posTypePCList at index 0 for 0 children.
				index--;
				//count number of existing children
				numDepType = index - pos.childDepList.size();
				if(pos.distToOrigin > 0) {
					//compound sentences with many grandchildren usually don't make much sense
					numDepType = numDepType > 1 ? numDepType - 1 : numDepType;
				}else //if(pos.distToOrigin == 0) 
				{
					System.out.println("boosting numDepTpye!");
					//also have at least one child if originPos, to avoid empty sentence
					numDepType = numDepType < 3 ? 2 : numDepType;
				}
				
				//numDepType = numDepType == 0 && pos.distToOrigin == 0 ? 1 : numDepType;
			}
			
			for(int i = 0; i < numDepType; i++) {
				//don't add 1 since prob starts at 0
				int randInt = RAND_GEN.nextInt(totalProb);		
				//use binary search to find the right interval,
				//map already sorted according to 				
				int targetIndex = selectRandomDepTypeSearch(randInt, 0, depTypeList.size()-1, depTypeList);
				DepType depType = depTypeList.get(targetIndex).depType;
				
				//avoid incompatible DepType pairs e.g. "det" and "pre (case)" occuring in same list, e.g.: "any for baron".
				//Also remove if chosen DepType is same as the parent type of pos.
				while(shouldRemoveIncompatiblePairs(pos, posParentChildType, depType, dTList)) {
					randInt = RAND_GEN.nextInt(totalProb);
					targetIndex = selectRandomDepTypeSearch(randInt, 0, depTypeList.size()-1, depTypeList);
					depType = depTypeList.get(targetIndex).depType;					
				}
				
				//-1 since the upper bound is returned, rather than lower bound
				//targetIndex = targetIndex == 0 ? targetIndex : targetIndex - 1; 
				dTList.add(depType);
			}
			
			return dTList;			
		}
		
		/**
		 * Remove incompatible DepType pairs e.g. "det" and "pre (case)" occuring in same list, e.g.: "any for baron".
		 * Also remove if chosen DepType is same as the parent type of pos.
		 * @param depType
		 * @param depTypeSet depType so far
		 * @return True if removed, False otherwise.
		 */
		private static boolean shouldRemoveIncompatiblePairs(Pos pos, PosPCType posPCType,
				DepType depType, List<DepType> depTypeList) {
			
			if(posPCType == PosPCType.PARENT && null != pos.parentDep && pos.parentDep.depType() == depType) {
				return true;
			}
			
			for(DepType type : depTypeList) {
				Set<DepType> incompatibleTypes = INCOMPATIBLE_PAIRS.get(type);
				
				if(null == incompatibleTypes) {
					continue;
				}else if(incompatibleTypes.contains(depType)) {					
					return true;
				}
			}
			return false;
		}

		public static int selectRandomDepTypeSearch(int targetProb, int lowerIndex, int upperIndex, 
				List<DepTypeProbPair> depTypePairList) {
			
			if(lowerIndex + 1 == upperIndex){
				//return lowerIndex;
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
		
		@Override
		public String toString() {
			return "{" + depType + " " + prob + "}";
		}
	}/**/
	
	/**
	 * Creates map for statistics for all pos.
	 * DataString e.g. <a href="">en-dep/xcomp</a> (2502; 10% instances)
	 * @param dataString
	 * @param parentDepTypeListMap maps for relations to parent and children
	 * @param childDepTypeListMap
	 */
	private static void createPosStatsMap(String fileStr, Map<PosTypeName, List<DepTypeProbPair>> parentDepTypeListMap,
			Map<PosTypeName, List<DepTypeProbPair>> childDepTypeListMap,
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
					chosenMap = parentDepTypeListMap;
				}else{
					chosenMap = childDepTypeListMap;
				}
				
				List<DepTypeProbPair> probPairList = new ArrayList<DepTypeProbPair>();
				
				String dataStr = introMatcher.group(4);
				String[] dataAr = COMMA_SEP_PATTERN.split(dataStr);			
				Matcher m;
				String depTypeStr;
				int prob;
				int occurrenceCount;
				DepType depType;
				int totalProb = 0;
				//initial padding so binary search can return upper index.
				probPairList.add(new DepTypeProbPair(DepType.NONE, totalProb));
				
				//System.out.println("dataAr "+Arrays.toString(dataAr));
				for(String s : dataAr) {
					if((m=DEP_PATTERN.matcher(s)).matches()) {
						//e.g. "nsubj", or "root"
						depTypeStr = m.group(1);
						//System.out.println("Dep.DepType.getTypeFromName(depTypeStr) "+depTypeStr+" "+Dep.DepType.getTypeFromName(depTypeStr));
						if((depType = Dep.DepType.getTypeFromName(depTypeStr)) != DepType.NONE) {
							occurrenceCount = Integer.parseInt(m.group(2));
							prob = Integer.parseInt(m.group(3));
							//System.out.println("depTypeStr "+depTypeStr);
							if("root".equals(depTypeStr)) {
								rootProbMap.put(PosTypeName.getTypeFromName(posNameStr), prob);
								//root doesn't extend as a dependence relation
								continue;
								//this.isRootProb = prob;
							}
							//experiment with this constant, then make field
							if(prob == 0 && occurrenceCount < 10){
								continue;
							}
							//some data have 0 prob because low occurrence.
							//experiment with this constant!!
							prob = prob == 0 ? 2 : prob*10;
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
	 * create sentence tree given a PosType. Returns
	 * that supplied entry pos, *not* root of tree.
	 * @param posType
	 */
	public static Pos createSentenceTree(PosType posType) {

		//create a pos with that Type
		Pos pos = new Pos(posType);
		pos.posWord = Story.getRandomWord(posType);
		System.out.println("originPos word: "+pos.posWord);
		int countSoFar = 1;
		growTree(pos, countSoFar);
		return pos;
	}

	/**
	 * Attach additional Dep and Pos to given Pos.
	 * @param pos
	 * @param posCount existing number of pos already in tree.
	 * @return updated pos count.
	 */
	private static int growTree(Pos pos, int posCount) {
		
		PosType posType = pos.posType;
		
		//not mutually exclusive!
		//PosPCType parentChildType = PosPCType.generateRandType();
		
		boolean getParentBool = whetherCreateParent(pos);
		//use prob to determine if get parent.
		if(getParentBool) {
			//create Dep with randomly generated DepType
			List<DepType> depTypeList = PosType.selectRandomDepType(pos, PosPCType.CHILD, posCount);
			System.out.println("Pos - parent depTypeList "+depTypeList);
			posCount += depTypeList.size();
			boolean b = false;
			if(b && !depTypeList.isEmpty()) {
				//delete duplicate dep, to avoid e.g. two prepositions stacked together, "as at"
				depTypeList = StoryUtils.deleteDuplicateDepType(depTypeList);
				
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
					System.out.println("randomly selected child matchingPosType: "+matchingPosType + " FOR " + depType
							+ " WORD " + parentPos.posWord);	
				//}	*/
				
				Dep dep = new Dep(depType, parentPos, childPos);
				//don't always add!!! Could repeat
				childPos.addDep(dep, PosPCType.CHILD);
				parentPos.addDep(dep, PosPCType.PARENT);
				
				//grow children
				//don't grow more children for parent node - Oct 8
				//posCount = growTree(parentPos, posCount);
			}			
		}
		
		boolean getChildBool = whetherCreateChild(pos);
		//get_child takes into account e.g. how far from Pos originator. how many children already, etc
		if(getChildBool) {
			//create Dep with randomly generated DepType
			/*if(posType == PosType.SCONJ) {
				System.out.println("sconj!");
			}*/
			List<DepType> depTypeList = PosType.selectRandomDepType(pos, PosPCType.PARENT, posCount);
			posCount += depTypeList.size();
			
			//delete duplicate dep, to avoid e.g. two prepositions stacked together, "as at"
			if(depTypeList.size() > 1) {
				depTypeList = StoryUtils.deleteDuplicateDepType(depTypeList);				
			}
			
			System.out.println("Pos - children depTypeList "+depTypeList);
			for(DepType depType : depTypeList) {
				//this is for child
				PosType matchingPosType = depType.selectRandomMatchingPos(posType, PosPCType.PARENT);
				
				if(null != pos.parentDep) {
					//avoid same consecutive pos, e.g. verb-verb
					PosType parentPosType = pos.parentDep.parentPos().posType;
					while(parentPosType == matchingPosType) {
						matchingPosType = depType.selectRandomMatchingPos(posType, PosPCType.PARENT);
					}
				}
				
				//create Dep from DepType
				//Pos parentPos_, Pos childPos_, DepType depType_
				Pos childPos;
				Pos parentPos;
				//if(parentChildType == PosPCType.PARENT) {
				
				parentPos = pos;
				childPos = new Pos(matchingPosType);
				childPos.distToOrigin = pos.distToOrigin + 1;
					
				childPos.posWord = Story.getRandomWord(matchingPosType);
				System.out.println("randomly selected child matchingPosType: "+matchingPosType + " FOR " + depType
						+ " WORD " + childPos.posWord);
				/*}/*else {
					parentPos = new Pos(matchingPosType);			
					childPos = pos;	
					parentPos.distToOrigin = 1;
				}	*/
				
				Dep dep = new Dep(depType, parentPos, childPos);
				childPos.addDep(dep, PosPCType.CHILD);
				parentPos.addDep(dep, PosPCType.PARENT);
				
				//grow children
				posCount = growTree(childPos, posCount);				
			}
		}
		return posCount;
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
		final int PARENT_DIST_THRESHOLD = 1;
		if(pos.distToOrigin > PARENT_DIST_THRESHOLD) {
			return false;
		}
		
		int rootProb = pos.posType.isRootProb;
		int randInt = RAND_GEN.nextInt(TOTAL_PROB_100)+1;
		
		if(randInt <= rootProb) {
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
		final int CHILD_DIST_THRESHOLD = 1;
		if(pos.distToOrigin > CHILD_DIST_THRESHOLD) {
			return false;
		}
		//verbs tend to have children that are also verbs
		if(pos.posType == PosType.VERB && pos.distToOrigin > 0) {
			return false;
		}
		
		if(pos.posType == PosType.PUNCT) {
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
		
		//List<String> leftDepList = new ArrayList<String>();
		//List<String> rightDepList = new ArrayList<String>();
		
		Collections.sort(this.childDepList, 
				new Comparator<Dep>(){
					public int compare(Dep dep1, Dep dep2){
						//these dist are ~2.5 on avg, with std dev 2.5?
						double dep1Dist = dep1.depType().parentChildDist();
						double dep2Dist = dep2.depType().parentChildDist();
						int comp = dep1Dist > dep2Dist ? 1 : (dep1Dist < dep2Dist ? -1 : 0);
						//introduce some randomness if distance difference is small.
						//but this makes the compare non-transitive and non-symmetric!!
						if(comp > 0 && dep1Dist - dep2Dist < 1 
								|| comp < 0 && dep2Dist - dep1Dist < 1){
							int randInt = RAND_GEN.nextInt(TOTAL_PROB_100);
							if(randInt < 40){
								comp = -comp;
							}
						}
						return comp;
					}
				} 
		);
		
		StringBuilder rightSb = new StringBuilder(30);
		StringBuilder leftSb = new StringBuilder(30);

		//arrange based on dep avg dist and left-right ordering.
		//list already sorted according to distance
		for(Dep dep : this.childDepList) {
			
			int parentFirstProb = dep.depType().parentFirstProb();
			//System.out.println("~~~~parentFirstProb "+parentFirstProb);
			//parentFirstProb between 0 and 100
			int randInt = RAND_GEN.nextInt(TOTAL_PROB_100 + 1);
			Pos childPos = dep.childPos();
			if(childPos == this){
				throw new IllegalArgumentException("child pos equal to this!");
			}
			String childPosStr = childPos.createSubTreePhrase();
			
			if(randInt < parentFirstProb){
				//depList already sorted
				//rightDepList.add(childPosStr);
				rightSb.append(childPosStr).append(" ");
			}else{
				//leftDepList.add(0, childPosStr);
				leftSb.insert(0, " ").insert(0, childPosStr);
			}			
		}		
		this.subTreePhrase = leftSb.toString() + this.posWord + " " + rightSb.toString();
		return this.subTreePhrase;		
	}
	
	/**
	 * Create sentence string from pos tree, the sentence arranged
	 * based on avg distances in a Dep and left-right ordering.
	 * @param originPos
	 * @return
	 */
	public static String arrangePosStr(Pos originPos) {
		
		if(null == originPos) {
			throw new IllegalArgumentException("originPos cannot be null!");
		}
		Pos curPos = originPos;
		Pos prevPos = curPos;
		curPos.createSubTreePhrase();
		
		Dep parentDep = curPos.parentDep;
		
		while(null != parentDep && (curPos = parentDep.parentPos()) != null) {			
			curPos.createSubTreePhrase();
			prevPos = curPos;			
			parentDep = curPos.parentDep;
			//get the parent
		}
		
		return prevPos.subTreePhrase;
	}
	
	public PosType posType() {
		return this.posType;
	}
	
	public String toString(){
		return this.posType.toString();
	}
}
