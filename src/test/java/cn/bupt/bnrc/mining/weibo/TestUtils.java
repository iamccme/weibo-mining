package cn.bupt.bnrc.mining.weibo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import cn.bupt.bnrc.mining.weibo.classify.WordSentimentClassifier;
import cn.bupt.bnrc.mining.weibo.util.Utils;

public class TestUtils {

	//@Test
	public void testContextToRegex() {
		String content = "最好的面是阳春面 最sfs好的茶是绿茶 最好的酒是清酒最 的 最好的菜是生菜 最好的汤是清汤 最好的朋友淡如水 ";
		Pattern p = Pattern.compile(Utils.contextToRegex("最 的"));
		Matcher m = p.matcher(content);
		while (m.find()){
			System.out.println(m.group(1));
		}
	}
	
	//@Test
	public void testIsContainEmoticons(){
		HashSet<String> emoticons = new HashSet<String>();
		emoticons.addAll(Arrays.asList(WordSentimentClassifier.negativeWords));
		emoticons.addAll(Arrays.asList(WordSentimentClassifier.positiveWords));
		String str = "likydba [给力] 对名字很纠结的兔子h [威武]";
		System.out.println(Utils.isContainEmoticons(str, emoticons));
	}

}
