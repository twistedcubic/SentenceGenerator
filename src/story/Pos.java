package story;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import story.Dep.DepType;

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

	private PosType posType;
	//relation to parent for this Pos instance
	private Dep parentDep;
	//relation to child
	private Dep childDep;
	
	static {
		//construct depTypeDataMap by reading data from file
		parentPosTypeDataMap = new HashMap<String, String>();
		childPosTypeDataMap = new HashMap<String, String>();
		
		/////////
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
		
		private PosType(String parentDataString, String childDataString) {
			parentDepTypeMap = new HashMap<DepType, Integer>();
			childDepTypeMap = new HashMap<DepType, Integer>();			
			createMap(parentDataString, parentDepTypeMap);
			createMap(childDataString, childDepTypeMap);			
		}
		
		/**
		 * DataString e.g. <a href="">en-dep/xcomp</a> (2502; 10% instances)
		 * @param dataString
		 * @param parentDepTypeMap
		 * @param childDepTypeMap
		 */
		private static void createMap(String dataString, Map<DepType, Integer> depTypeMap) {
			
			if(null == dataString) {
				throw new IllegalArgumentException("data string for posType cannot be null.");
			}			
			String[] dataAr = COMMA_SEP_PATTERN.split(dataString);			
			Matcher m;
			String depTypeStr;
			int prob;
			DepType depType;
			
			for(String s : dataAr) {
				if((m=DEP_PATTERN.matcher(s)).matches()) {
					depTypeStr = m.group(1);
					
					if((depType = Dep.DepType.getTypeFromName(depTypeStr)) != DepType.NONE) {
						prob = Integer.parseInt(m.group(2));
						depTypeMap.put(depType, prob);
					}					
				}				
			}
			
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
		
		
	}
	
}
