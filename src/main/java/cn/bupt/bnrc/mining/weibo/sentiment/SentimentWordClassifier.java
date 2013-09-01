package cn.bupt.bnrc.mining.weibo.sentiment;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 * classify the sentiment word, whether it is positive, or it is negative;
 * @author cheng chen
 *
 */
public class SentimentWordClassifier {

	public static String[] positiveWords = {"",""};
	public static String[] negativeWords = {"",""};
	
	
	
	public static Map<String, Integer> getPositiveWords(){
		Map<String, Integer> wordSet = new HashMap<String, Integer>();
		
		return wordSet;
	}
	
	public static Map<String, Integer> getNegativeWords(){
		Map<String, Integer> wordSet = new HashMap<String, Integer>();
		
		return wordSet;
	}
}
