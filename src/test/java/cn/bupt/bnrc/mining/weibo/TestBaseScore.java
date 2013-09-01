package cn.bupt.bnrc.mining.weibo;

import java.util.HashMap;

import org.junit.Test;

import cn.bupt.bnrc.mining.weibo.weka.SentimentalWordExpander6;

public class TestBaseScore {

	//@Test
	public void testBaseScore() {
		int maxBaseScore = 20;
		SentimentalWordExpander6 expander = new SentimentalWordExpander6();
		expander.initEnvironment("index", 4);
		for (int baseScore = 0; baseScore < maxBaseScore; baseScore++){
			expander.setBaseScore(baseScore);
			expander.removeIndicationAndRanking();
			expander.runAll(expander.buildSeedSet());
			expander.storeIndicationAndRankingToFile();
		}
	}
	
	//@Test
	public void testStore(){
		SentimentalWordExpander6 expander = new SentimentalWordExpander6();
		HashMap<String, Integer> example = new HashMap<String, Integer>();
		example.put("test", 1);
		example.put("test1", 2);
		expander.storeWordsOfContext("t s", example);
		HashMap<String, Integer> restore = expander.restoreWordsOfContext("t s");
		if (restore.equals(example)) System.out.println("true");
	}

}
