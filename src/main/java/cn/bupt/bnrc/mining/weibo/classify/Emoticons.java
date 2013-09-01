package cn.bupt.bnrc.mining.weibo.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 有关表情的类。
 * 在该类中，定义了哪些表情是正向的表情词，哪些是负向的表情词。
 * 正向表情词和负向表情词个数是一样的，都是24个。
 * @author cheng chen
 *
 */
public class Emoticons {

	public static String emoticonRegexStr = "\\[([a-zA-Z\u4e00-\u9FA5]+?)\\]";
	
	public static String[] positiveEmoticonWords = {
		"哈哈", "心", "偷笑", "嘻嘻", "爱你", "鼓掌", "花心", "good", "赞", "酷", "给力", "威武", 
		"太开心", "可爱", "耶", "蛋糕", "奥特曼", "亲亲", "呵呵", "愛你", "兔子", "哇哈哈", "萌", "給力"};
	public static String[] negativeEmoticonWords = {
		"泪", "抓狂", "怒", "衰", "可怜", "汗", "晕", "囧", "困", "生病", "委屈", "淚", 
		"鄙视", "伤心", "怒骂", "崩溃", "可憐","悲伤", "失望", "鄙視", "好可怜", "阴险", "暈", "弱"};
	
	public static Set<String> positiveEmoticonSet = new HashSet<String>();
	public static Set<String> negativeEmoticonSet = new HashSet<String>();
	
	public static Set<String> emoticons = new HashSet<String>();
	static{
		emoticons.addAll(Arrays.asList(positiveEmoticonWords));
		emoticons.addAll(Arrays.asList(negativeEmoticonWords));
		
		positiveEmoticonSet.addAll(Arrays.asList(positiveEmoticonWords));
		negativeEmoticonSet.addAll(Arrays.asList(negativeEmoticonWords));
	}
	
	//this method has some bug..
	//eg. "[给力 对名字很纠结的兔子h 威武]", can't match [..]
	public static boolean isContainEmoticons(String string, Set<String> emoticons){
		String regex = emoticonRegexStr;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(string);
		while (m.find()){
			if(emoticons.contains(m.group(1))) return true;
		}
		return false;
	}
	
	public static List<String> extractEmoticonsFromContent(String content){
		return Emoticons.extractEmoticonsFromContent(content, emoticons);
	}
	
	public static List<String> extractEmoticonsFromContent(String content, Set<String> emoticons){
		List<String> extractedEmoticons = new ArrayList<String>();
		String regex = emoticonRegexStr;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		while (m.find()){
			if (emoticons.contains(m.group(1))){
				extractedEmoticons.add(m.group(1));
			}
		}
		
		return extractedEmoticons;
	}
	
	public static boolean isContainEmoticons(String content){
		return Emoticons.isContainEmoticons(content, emoticons);
	}
	
	public static boolean isPositiveEmoticon(String word){
		if (positiveEmoticonSet.contains(word)) return true;
		return false;
	}
	
	public static boolean isNegativeEmoticon(String word){
		if (negativeEmoticonSet.contains(word)) return true;
		return false;
	}
}
