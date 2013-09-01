package cn.bupt.bnrc.mining.weibo.classify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.search.Constants;
import cn.bupt.bnrc.mining.weibo.search.ContentSearcher;

public class WordSentimentClassifier {
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static String[] positiveWords = Emoticons.positiveEmoticonWords;
	public static String[] negativeWords = Emoticons.negativeEmoticonWords;
	
	public static Map<String, Integer> positiveWordsHits = new HashMap<String, Integer>();
	public static Map<String, Integer> negativeWordsHits = new HashMap<String, Integer>();
	
	private double factor = 1;
	private double smoothingFactor = 0.01;
	
	public static int POSITIVE = 10;
	public static int NEGATIVE = 11;
	public static int NEUTRAL = 12;
	public static int UNKOWN = 13;
	
	private ContentSearcher searcher = null;
	
	private Lexicon lexicon = null;
	
	public static void main(String[] args){
		WordSentimentClassifier classifier = new WordSentimentClassifier();
		classifier.run();
	}
	
	
	public WordSentimentClassifier(){
		searcher = new ContentSearcher(Constants.EMOTICON_INDEX);
		this.initTaggedWordsHits();
		lexicon = Lexicon.getInstance();
		lexicon.readUntaggedSentimentWords();
	}
	
	public void run(){
		List<String> words = lexicon.getSentimentWords(2000);
		
		for (String word : words){
			double pmi = this.classifierWord(word);
			if (pmi < 0){
				lexicon.addNegativeWord(word, Math.abs(pmi));
			}else{
				lexicon.addPositiveWord(word, pmi);
			}
		}

		/*
		String anotherPositiveWord = Utils.sortHashMap(positiveWordsMap).get(0).getKey();
		String anotherNegativeWord = Utils.sortHashMapInverse(negativeWordsMap).get(0).getKey();
		logger.info(String.format("anotherPositiveWord: %s, anotherNegativeWord: %s", anotherPositiveWord, anotherNegativeWord));
		
		int deltaPCount = searcher.searchWord(anotherPositiveWord, 1000).totalHits;
		int deltaNCount = searcher.searchWord(anotherNegativeWord, 1000).totalHits;
		factor = factor * deltaNCount/deltaPCount;
		
		logger.info(String.format("deltaPCount: %d, deltaNCount: %d", deltaPCount, deltaNCount));
		logger.info(String.format("new factor: %.10f", factor));
		
		for (String word : words){
			if (!(word.equals(anotherNegativeWord) && word.equals(anotherPositiveWord))){
				double pCount = searcher.search2Words(anotherPositiveWord, word, 1000).totalHits + this.smoothingFactor;
				double nCount = searcher.search2Words(anotherNegativeWord, word, 1000).totalHits + this.smoothingFactor;
				double daf = pCount / nCount;
				
				double previous = 0.0;
				if (positiveWordsMap.containsKey(word)){
					previous = positiveWordsMap.get(word);
				}else if(negativeWordsMap.containsKey(word)){
					previous = negativeWordsMap.get(word);
				}
				
				double delta = Math.log(factor * daf)/Math.log(2);
				double current = previous + delta;

				logger.info(String.format("word: %s, pCount: %f, nCount: %f", word, pCount, nCount));
				String log = String.format("word: %s, previous: %.10f, current: %.10f", word, previous, current);
				logger.info(log);
			}
		}
		*/
	}
	
	public double classifierWord(String word){
		String[] positiveWords = this.getTaggedPositiveWords();
		String[] negativeWords = this.getTaggedNegativeWords();
		double f = 1.0;
		for(int i = 0; i < positiveWords.length; i++){
			String pWord = positiveWords[i];
			String nWord = negativeWords[i];
			double pCount = searcher.search2Words(pWord, word, 1000).totalHits + this.smoothingFactor;
			double nCount = searcher.search2Words(nWord, word, 1000).totalHits + this.smoothingFactor;
			f *= pCount/nCount;
			
			logger.info(String.format("word: %s, pWord: %s, pCount: %f", word, pWord, pCount));
			logger.info(String.format("word: %s, nWord: %s, nCount: %f", word, nWord, nCount));
		}
		double pmi = Math.log(f*factor)/Math.log(2);
		
		logger.info(String.format("word: %s, pmi: %.10f", word, pmi));
		
		return pmi;
	}
	
	public String[] getTaggedPositiveWords(){
		return positiveWords;
	}
	
	public String[] getTaggedNegativeWords(){
		return negativeWords;
	}
	
	public void initTaggedWordsHits(){
		String[] positiveWords = this.getTaggedPositiveWords();
		String[] negativeWords = this.getTaggedNegativeWords();
		if (positiveWords.length != negativeWords.length){
			logger.error("length(positiveWords) != length(negativeWords)");
			System.exit(-1);
		}
		
		for(int i = 0; i < positiveWords.length; i++){
			String pWord = positiveWords[i];
			String nWord = negativeWords[i];
			int pCount = searcher.searchWord(pWord, 1000).totalHits;
			int nCount = searcher.searchWord(nWord, 1000).totalHits;
			
			positiveWordsHits.put(pWord, pCount);
			negativeWordsHits.put(nWord, nCount);
			
			factor *= (double)nCount/pCount;
			
			String log = String.format("i: %d, pWord: %s, pCount: %d -- nWord: %s, nCount: %d -- factor: %.10f",
					i, pWord, pCount, nWord, nCount, factor);
			logger.info(log);
		}
		logger.info("nwords/pwords: {}", factor);
	}
	
	public Lexicon getLexicon(){
		return lexicon;		
	}
	
	public void setLexicon(Lexicon lexicon){
		this.lexicon = lexicon;
	}
}
