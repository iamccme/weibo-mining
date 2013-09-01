package cn.bupt.bnrc.mining.weibo.sentiment.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import cn.bupt.bnrc.mining.weibo.sentiment.SentimentWordExpander;
import cn.bupt.bnrc.mining.weibo.util.Utils;
import cn.bupt.bnrc.mining.weibo.weka.Constants;

public class TestSeedContextCount extends SentimentWordExpander{

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private String countFileName = "counting.txt";
	
	public static void main(String[] args){
		TestSeedContextCount test = new TestSeedContextCount();
		HashSet<String> seeds = test.buildSeedSet();
		seeds.add("温馨");
		seeds.add("坑爹");
		seeds.add("有用");
		seeds.add("悲哀");
		test.expanding(seeds, "index", null, 3);
	}
	
	public void statisticsWordCount(){
		File file = new File(countFileName);
		HashMap<String, Integer> count = new HashMap<String, Integer>();
		int allCounts = 0;
		if (file.exists()){
			try {
				List<String> lines = Files.readLines(file, Charset.forName("utf-8"));
				for (String line : lines){
					String[] oneLine = line.split(",");
					if (oneLine.length == 2){
						Integer counting = count.get(oneLine[1]);
						counting = (counting==null)?1:(counting+1);
						count.put(oneLine[1], counting);
						allCounts++;
					}else{
						System.out.println("error");
					}
				}
				ArrayList<Entry<String, Integer>> array = Utils.sortHashMap(count);
				for (int i = 0; i < array.size(); i++){
					Entry<String, Integer> entry = array.get(i);
					String counting = entry.getKey();
					Integer wordCount = entry.getValue();
					System.out.println(counting+","+wordCount+","+(double)wordCount/allCounts);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void runAll(HashSet<String> seeds){
		logger.info("runAll in testSeedContextCount");
		this.getSeedsContext(seeds);
		List<String> commonContexts = this.selectCommonContexts(seeds.size());
		HashMap<String, Double> contexts = new HashMap<String, Double>(commonContexts.size());
		for (String context : commonContexts){
			Double value = new Double(redis.hget(Constants.contextIndicationFileName, context));
			contexts.put(context, value);	//value is used to compute the closeness
		}
		//this.normalization(contexts);
		
		logger.info("write commonContexts.txt");
		File file = new File("commonContexts.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Utils.hashMapToDisk(contexts, file);
		
		ArrayList<Entry<String, Double>> array = Utils.sortHashMap(contexts);
		
		HashMap<String, Integer> allCandidateWordList = new HashMap<String, Integer>();
		
		for (int contextIndex = 0; contextIndex < array.size(); contextIndex++){
			logger.info("run -- contextIndex: {}", contextIndex);
			Entry<String, Double> entry = array.get(contextIndex);
			String neighborWord = entry.getKey();
			HashMap<String, Integer> candidateWordList = extractCandidateWords(neighborWord);
			
			file = new File(neighborWord);
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Utils.hashMapToDisk(candidateWordList, file);
			
			this.merge(allCandidateWordList, candidateWordList);	//get all candidateWords, but the total count is not used.
		}
		logger.info("compute candidateWords complete!");
		
		file = new File("counting.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Utils.hashMapToDisk(allCandidateWordList, file);
	}
	public void merge(HashMap<String, Integer> total, HashMap<String, Integer> part){
		Set<Entry<String, Integer>> entrySet = part.entrySet();
		for (Iterator<Entry<String, Integer>> it = entrySet.iterator();it.hasNext();){
			Entry<String, Integer> entry = it.next();
			String key = entry.getKey();
			int value = 1;
			Integer previous = total.get(key);
			previous = previous==null?value:(previous+value);
			total.put(key, previous);
		}
	}
}
