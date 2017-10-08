package utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import story.Dep.DepType;

/**
 * Utility class for generating story 
 * @author yihed
 *
 */
public class StoryUtils {

	public static final Pattern WHITE_EMPTY_SPACE_PATT = Pattern.compile("\\s*");
	public static final Pattern WHITE_NON_EMPTY_SPACE_PATT = Pattern.compile("\\s+");
	public static final Pattern SURROUNDING_SPACE_PATT = Pattern.compile("^\\s*(.+?)\\s*$");
	
	/**
	 * Wrapper around list where each successive element 
	 * is guaranteed to be larger, for binary search suited
	 * for this probability scheme.
	 */
	public static class SearchableList<T extends Comparable<T>>{
		
		List<T> list;
		
		public SearchableList(List<T> list_) {
			if(null == list_) {
				throw new IllegalArgumentException("List cannot be null.");
			}
			this.list = list_;
			if(list.isEmpty()) {
				return;
			}
			T prevElem = list.get(0);
			T curElem = list.get(0);
			for(int i = 1; i < list.size(); i++) {
				curElem = list.get(i);
				if(prevElem.compareTo(curElem) > 0) {
					throw new IllegalArgumentException("Elements must be ordered "
							+ "in ascending order to be searchable.");
				}
				prevElem = curElem;
			}			
		}
		
		/**
		 * Looks for target index in list through binary search. So
		 * target lies within the range between the immediately prior
		 * element and the returned index. So strictly in the returned bracket.
		 * @param target
		 * @return
		 */
		public int listBinarySearch(T target) {
			
			return listBinarySearch(target, 0, list.size()-1);
		}
		
		/**
		 * Looks for target index in list through binary search. So
		 * target lies within the range between the immediately prior
		 * element and the returned index. So strictly in the returned bracket.
		 * @param target
		 * @param lowerIndex
		 * @param upperIndex
		 * @return Index in list containing target.
		 */
		private int listBinarySearch(T target, int lowerIndex, int upperIndex) {
			//return lowerIndex, since the current bracket belongs to lowerIndex.
			if(lowerIndex+1 >= upperIndex) {
				//return lowerIndex;
				return upperIndex;
			}

			int midIndex = (lowerIndex+upperIndex)/2;
			T midElem = list.get(midIndex);
			
			if(target.compareTo(midElem) > 0) {
				return listBinarySearch(target, midIndex, upperIndex);
			}else if(target.compareTo(midElem) < 0) {
				return listBinarySearch(target, lowerIndex, midIndex);
			}else {
				return upperIndex;
			}
		}
		
		public T getTargetElem(int targetIndex) {
			return list.get(targetIndex);
		}
		
		@Override
		public String toString() {
			return this.list.toString();
		}		
	}
	
	public static List<String> readLinesFromFile(String fileStr , Charset... charsetAr){
		Charset charset;
		if(0 == charsetAr.length){
			charset = Charset.forName("UTF-8");
		}else{
			charset = charsetAr[0];
		}
		List<String> lines = new ArrayList<String>();
		try{
			//FileReader fReader = null;
			BufferedReader bReader = null;
			InputStream fileInputStream = null;
			InputStreamReader isReader = null;
			try{
				//fReader = new FileReader(fileStr);
				fileInputStream = new FileInputStream(fileStr);
				isReader = new InputStreamReader(fileInputStream, charset);
				bReader = new BufferedReader(isReader);
				
				String line;
				while((line=bReader.readLine()) != null){
					lines.add(line);
				}
			}finally{
				bReader.close();
				isReader.close();
				fileInputStream.close();
			}
		
		}catch(FileNotFoundException e){
			e.printStackTrace();
			throw new IllegalStateException(e);
		}catch(IOException e){
			throw new IllegalStateException(e);
		}
		return lines;
	}
		
	/**
	 * Delete duplicate dep, to avoid e.g. two prepositions stacked together, "as at"
	 * @param depTypeList
	 * @return
	 */
	public static List<DepType> deleteDuplicateDepType(List<DepType> depTypeList) {
		
		Set<DepType> depTypeSet = new HashSet<DepType>();
		List<DepType> noDupList = new ArrayList<DepType>();
		for(DepType depType : depTypeList) {
			if(!depTypeSet.contains(depType)) {
				noDupList.add(depType);
				depTypeSet.add(depType);
			}
		}
		return noDupList;
	}
	
	/**
	 * IDEAS:
	 * -tell a story where all words start with a particular letter!
	 * haikus
	 * any for baron, det, pre (case) should not both occur, for e.g. NOUN
	 * -good starting points: aux
	 * -remove those children pos that are same as parent Pos?
	 * -keep going if no verb so far
	 * -nounand verb good starting pos, now short, good for haikus. noun particular, verb too many verbs
	 * -don't use if first or last word verb <--yes do this.
	 * increase verb occurences! right now seem to need to try a lot to find a verb
	 * -generate several candidates, in the rank, e.g based on non ideal pairs, adj-verb secretarial barrage, ***
	 * -should not end in Aux or verb, aux eg "will", "be", should not start with e.g. cconj.
	 * -add "s" to verbs after tihrd person pronouns.
	 * -one word eg noun, tell a sentence about it. ***
	 * on website, show relation and pos to explain
	 * they 's
	 * -shouldn't end in PRON e.g. "we"
	 * 
	 */
}
