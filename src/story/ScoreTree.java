package story;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import story.Pos.PosType;

/**
 * Data and methods to score Pos tree created.
 * @author yihed
 *
 */
public class ScoreTree {
	
	private static final Map<PosType, Map<PosType, Double>> posTypeScoreMap;
	public static final double MAX_TREE_SCORE = 1.;
	//default score for same PosType's in a row, used unless explicit score specified.
	public static final double DUPLICATE_SCORE = 0.8;
	
	static {
		posTypeScoreMap = new HashMap<PosType, Map<PosType, Double>>();
		
		//NONE representing empty token after last pos.
		/**ending tokens**/
		addToPosTypeScoreMap(PosType.VERB, PosType.NONE, 0.8);
		addToPosTypeScoreMap(PosType.ADJ, PosType.NONE, 0.85);
		//".. he"
		addToPosTypeScoreMap(PosType.PRON, PosType.NONE, 0.8);
		//".. some"
		addToPosTypeScoreMap(PosType.DET, PosType.NONE, 0.85);
		
		/**starting tokens**/
		addToPosTypeScoreMap(PosType.NONE, PosType.ADJ, 0.92);
		//addToPosTypeScoreMap(PosType.NONE, PosType.ADP, 0.92);
		//"to tree", "of tree"
		addToPosTypeScoreMap(PosType.NONE, PosType.ADP, 0.95);
		
		/**tokens in middle*/
		//"can aboard"
		addToPosTypeScoreMap(PosType.AUX, PosType.ADV, 0.8);
		//"would within"
		addToPosTypeScoreMap(PosType.AUX, PosType.ADP, 0.95);
		
		//"within reflected"
		addToPosTypeScoreMap(PosType.ADP, PosType.ADJ, 0.85);
		//"via bloat"
		addToPosTypeScoreMap(PosType.ADP, PosType.VERB, 0.85);
		//"without can"
		addToPosTypeScoreMap(PosType.ADP, PosType.AUX, 0.9);
		
		//e.g. ceremonially have
		addToPosTypeScoreMap(PosType.ADV, PosType.VERB, 0.83);
		//"hoarsely dreariness"
		addToPosTypeScoreMap(PosType.ADV, PosType.NOUN, 0.9);
		//"direct can"
		addToPosTypeScoreMap(PosType.ADV, PosType.AUX, 0.9);
		//"sometime roundabout"
		addToPosTypeScoreMap(PosType.ADV, PosType.ADP, 0.95);
		//"sucessfully he"
		addToPosTypeScoreMap(PosType.ADV, PosType.PRON, 0.95);

		//"Iran five"
		addToPosTypeScoreMap(PosType.PROPN, PosType.NUM, 0.85);
		addToPosTypeScoreMap(PosType.PROPN, PosType.DET, 0.82);
		//"I apple"
		addToPosTypeScoreMap(PosType.PROPN, PosType.NOUN, 0.7);
		//"this he"
		addToPosTypeScoreMap(PosType.DET, PosType.PRON, 0.75);
		// "a US"
		addToPosTypeScoreMap(PosType.DET, PosType.PROPN, 0.82);
		//"a where"
		addToPosTypeScoreMap(PosType.DET, PosType.AUX, 0.82);
		//"some direct"
		addToPosTypeScoreMap(PosType.DET, PosType.ADV, 0.82);
		addToPosTypeScoreMap(PosType.DET, PosType.VERB, 0.82);
		
		//e.g. "resistant the skein"
		addToPosTypeScoreMap(PosType.ADJ, PosType.DET, 0.85);
		//"gingerly has"
		addToPosTypeScoreMap(PosType.ADJ, PosType.AUX, 0.85);
		//"amok neath"
		addToPosTypeScoreMap(PosType.ADJ, PosType.ADP, 0.85);
		//"auspicious primly"
		addToPosTypeScoreMap(PosType.ADJ, PosType.ADV, 0.9);
		//"recreational sing"
		addToPosTypeScoreMap(PosType.ADJ, PosType.VERB, 0.8);
		//explicitly allow adj-adj pair without resorting to default score
		addToPosTypeScoreMap(PosType.ADJ, PosType.ADJ, 1.);
		
		//"she high"
		addToPosTypeScoreMap(PosType.PRON, PosType.ADJ, 0.89);
		//e.g. "she a"
		addToPosTypeScoreMap(PosType.PRON, PosType.DET, 0.82);
		//"me apple"
		addToPosTypeScoreMap(PosType.PRON, PosType.NOUN, 0.75);
		//"your plagiarize"
		addToPosTypeScoreMap(PosType.PRON, PosType.VERB, 0.82);
		//"I boringly"
		addToPosTypeScoreMap(PosType.PRON, PosType.ADP, 0.95);
		//"she since"
		addToPosTypeScoreMap(PosType.PRON, PosType.SCONJ, 0.9);
		
		//"shrift me"
		addToPosTypeScoreMap(PosType.NOUN, PosType.PRON, 0.8);
		//"bandwidth US"
		addToPosTypeScoreMap(PosType.NOUN, PosType.PROPN, 0.85);
		//"apple 2"
		addToPosTypeScoreMap(PosType.NOUN, PosType.NUM, 0.9);
		addToPosTypeScoreMap(PosType.NOUN, PosType.NOUN, 0.93);
		//"vial these"
		addToPosTypeScoreMap(PosType.NOUN, PosType.DET, 0.94);
		//"apple nicely"
		addToPosTypeScoreMap(PosType.NOUN, PosType.ADJ, 0.95);	
		
		addToPosTypeScoreMap(PosType.VERB, PosType.VERB, 0.5);
		//"refurbish is"
		addToPosTypeScoreMap(PosType.VERB, PosType.AUX, 0.5);
		
		//"2 splice"
		addToPosTypeScoreMap(PosType.NUM, PosType.VERB, 0.7);
		
	}
	
