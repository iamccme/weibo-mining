package cn.bupt.bnrc.mining.weibo.classify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.search.Constants;
import cn.bupt.bnrc.mining.weibo.util.Utils;

import com.google.common.io.Files;

public class Lexicon {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	private static Lexicon lexicon = null;
	
	private static Map<String, Double> positiveWordsMap = null;
	private static Map<String, Double> negativeWordsMap = null;
	
	private static Map<String, Double> sentimentWordsMap = null;

	private static Set<String> stopWords = null;
	
	private double smoothFactor = 0.01;
	
	private Lexicon(){}
	public static Lexicon getInstance(){
		if (lexicon == null){
			lexicon = new Lexicon();
			lexicon.readPredictedNegativeWords();
			lexicon.readPredictedPositiveWords();
			lexicon.readSAStopWords();
			Lexicon.negativeWordsMap = lexicon.filter(negativeWordsMap, stopWords);
			Lexicon.positiveWordsMap = lexicon.filter(positiveWordsMap, stopWords);
			Lexicon.sentimentWordsMap = lexicon.filter(sentimentWordsMap, stopWords);
		}
		return lexicon;
	}
	
	public boolean isSentimentWord(String word){
		return sentimentWordsMap.containsKey(word);
	}
	
	public boolean isPositive(String word){
		return positiveWordsMap.containsKey(word);
	}
	
	public boolean isNegative(String word){
		return negativeWordsMap.containsKey(word);
	}
	
	public double getPositiveScore(String word){
		if (isPositive(word)) return positiveWordsMap.get(word);
		return this.smoothFactor;
	}
	
	public double getNegativeScore(String word){
		if (this.isNegative(word)) return negativeWordsMap.get(word);
		return this.smoothFactor;
	}
	
	public void addPositiveWord(String positiveWord, Double score){
		positiveWordsMap.put(positiveWord, score);
	}
	
	public void addNegativeWord(String negativeWord, Double score){
		negativeWordsMap.put(negativeWord, score);
	}
	
	public void addSentimentWord(String word, Double score){
		sentimentWordsMap.put(word, score);
	}
	
	public List<String> getPositiveWords(int count){
		if (positiveWordsMap == null){
			logger.info("getPositiveWords -- positive words map is null");
			return null;
		}
		return this.selectTopNWords(positiveWordsMap, count);
	}
	
	public List<String> getNegativeWords(int count){
		if (negativeWordsMap == null){
			logger.info("getNegativeWords -- negative words map is null");
			return null;
		}
		return this.selectTopNWords(negativeWordsMap, count);
	}
	
	public List<String> getSentimentWords(int count){
		if (sentimentWordsMap == null){
			logger.info("getSentimentWords -- sentiment words map is null");
			return null;
		}
		return this.selectTopNWords(sentimentWordsMap, count);
	}
	
	public void setSentimentWords(Map<String, Double> sentimentWords){
		sentimentWordsMap = sentimentWords;
	}
	
	public void setPositiveWords(Map<String, Double> positiveWords){
		positiveWordsMap = positiveWords;
	}
	
	public void setNegativeWords(Map<String, Double> negativeWords){
		negativeWordsMap = negativeWords;
	}
	
	public void setStopWords(Set<String> stopWords){
		Lexicon.stopWords = stopWords;
	}
	
	public Set<String> readSAStopWords(){
		String filePath = Lexicon.class.getClassLoader().getResource("").getPath() + "/words/not-sentiment-words.txt".substring(1);
		logger.info(String.format("readSAStopWords start -- %s", filePath));
		List<String> stopWords = this.readWordsFromFile(filePath);
		Set<String> stopWordsSet = new HashSet<String>();
		stopWordsSet.addAll(stopWords);
		if (Lexicon.stopWords == null){
			Lexicon.stopWords = stopWordsSet;
		}
		logger.info(String.format("readSAStopWords complete -- size: %d", stopWordsSet.size()));
		return stopWordsSet;
	}
	
	public Map<String,Double> readUntaggedSentimentWords(){
		String filePath = Lexicon.class.getClassLoader().getResource("").getPath() + "/words/sentiment-words.txt".substring(1);
		logger.info(String.format("readUntaggedSentimentWords start -- file: %s", filePath));
		Map<String, Double> sentimentWords = this.readWordValueFromFile(filePath);
		if (sentimentWordsMap == null){
			sentimentWordsMap = sentimentWords;
		}
		logger.info(String.format("readUntaggedSentimentWords complete -- size; %d", sentimentWords.size()));
		return sentimentWords;
	}
	
	public Map<String, Double> readPredictedPositiveWords(){
		String filePath = Lexicon.class.getClassLoader().getResource("").getPath() + "/words/predict-positive.txt".substring(1);
		logger.info(String.format("readPredictedPositiveWords start -- file: %s", filePath));
		Map<String, Double> positiveWords = this.readWordValueFromFile(filePath);
		if (positiveWordsMap == null){
			positiveWordsMap = positiveWords;
		}
		logger.info(String.format("readPredictedPositiveWords complete -- size: %d", positiveWords.size()));
		return positiveWords;
	}
	
	public Map<String, Double> readPredictedNegativeWords(){
		String filePath = Lexicon.class.getClassLoader().getResource("").getPath() + "/words/predict-negative.txt".substring(1);
		logger.info(String.format("readPredictedNegativeWords start -- file: %s", filePath));
		Map<String, Double> negativeWords = this.readWordValueFromFile(filePath);
		if (negativeWordsMap == null){
			negativeWordsMap = negativeWords;
		}
		logger.info(String.format("readPredictedNegativeWords complete -- size: %d", negativeWords.size()));
		return negativeWords;
	}
	
	/**
	 * 从map中，去除包含在toRemove中键。
	 * @TODO 该方法可以改成使用泛型的方式。
	 * @param map
	 * @param toRemove
	 * @return
	 */
	public Map<String, Double> filter(Map<String, Double> map, Set<String> toRemove){
		if (map == null) return null;
		Map<String, Double> resultMap = new HashMap<String, Double>();
		for (Iterator<Entry<String, Double>> it = map.entrySet().iterator();it.hasNext();){
			Entry<String, Double> entry = it.next();
			if (!toRemove.contains(entry.getKey())){
				resultMap.put(entry.getKey(), entry.getValue());
			}
		}
		return resultMap;
	}
	
	private List<String> selectTopNWords(Map<String, Double> map, int count){
		List<String> words = new ArrayList<String>();
		count = Math.min(count, map.size());
		ArrayList<Entry<String, Double>> array = Utils.sortHashMap((HashMap<String, Double>)map);
		for (int i = 0; i < count; i++){
			words.add(array.get(i).getKey());
		}
		return words;
	}
	
	private Map<String, Double> readWordValueFromFile(String filePath){
		Map<String, Double> map = new HashMap<String, Double>();
		
		File file = new File(filePath);
		if (!file.exists()){
			logger.info(String.format("file not exists -- %s", filePath));
			return null;
		}
		try {
			List<String> lines = Files.readLines(file, Constants.defaultCharset);
			for (String entry : lines){
				String[] keyValuePair = entry.split(",");
				map.put(keyValuePair[0], new Double(keyValuePair[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return map;
	}
	
	private List<String> readWordsFromFile(String filePath){
		List<String> words = new ArrayList<String>();
		
		File file = new File(filePath);
		if (!file.exists()){
			logger.info(String.format("file not exists -- %s", filePath));
			return null;
		}
		
		try{
			List<String> lines = Files.readLines(file, Constants.defaultCharset);
			for (String line : lines){
				words.add(line.trim());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return words;
	}
}
