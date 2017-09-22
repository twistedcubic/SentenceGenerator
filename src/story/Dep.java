package story;

import java.nio.charset.Charset;
import java.util.Collections;
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
import utils.StoryUtils;

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
	//<p>17580 instances of <code>nsubj</code> (96%) are right-to-left (child precedes parent).
	//Average distance between parent and child is 2.0255896408201.
	//used to determine ordering and relation. 
	//keys are dep names, e.g. "nsubj"
	private static final Map<String, Integer> leftRightProbMap;
	//avg distance between parent and child
	private static final Map<String, Double> childDistMap;
	
	private static final Random RAND_GEN = new Random();
	private static final int TOTAL_PROB = 100;
	private static final Pattern AVG_DIST_PATTERN 
		= Pattern.compile(".+verage distance.+is ([\\d]+\\.[\\d]{2}).+");
	private static final Pattern DEP_STATS_PATT 
		= Pattern.compile(".+<code>(.+)</code> \\((\\d+)%\\) are (.+) \\(.+");
	//e.g. parts of speech are connected with <code>neg</code>
	private static final Pattern DEP_STATS_INTRO_PATT 
		= Pattern.compile(".+speech are connected with <code>(.+)</code>:.+");
	
	//<a href="">en-pos/VERB</a>-<a href="">en-pos/PROPN</a> (1372; 8% instances).
	private static Pattern COMMA_SEP_PATTERN = Pattern.compile("\\s*, \\s*");
			//pattern used to extract parent-child relations. 3 groups.
	private static Pattern DEP_PATTERN 
				= Pattern.compile(".+en-pos/(.+)</a>(?:.+)pos/(.+)</a>(?:.+);\\s*([\\d]+)% instance.+");			
			
	private static final Map<String, String> depTypeNameConvertMap;
	private static final Map<String, String> depTypeNameConvertReverseMap;
	
	//dependencies
	
	//type of relation for this instance.
	private DepType depType;
	//whether parent or child 
	private Pos parentPos;
	private Pos childPos;
	
	static {
		//construct depTypeDataMap by reading data from file
		depTypeDataMap = new HashMap<String, String>();
		depTypeNameConvertMap = new HashMap<String, String>();
		depTypeNameConvertReverseMap = new HashMap<String, String>();
		//acl:relcl -> aclrelcl case->pre cc:preconj compound:prt  det:predet
		//nmod:npmod nmod:poss nmod:tmod
		depTypeNameConvertMap.put("acl:relcl", "aclrelcl");
		depTypeNameConvertMap.put("case", "pre");
		depTypeNameConvertMap.put("cc:preconj", "ccpreconj");
		depTypeNameConvertMap.put("compound:prt", "compoundprt");
		depTypeNameConvertMap.put("det:predet", "detpredet");
		depTypeNameConvertMap.put("nmod:npmod", "nmodnpmod");
		depTypeNameConvertMap.put("nmod:poss", "nmodposs");
		depTypeNameConvertMap.put("nmod:tmod", "nmodtmod");
		
		for(Map.Entry<String, String> entry : depTypeNameConvertMap.entrySet()){
			depTypeNameConvertReverseMap.put(entry.getValue(), entry.getKey());
		}
		
		/////////construct map data string map for dep stats!
		createDepTypeDataMap("data/depStats.txt", depTypeDataMap);
		
		leftRightProbMap = new HashMap<String, Integer>();
		childDistMap = new HashMap<String, Double>();
		//create map
		createLeftRightProbMap("data/depLeftRightProb.txt", leftRightProbMap, childDistMap);
		
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
		//don't need names!!!
		acl("acl"),
		advcl("advcl"),
		aclrelcl("aclrelcl"),
		advmod("advmod"),
		amod("amod"),
		appos("appos"),
		aux("aux"),
		auxpass("auxpass"),
		pre("pre"),
		ccpreconj("ccpreconj"),
		cc("cc"),
		ccomp("ccomp"),
		compoundprt("compoundprt"),
		compound("compound"),
		conj("conj"),
		cop("cop"),
		csubj("csubj"),
		csubjpass("csubjpass"),
		dep("dep"),
		detpredet("detpredet"),
		det("det"),
		discourse("discourse"),
		dislocated("dislocated"),
		expl("expl"),
		fixed("fixed"),
		flat("flat"),
		foreign("foreign"),
		goeswith("goeswith"),
		iobj("iobj"),
		list("list"),
		mark("mark"),
		nmodnpmod("nmodnpmod"),
		nmodposs("nmodposs"),
		nmodtmod("nmodtmod"),
		nmod("nmod"),		
		nsubj("nsubj"),
		nummod("nummod"),
		dobj("dobj"),
		orphan("orphan"),
		parataxis("parataxis"),
		punct("punct"),
		reparandum("reparandum"),		
		root("root"),
		vocative("vocative"),
		xcomp("xcomp"),
		//acl:relcl -> aclrelcl case->pre cc:preconj compound:prt  det:predet
		//nmod:npmod nmod:poss nmod:tmod
		//handle exceptions triggered by "" !
		NONE("");		
		
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
		//private int leftRightProb;
		
		/**Prob for Dep to be left-to-right (parent preceds child)
		 * int between 0 and 100.*/		
		private int parentFirstProb;
		
		private DepType(String depTypeName) {
			//System.out.println("Dep - depTypeName "+depTypeName);
			
			if("".equals(depTypeName)){
				//use placeholder constants
				this.parentFirstProb = 1;
				this.parentChildDist = 1;
				
				//create maps for possible pos pairs for this DepType
				parentChildMMap = ArrayListMultimap.create();
				childParentMMap = ArrayListMultimap.create();
				
				parentChildTotalProbMap = Collections.emptyMap();
				childParentTotalProbMap = Collections.emptyMap();
				return;
			}
			
			String mmapDataString = depTypeDataMap.get(depTypeName);
			Integer leftRightProb = leftRightProbMap.get(depTypeName);
			Double depDist = childDistMap.get(depTypeName);
			
			if(null == mmapDataString || null == leftRightProb || null == depDist) {
				
				System.out.println("Dep - depTypeName "+depTypeName+" "
						+leftRightProb + " "+depDist);
				throw new IllegalArgumentException("data string for DepType cannot be null.");
			}
			
			//extract distance from data		
			this.parentFirstProb = leftRightProb;
			this.parentChildDist = depDist;
			
			//create maps for possible pos pairs for this DepType
			parentChildMMap = ArrayListMultimap.create();
			childParentMMap = ArrayListMultimap.create();
			
			parentChildTotalProbMap = new HashMap<PosTypeName, Integer>();
			childParentTotalProbMap = new HashMap<PosTypeName, Integer>();
			createDepMMaps(mmapDataString, parentChildMMap, childParentMMap,
					parentChildTotalProbMap, childParentTotalProbMap);			
			
		}
	
		public static DepType getTypeFromName(String depTypeName) {
			/*switch(depTypeName) {
			
			case "nsubj":
				return NSUBJ;
			case "root":
				return ROOT;
			
			default:
				//better default!?
				return NONE;
			}*/
			depTypeName = normalizeDepTypeName(depTypeName);
			try{
				return DepType.valueOf(depTypeName);
			}catch(IllegalArgumentException e){
				System.out.println("Unrecognized dep name "+depTypeName);
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
			//System.out.println("COMMA_SEP_PATTERN "+COMMA_SEP_PATTERN);
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
						
						//System.out.println("Dep - parentChildTotalProbMap "+parentChildTotalProbMap);
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
							//System.out.println("Dep - parentTypeName childTypeName "+parentTypeName + " "+childTypeName);
							childTotalSoFar = childTotal+prob;
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
		
		public int parentFirstProb(){
			return this.parentFirstProb;
		}
		
		public double parentChildDist(){
			return this.parentChildDist;
		}
		
	}/*end of DepType enum*/
	

	/**
	 * createDepTypeDataMap
	 * @param string
	 * @param deptypedatamap
	 */
	private static void createDepTypeDataMap(String fileStr, Map<String, String> deptypedatamap) {
		List<String> depStatsLines = StoryUtils.readLinesFromFile(fileStr);
		
		Matcher m;
		
		for(String line : depStatsLines){
			if((m=DEP_STATS_INTRO_PATT.matcher(line)).matches()){
				String depTypeName = m.group(1);
				deptypedatamap.put(Dep.normalizeDepTypeName(depTypeName), line);				
			}
		}		
	}
	
	/**
	 * Creates parent-first (left-right) probability.
	 * E.g.
	 * <p>17580 instances of <code>nsubj</code> (96%) are right-to-left (child precedes parent).
	 * Average distance between parent and child is 2.54403066812705.</p>
	 * @param leftRightDataString
	 */
	private static void createLeftRightProbMap(String fileStr, Map<String, Integer> leftRightProbMap,
			Map<String, Double> childDistMap, Charset...charset) {
		//read data in from file
		List<String> lines = StoryUtils.readLinesFromFile(fileStr, charset);
		
		Matcher m;
		String depTypeName;
		
		for(String line : lines){
			
			if(StoryUtils.WHITE_EMPTY_SPACE_PATT.matcher(line).matches()){
				continue;
			}
			boolean probAdded = false;
			boolean distAdded = false;
			if((m=DEP_STATS_PATT.matcher(line)).matches()){
				depTypeName = m.group(1);
				depTypeName = normalizeDepTypeName(depTypeName);
				int prob = Integer.parseInt(m.group(2));
				String leftRightStr = m.group(3);
				
				if(leftRightStr.contains("left-to-right")){
					leftRightProbMap.put(depTypeName, prob);
					probAdded = true;
				}else if(leftRightStr.contains("right-to-left")){
					leftRightProbMap.put(depTypeName, TOTAL_PROB - prob);
					probAdded = true;
				}
				//these two need to be on same line, so know which dep is being referred to
				if((m=AVG_DIST_PATTERN.matcher(line)).matches()){
					double dist = Double.parseDouble(m.group(1));
					childDistMap.put(depTypeName, dist);
					distAdded = true;
				}
			}
			//System.out.println("Dep - line \""+line +"\""+DEP_STATS_PATT.matcher(line).matches()+" "
				//+AVG_DIST_PATTERN.matcher(line).matches());
			if(!probAdded || !distAdded){
				throw new IllegalArgumentException("leftRightDataString must contain ordering data");
			}	
		}		
	}

	public static String normalizeDepTypeName(String name){
		String convertedName = depTypeNameConvertMap.get(name);
		return null == convertedName ? name : convertedName;
	}
	
	public Pos parentPos() {
		return parentPos;
	}

	public Pos childPos() {
		return childPos;
	}

	public DepType depType(){
		return this.depType;
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