	/**
	 * Adds score corresponding to types to posTypeScoreMap.
	 * @param type1
	 * @param type2
	 * @param score
	 */
	private static void addToPosTypeScoreMap(PosType type1, PosType type2, double score) {
		
		Map<PosType, Double> posMap = posTypeScoreMap.get(type1);
		if(null == posMap) {
			posMap = new HashMap<PosType, Double>();
			//posMap.put(type2, score);
			posTypeScoreMap.put(type1, posMap);
		}
		posMap.put(type2, score);		
	}
	
	private static double getTypePairScore(PosType type1, PosType type2) {
		
		Map<PosType, Double> posMap = posTypeScoreMap.get(type1);
		if(null == posMap) {		
			//same PosType in a row
			if(type1 == type2) {
				return DUPLICATE_SCORE;
			}
			return MAX_TREE_SCORE;
		}
		Double score = posMap.get(type2);
		
		if(null == score) {
			//same PosType in a row
			if(type1 == type2) {
				return DUPLICATE_SCORE;
			}
			return MAX_TREE_SCORE;
		}
		
		return score;		
	}
	
	/**
	 * Compute score based on adjacent PosTypes.
	 * @return
	 */
	public static double computeTreeScore(Pos rootPos, double initialScore) {
		List<PosType> posTypeList = rootPos.subTreePosList();
		int posTypeListSz = posTypeList.size();
		
		if(posTypeListSz == 0) {
			return MAX_TREE_SCORE;
		}
		double treeScore = initialScore * getTypePairScore(PosType.NONE, posTypeList.get(0));
		
		for(int i = 0; i < posTypeListSz-1; i++) {
			treeScore *= getTypePairScore(posTypeList.get(i), posTypeList.get(i+1));
		}
		
		treeScore *= getTypePairScore(posTypeList.get(posTypeListSz-1), PosType.NONE);
		
		return treeScore;
	}
	
}
