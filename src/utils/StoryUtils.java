package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for generating story 
 * @author yihed
 *
 */
public class StoryUtils {

	/**
	 * Wrapper around list where each successive element 
	 * is guaranteed to be larger, for binary search suited
	 * for this probability scheme.
	 */
	public static class SearchableList<T extends Comparable<T>>{
		
		List<T> list;
		
		public SearchableList(List<T> list_) {
			if(null == list) {
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
			
			if(lowerIndex+1 == upperIndex) {
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
		
	}
	
		
	public static void main(String[] args) {
		//binarySearch(1, 0,1,new ArrayList<Integer>());
	}
}
