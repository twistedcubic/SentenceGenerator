package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import story.Pos.PosType.PosTypeName;

/**
 * Additional lexicon words not already in data, 
 * e.g. "where".
 * 
 * @author yihed
 */

public class ExtraLexicon {

	private static final ListMultimap<PosTypeName, String> commonPosWordListMultimap;
	private static final Map<String, PosTypeName> wordPosTypeNameMap;
	
	static {
		//map of postype and words of that type.
		commonPosWordListMultimap = ArrayListMultimap.create();
		wordPosTypeNameMap = new HashMap<String, PosTypeName>();
		String wordFreqFileStr = "data/wordFrequency.txt";
		getStockFreq(wordFreqFileStr, commonPosWordListMultimap, wordPosTypeNameMap);
		
	}
	
	private static void getStockFreq(String wordFreqFileStr,
			ListMultimap<PosTypeName, String> commonPosWordMultimap, 
			Map<String, PosTypeName> wordPosTypeNameMap) {
		
		List<String> lines = StoryUtils.readLinesFromFile(wordFreqFileStr);
		
		for(String line : lines) {
			String[] lineAr = StoryUtils.WHITE_NON_EMPTY_SPACE_PATT.split(line);					
			if(lineAr.length < 4) continue;		
			
			// 2nd is word, 4rd is freq
			String word = lineAr[1].trim();					
			PosTypeName wordPosTypeName = getPos(lineAr[2].trim());	
			if(wordPosTypeName == PosTypeName.NONE) {
				continue;
			}
			commonPosWordMultimap.put(wordPosTypeName, word);	
			//only get the most likely one, which appears first.
			if(!wordPosTypeNameMap.containsKey(word)) {
				wordPosTypeNameMap.put(word, wordPosTypeName);
			}
		}		
	}
	
	/**
	 * Get the part of speech corresponding to the pos tag/symbol.
	 * E.g. i -> "pre". 
	 * @param word
	 * @param wordPos
	 * @return
	 */
	public static PosTypeName getPos(String wordPos){
		/*
		 * ADJ("ADJ"),
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
		 */
		String pos;
		switch (wordPos) {
		case "i":
			//preposition, e.g. "on","in"
			pos = "adp";
			break;
		case "p":
			//pronoun
			pos = "pron";
			break;
		case "v":
			pos = "verb";
			break;
		case "n":
			pos = "noun";
			break;
		case "x":
			// not, no etc
			pos = "cconj";
			break;
		case "d":
			// determiner
			pos = "det";
			break;
		case "j":
			pos = "adj";
			break;
		case "r":
			pos = "adv";
			break;
		case "e":
			// "existential there"
			pos = "det";
			break;
		case "a":
			// article, eg the, every, a.
			// classify as adj because of the rules for
			// fusing adj and ent's
			pos = "adj";
			break;
		case "m":
			pos = "num";
			break;
		case "u":
			// interjection, eg oh, yes, um.
			pos = "intj";
			break;
		case "c":
			// conjunctions, eg before, until, although
			// and/or should be parsed as conj/disj, will
			// be overwritten in Maps.java
			pos = "cconj";
			break;
		case "t":
			//only word with this type is "to"
			pos = "adp";
			break;
		default:
			pos = "none";
			//System.out.println("default pos: "+ word + " "+ lineAr[2]);
			// defaultList.add(lineAr[2]);
		}
		
		return PosTypeName.getTypeFromName(pos.toUpperCase());
	}
	
	public static ListMultimap<PosTypeName, String> commonPosWordListMultimap() {
		return commonPosWordListMultimap;
	}
	
	/**
	 * Retrieve map of common words and their most likely PosTypeName.
	 * @return
	 */
	public static Map<String, PosTypeName> wordPosTypeNameMap(){
		return wordPosTypeNameMap;
	}
	
}
