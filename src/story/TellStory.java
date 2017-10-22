package story;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import story.Pos.PosType;
import story.Pos.PosType.PosTypeName;
import utils.StoryUtils;

/**
 * Tell story given a word.
 * 
 * @author yihed
 *
 */
public class TellStory {

	
	public static void main(String[] args) {
		//guess pos for the input words using pos tagger, 
		
		System.out.println("Please enter an English word: ");
		
		Scanner sc = new Scanner(System.in);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] lineAr = StoryUtils.WHITE_NON_EMPTY_SPACE_PATT.split(line);
			if(lineAr.length == 0) {
				continue;
			}
			String inputWord = lineAr[0];
			if("quit".equals(inputWord)) {
				break;
			}
			
			PosType targetPosType = Story.wordPosTypeMap().get(inputWord);
			
			//PosType posType = PosTypeName.getTypeFromName(word.toUpperCase()).getPosType();
			if(null == targetPosType) {
				System.out.println("Sorry, don't know that word. Please enter another word.");
				continue;
			}
			
			//treemap to keep track of scores of various pos.
			TreeMap<Double, Pos> scorePosTMap = new TreeMap<Double, Pos>();
			//List<String> posStringList = new ArrayList<String>();
			double topScore = 0.;
			boolean inputPosEncountered = false;
			
			int maxIter = 15;
			
			while((!inputPosEncountered || --maxIter > 0) || scorePosTMap.isEmpty() || topScore < 0.97) {	
				//PosType posType = PosType.VERB;
				//origin of tree, the supplied entry point, *not* root
				Pos originPos = Pos.createSentenceTree(targetPosType);
				double initialScore = ScoreTree.MAX_TREE_SCORE;
				
				if(!Pos.treeContainsVerb(originPos)) {
					System.out.println("~~~~~~~ ++++ NO VERB ++++ ");
					continue;
					//initialScore = .6;					
					//System.out.println("*** Trying again to find a verb!");					
				}
				
				//arrange tree into a sentence based on 		
				String sentence = Pos.arrangePosStr(originPos);
				//take sentence length into account
				if(originPos.subTreeWordsList().size() < 5) {
					initialScore = 0.95;
				}
				double score = ScoreTree.computeTreeScore(originPos, initialScore);
				topScore = score > topScore ? score : topScore;
				List<PosType> posTypeList = originPos.subTreePosList();
				
				boolean inputPosEncounteredLocal = false;
				int posTypeListSz = posTypeList.size();
				
				for(int i = 0; i < posTypeListSz; i++) {
					PosType posType = posTypeList.get(i);
					//better to traverse the tree and substitute in place
					if(targetPosType == posType) {
						inputPosEncountered = true;
						inputPosEncounteredLocal = true;
					}
				}
				if(!inputPosEncounteredLocal) {
					System.out.println(" ~~~~~~~~~~~~~~~~~~~~~~ input pos not encountered");
					continue;
				}
				System.out.println("current sentence: " + sentence);
				System.out.println("score: " + score);
				
				System.out.println(" ~~~~~~~~~~~~~~~~~~~~~~ ");
				scorePosTMap.put(score, originPos);
			}
			
			if(scorePosTMap.isEmpty()) {
				System.out.println("Sorry, please enter another word.");
				continue;
			}
			Map.Entry<Double, Pos> mapEntry = scorePosTMap.floorEntry(ScoreTree.MAX_TREE_SCORE);
			Pos winningRootPos = mapEntry.getValue();
			//String sentence = winningRootPos.subTreePhrase();
			StringBuilder sentenceSb = new StringBuilder(100);
			
			List<PosType> posTypeList = winningRootPos.subTreePosList();	
			List<String> wordsList = winningRootPos.subTreeWordsList();
			
			int posTypeListSz = posTypeList.size();
			boolean wordReplaced = false;
			
			for(int i = 0; i < posTypeListSz; i++) {
				PosType posType = posTypeList.get(i);
				//better to traverse the tree and substitute in place
				if(targetPosType == posType && !wordReplaced) {
					sentenceSb.append(inputWord).append(" ");
					wordReplaced = true;
				}else {
					sentenceSb.append(wordsList.get(i)).append(" ");
				}
			}
			
			//e.g. "is verboten divine "
			if(posTypeList.get(0) == PosType.AUX) {
				sentenceSb.append("?");
			}
			
			System.out.println("top sentence: " + sentenceSb.toString());
			System.out.println("posTypeList: " + posTypeList);
			System.out.println("score: " + mapEntry.getKey());
			System.out.println(" ~~~~~~~~~~~~~~~~~~~~~~ ");
			System.out.println("Please enter an English word: ");
		}
		sc.close();
	}
}
