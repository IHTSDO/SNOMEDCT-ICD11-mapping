package org.ihtsdo.icdmapping;

import org.apache.commons.lang3.StringUtils;

public class SimilarityCalculation {

	public static double standardLevenshteinDistance(String s1, String s2) {
		int length1 = s1.length();
		int length2 = s2.length();
		double longerLength = length1 > length2 ? length1 :length2;
		double similarity = 1 - (StringUtils.getLevenshteinDistance(s1, s2) / longerLength);
		return similarity;
	}
	
	public static double wordLevenshteinDistance(String s1, String s2, int charCount) {
		String s1Recounstruct = recounstructString(s1, charCount, false);
		String s2Recounstruct = recounstructString(s2, charCount, false);
		return standardLevenshteinDistance(s1Recounstruct, s2Recounstruct);
		
	}
	
	public static String recounstructString(String input, int charCount, boolean caseSensitive) {
		StringBuilder sRecounstruct = new StringBuilder();
		for(String part : input.split("[\\W]")) {
			if(part.length()> charCount) {
				sRecounstruct.append(part.substring(0,charCount)); 
			}
			else {
				sRecounstruct.append(part); 
			}
		}
		if(caseSensitive) {
			return sRecounstruct.toString();
		}
		else {
			return sRecounstruct.toString().toLowerCase();
		}
		
		
	}
	
	public static String getPrefix(String input, int charCount) {
			if(input.length()> charCount-1) {
				return (input.substring(0,charCount)); 
			}
			else {
				return null; 
			}
		
		
		
	}
}
