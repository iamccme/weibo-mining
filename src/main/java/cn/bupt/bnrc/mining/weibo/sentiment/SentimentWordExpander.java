package cn.bupt.bnrc.mining.weibo.sentiment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import cn.bupt.bnrc.mining.weibo.search.ContentIndexer;
import cn.bupt.bnrc.mining.weibo.search.ContentSearcher;
import cn.bupt.bnrc.mining.weibo.util.Utils;
import cn.bupt.bnrc.mining.weibo.weka.Constants;

/**
 * @TODO
 * add filter to the count of very small context or candidate.
 * another method: mutual information;
 * base on SentimentalWordExpander2.java
 * using redis to write the intermediate result
 * @author cheng chen
 *
 */

@Service
public class SentimentWordExpander {

	private Logger logger = LoggerFactory.getLogger(getClass());
	public ContentSearcher contentSearcher;
	
	private static JedisPool pool;
	public Jedis redis = null;
	
	private int databaseIndex = 10;
	
	private int MAX_SIZE = 500000;
	
	private int NUMBER_OF_NEIGHBORS = 100;
	
	private double DEFAULT_BASE_SCORE = 50;	//to rectify
	
	public int MAX_SENTIMENTAL_WORD_COUNT = 10000;
	
	private int TOTAL_DOCUMENTS_SIZE;
	private int TOTAL_DOCUMENT_WORD_SIZE;
	private double LOG2_DOCUMENT_SIZE;
	
	public static void main(String[] args){
		SentimentWordExpander expander = new SentimentWordExpander();
		
		expander.initRedis(3);
		List<String> commonContexts = expander.selectCommonContexts(4);
		HashMap<String, Double> contexts = new HashMap<String, Double>(commonContexts.size());
		for (String context : commonContexts){
			Double value = new Double(expander.redis.hget(Constants.contextIndicationFileName, context));
			contexts.put(context, value);	//value is used to compute the closeness
		}
		File file = new File("commonContexts.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Utils.hashMapToDisk(contexts, file);
	}
	
	public void expanding(HashSet<String> seeds, String indexPath, List<Map<String,Object>> statuses, int databaseIndex){
		this.databaseIndex = databaseIndex;
		if (statuses != null){
			new ContentIndexer(indexPath).indexStringList(statuses);
		}
		
		contentSearcher = new ContentSearcher(indexPath);
		
		this.initEnvironment();
		
		this.runAll(seeds);
	}
	
	public void expanding(Set<String> seeds, Set<String> contexts, String indexPath, List<String> statuses, int databaseIndex){
		
	}

	public HashSet<String> buildSeedSet(){
		HashSet<String> seeds = new HashSet<String>();
		seeds.add("高兴");
		seeds.add("郁闷");
		seeds.add("恶心");
		seeds.add("美好");
		
		//seeds.add("喜欢");
		//seeds.add("悲观");
		//seeds.add("真诚");
		//seeds.add("鄙视");
		
		return seeds;
	}
	
	public String getNeighborWordFileName(String neighborWord){
		return Constants.candidateWordsWithinContextDir + File.separatorChar + neighborWord + Constants.suffixOfFile;
	}
	
	public String getSeedWordFileName(String seed){
		return Constants.neighborWordsWithinSeedDir + File.separatorChar + seed + Constants.suffixOfFile;
	}
	
	public void initEnvironment(){
		logger.info("initEnvironment starts...");
		try{
			File f1 = new File(Constants.contextCountInTotalFileName);
			if (f1.exists()){
				
			}else{
				f1.createNewFile();
			}
			f1 = new File(Constants.wordCountInTotalFileName);
			if (f1.exists()){
				
			}else{
				f1.createNewFile();
			}
			f1 = new File(Constants.candidateWordsWithinContextDir);
			if (f1.exists()){
				
			}else{
				f1.mkdir();
			}
			
			f1 = new File(Constants.neighborWordsWithinSeedDir);
			if (f1.exists()){
				
			}else{
				f1.mkdir();
			}
			
			this.initRedis(databaseIndex);
			
			this.TOTAL_DOCUMENTS_SIZE = this.getTotalDocumentsCount();
			this.TOTAL_DOCUMENT_WORD_SIZE = this.TOTAL_DOCUMENTS_SIZE * Constants.wordsPerStatus;
			this.LOG2_DOCUMENT_SIZE = Math.log((double)this.TOTAL_DOCUMENT_WORD_SIZE)/Constants.log2;
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.info("initEnvironment ends.");
	}
	
	public void initRedis(int databaseIndex){
		JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxActive(10);
        config.setMaxIdle(10);
        config.setMaxWait(100000);
        config.setTestOnBorrow(true);
		
		pool = new JedisPool(config, Constants.IP, Constants.PORT, 100000);
		redis = pool.getResource();
		redis.select(databaseIndex);
	}
	
	/**
	 * get the hashMap: contextIndication
	 * @param seeds
	 */
	public void getSeedsContext(HashSet<String> seeds){
		logger.info("start getSeedsContext...");		
		Date startTime = new Date();

		for (String seed : seeds){
			logger.info("start extract neighbor words of seed: {}", seed);
			HashMap<String, Integer> contextList = this.extractNeighborWords(seed);		//get context for one seed.
			int wordCount = this.getWordCountInTotal(Constants.wordCountInTotalFileName, seed);	//this should return very fastly
			if(wordCount == 0){		//the seed count in the total statuses is also computed in the extractNeighborWords
				logger.info("word count is zero! this should not be happend!!! -- seed: ", seed);
				return;
			}
			
			logger.info("start update context indication with seed: {}", seed);
			for (Iterator<Entry<String, Integer>> it = contextList.entrySet().iterator();it.hasNext();){	//for every context.
				Entry<String, Integer> entry = it.next();
				String neighborWord = entry.getKey();
				int contextCountInWord = entry.getValue();
				//if (contextCountInWord == 1) continue;	//if the context count is 1, then, we should not consider this context.	why?
				int contextCountInTotal = getContextCountInTotal(Constants.contextCountInTotalFileName, neighborWord);	//compute the count of the neighbor ocurrence in the total statuses
				if (contextCountInTotal == 0){	//for special context, the count is zero, which means the context is illegal
					logger.info("getSeedsContext exception -- contextCountInTotal is zero. neighborWord: {}", neighborWord);
					continue;
				}

				this.updateContextWithSeeds(neighborWord, seed, contextCountInWord);
				this.updateContextIndication(Constants.contextIndicationFileName, neighborWord, wordCount, contextCountInWord, contextCountInTotal);	//update the neighbor word indication
			}
			logger.info("extracting neighbor words of seed: {} completes", seed);
		}
		Date endTime = new Date();
		logger.info("getSeedsContext completes! -- cost time: {} milliseconds. ", endTime.getTime() - startTime.getTime());
		return;
	}
	
	/**
	 * remember the context for which seed is attached.
	 * @param context
	 * @param addingSeedWord
	 * @param value
	 */
	public void updateContextWithSeeds(String context, String addingSeedWord, double value){
		redis.hset(context, addingSeedWord, value+"");
	}

	/**
	 * this method assigns the fraction to context which is extracted from the statuses that contain seeds
	 * @param neighborWord
	 * @param wordCount
	 * @param contextCountInWord
	 * @param contextCountInTotal
	 */
	public void updateContextIndication(String contextMapName, String neighborWord, int wordCount, int contextCountInWord, int contextCountInTotal){
		String value = redis.hget(contextMapName, neighborWord);
		double deltaIndication = this.indicationBetweenWordAndContext(wordCount, contextCountInWord, contextCountInTotal);
		double newValue = deltaIndication;
		if (value != null){
			newValue += new Double(value);
		}
		redis.hset(contextMapName, neighborWord, newValue+"");

		logger.info("updateContextIndication -- neighborWord: {}, wordCount: {}, contextCountInWord: {}, contextCountInTotal: {}, deltaIndication: {}", 
				new Object[]{neighborWord, wordCount, contextCountInWord, contextCountInTotal, deltaIndication});
	}
	
	/**
	 * this method must assure the existence of the contextIndiction of neighborWord
	 * assign the fraction to the candidate word which is extracted from the context. 
	 * @param word
	 * @param neighborWord
	 * @param contextCount
	 * @param wordCountInContext
	 * @param wordCountInTotal
	 */
	public void updateWordIndication(String wordMapName, String word, String neighborWord, int contextCount, int wordCountInContext, int wordCountInTotal){
		double contextFraction = new Double(redis.hget(Constants.contextIndicationFileName, neighborWord));
		double deltaIndication = this.indicationBetweenWordAndContext(contextCount, wordCountInContext, wordCountInTotal) + this.DEFAULT_BASE_SCORE;
		String value = redis.hget(wordMapName, word);
		double newValue = deltaIndication*contextFraction;
		if (value != null){
			newValue += new Double(value);
		}
		redis.hset(wordMapName, word, newValue+"");

		logger.info("updateWordIndication -- word: {}, neighborWord: {}, contextCount: {}, wordCountInContext: {}, wordCountInTotal: {}, deltaIndication: {}",
				new Object[]{word, neighborWord, contextCount, wordCountInContext, wordCountInTotal, deltaIndication});
	}
	
	public void runAll(HashSet<String> seeds){
		this.getSeedsContext(seeds);
		List<String> commonContexts = this.selectCommonContexts(seeds.size());
		HashMap<String, Double> contexts = new HashMap<String, Double>(commonContexts.size());
		for (String context : commonContexts){
			Double value = new Double(redis.hget(Constants.contextIndicationFileName, context));
			contexts.put(context, value);	//value is used to compute the closeness
		}
		//this.normalization(contexts);
		ArrayList<Entry<String, Double>> array = Utils.sortHashMap(contexts);
		
		HashMap<String, Integer> allCandidateWordList = new HashMap<String, Integer>();
		for (int contextIndex = 0; contextIndex < array.size(); contextIndex++){
			logger.info("run -- contextIndex: {}", contextIndex);
			Entry<String, Double> entry = array.get(contextIndex);
			String neighborWord = entry.getKey();
			HashMap<String, Integer> candidateWordList = extractCandidateWords(neighborWord);
			
			Utils.merge(allCandidateWordList, candidateWordList);	//get all candidateWords, but the total count is not used.
		}
		logger.info("compute candidateWords complete!");
		
		ArrayList<Entry<String, Integer>> candidateWords = Utils.sortHashMap(allCandidateWordList);
		int candidateWordSize = candidateWords.size();
		candidateWordSize = Math.min(candidateWordSize, this.MAX_SENTIMENTAL_WORD_COUNT);
		HashMap<String, Double> rankings = new HashMap<String, Double>(candidateWordSize);

		for (int wordIndex = 0; wordIndex < candidateWordSize; wordIndex++){
			logger.info("start to compute word ranking -- wordIndex: {}, totalSize: {}", wordIndex, candidateWordSize);
			Entry<String, Integer> candidateWord = candidateWords.get(wordIndex);	//candidateWord count is not used.
			String wordString = candidateWord.getKey();
			int candidateWordCount = candidateWord.getValue();
			double closeness = 0;
			double unfitness = 1;
			
			HashMap<String, Integer> wordContextMap = this.extractNeighborWords(wordString);
			if (!isContainSeedContexts(wordContextMap, commonContexts, 3)) {
				logger.info("candidateWord: {} not cantains the required common contexts -- context size: {}", wordString, wordContextMap.size());
				continue;
			}
			ArrayList<Entry<String, Integer>> wordContextArray = Utils.sortHashMap(wordContextMap);
			logger.info("start computing word ranking -- word: {}, wordContextSize: {}", wordString, wordContextArray.size());
			
			for (int contextIndex = 0; contextIndex < wordContextArray.size(); contextIndex++){
				Entry<String, Integer> wordContext = wordContextArray.get(contextIndex);
				String wordContextString = wordContext.getKey();
				int contextCountInWord = wordContext.getValue();
				int contextCountInTotal = this.getContextCountInTotal(Constants.contextCountInTotalFileName, wordContextString);
				int wordCountInTotal = this.getWordCountInTotal(Constants.wordCountInTotalFileName, wordString);
				if (wordCountInTotal == 0 || contextCountInTotal == 0) {
					logger.info("wordContextString: {}, contextCountInTotal: {}, wordString: {}, wordCountInTotal: {}", 
							new Object[]{wordContextString, contextCountInTotal, wordString, wordCountInTotal});
					continue;
				}
				double pmi = this.indicationBetweenWordAndContext(wordCountInTotal, contextCountInWord, contextCountInTotal);
				if (commonContexts.contains(wordContextString)){	//common context
					closeness += pmi * contexts.get(wordContextString);
				}else{
					unfitness += pmi;
				}
			}
			double ranking = closeness/unfitness;
			logger.info("compute word ranking -- word: {}, wordCount: {}, closeness: {}, unfitness: {}, ranking: {}",
					new Object[]{wordString, candidateWordCount, closeness, unfitness, ranking});
			rankings.put(wordString, ranking);
			this.updateWordRanking(Constants.wordRankingFileName, wordString, ranking);
			this.updateWordUnfitness(Constants.wordUnfitnessFileName, wordString, unfitness);
			this.updateWordCloseness(Constants.wordIndicationFileName, wordString, closeness);
		}
		
		File file = new File("ranking.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Utils.hashMapToDisk(rankings, file);
	}
	
	public double maxValue(HashMap<String, Double> map){
		double max = -1;
		for (Iterator<Entry<String, Double>> it = map.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			if (max < entry.getValue()) max = entry.getValue();
		}
		return max;
	}
	
	public double sumMap(HashMap<String, Double> map){
		double sum = 0;
		for (Iterator<Entry<String, Double>> it = map.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			sum += entry.getValue();
		}
		return sum;
	}
	
	public void normalization(HashMap<String, Double> map){
		double sum = sumMap(map);
		for (Iterator<Entry<String, Double>> it = map.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			entry.setValue(entry.getValue()/sum);
		}
	}
	
	public boolean isContainSeedContexts(HashMap<String, Integer> candidateWordContexts, List<String> commonContexts, int n){
		int hits = 0;
		for (String commonContext : commonContexts){
			if (candidateWordContexts.containsKey(commonContext)){
				hits += 1;
				if (hits >= n) return true;
			}
		}		
		return false;
	}
	
	public List<String> selectCommonContexts(int minSeedSize){
		Set<String> keys = redis.keys("*");
		List<String> fields = new ArrayList<String>(keys.size());
		for (String key : keys){
			if (!key.contains(".txt")){
				if (redis.hlen(key) >= minSeedSize){
					fields.add(key);
				}
			}
		}
		return fields;
	}
	
	public HashMap<String, Integer> extractNeighborWords(String word){
		HashMap<String, Integer> contextList = null;
		int count = 0;
		
		Date start = new Date();
		TopDocs results = contentSearcher.searchWord(word, MAX_SIZE);
		logger.info("extractNeighborWords -- search word -- word: {}, totalHits: {}", word, results.totalHits);
		
		int number = Math.min(MAX_SIZE, results.totalHits);
		contextList = new HashMap<String, Integer>(number);
		for (int i = 0; i < number; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);
			List<String> contexts = Utils.extractWordNeigbors(content, word);
			for (String context : contexts){
				Integer previous = contextList.get(context);
				previous = previous==null?1:(previous+1);
				contextList.put(context.toString(), previous);
				count++;	//count the word occurrences	
				//TODO: because there are some contexts that only has left or right, so count is smaller
			}
		}
		this.updateWordCountInTotal(Constants.wordCountInTotalFileName, word, count);

		Date end = new Date();
		logger.info("extractNeighborwords word: {}, totalHits: {}, wordCountInTotal: {}, contextNum: {}, cost time: {} total milliseconds", 
				new Object[]{word, results.totalHits, count, contextList.size(), end.getTime() - start.getTime()});
		
		return contextList;
	}
	
	public HashMap<String, Integer> extractCandidateWords(String neighborWord){
		if (neighborWord.trim().length() < 2) return null;
		
		HashMap<String, Integer> candidateWords = null;
		int count = 0;

		Date start = new Date();
		TopDocs results = contentSearcher.searchContext(neighborWord, MAX_SIZE);
		logger.info("extractCandidateWords -- search content -- neighborWord: {}, totalHits: {}", neighborWord, results.totalHits);
		
		String regex = Utils.contextToRegex(neighborWord);
		Pattern pattern = Pattern.compile(regex);
		int number = Math.min(MAX_SIZE, results.totalHits);
		candidateWords = new HashMap<String, Integer>(number);
		for (int i = 0; i < number; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);			
			Matcher matcher = pattern.matcher(content);				
			while (matcher.find()){
				String candidate = matcher.group(1);
				Integer previous = candidateWords.get(candidate);
				previous = previous==null?1:(previous+1);
				candidateWords.put(candidate, previous);
				count++;
			}
		}
		this.updateContextCountInTotal(Constants.contextCountInTotalFileName, neighborWord, count);
		
		Date end = new Date();
		logger.info("extractCandidateWords -- neighborWord: {}, contextTotalHits: {}, contextCountInTotal: {}, candidateWordsNum: {}, cost time: {} total milliseconds", 
				new Object[]{neighborWord, results.totalHits, count, candidateWords.size(), end.getTime() - start.getTime()});
		
		return candidateWords;
	}
	
	/**
	 * the formula: log2(P(y|x)/P(y)), y stands for context, x stands for word.
	 * P(y|x) = contextCountInWord/wordCount.
	 * P(y) = contextCountInTotal/totalDocumentsCount;
	 * 
	 * this is also equaled with: log2(P(x|y)/P(x)), just the same.
	 * @param wordCount
	 * @param contextCountInWord
	 * @param contextCountInTotal
	 * @return
	 */
	public double indicationBetweenWordAndContext(int wordCount, int contextCountInWord, int contextCountInTotal){
		double indication = 0;
		indication = Math.log(contextCountInWord/((double)wordCount*contextCountInTotal))/Constants.log2 + this.LOG2_DOCUMENT_SIZE;
		return indication;
	}
	
	/**
	 * when the corpus is stable, the document count is stable.
	 * this can be utilized.
	 * @return
	 */
	public int getTotalDocumentsCount(){
		return this.contentSearcher.getTotalDocumentsCount();
	}
	
	/**
	 * this method is used to compute the word count in the whole statuses.
	 * for every word, the compute progress is runned for once.
	 * @param word
	 * @return occurrence count
	 */
	public int getWordCountInTotal(String wordMapName, String word){
		int count = 0;
		String totalCount = redis.hget(wordMapName, word);
		if (totalCount != null) {
			logger.info("getWordCountInTotal -- already be computed, word: {}, count: {}", word, totalCount);
			return new Integer(totalCount);
		}
		TopDocs results = contentSearcher.searchWord(word, MAX_SIZE);
		logger.info("start getWordCountInTotal -- word: {}, totalHits: {}", word, results.totalHits);
		
		int number = Math.min(MAX_SIZE, results.totalHits);
		for (int i = 0; i < number; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);			
			List<String> contexts = Utils.extractWordNeigbors(content, word);
			count += contexts.size();
		}
		this.updateWordCountInTotal(Constants.wordCountInTotalFileName, word, count);
		logger.info("getWordCountInTotal -- word: {}, count: {}", word, count);
		return count;
	}
	
	/**
	 * this method is used to compute the neighborWord count in the whole statuses.
	 * The same with wordCountInTotal, for every neighborWord, the compute is runned for once.
	 * @param neighborWord
	 * @return occurrence count
	 */
	public int getContextCountInTotal(String contextMapName, String neighborWord){
		int count = 0;
		String totalCount = redis.hget(contextMapName, neighborWord);
		if (totalCount != null) {
			logger.info("getContextCountInTotal -- already be computed, neighborWord: {}, count: {}", neighborWord, totalCount);
			return new Integer(totalCount);
		}
		
		TopDocs results = contentSearcher.searchContext(neighborWord, MAX_SIZE);
		logger.info("start getContextCountInTotal -- neighborWord: {}, totalHits: {}", neighborWord, results.totalHits);
		
		String regex = Utils.contextToRegex(neighborWord);
		Pattern pattern = Pattern.compile(regex);
		int number = Math.min(MAX_SIZE, results.totalHits);
		for (int i = 0; i < number; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);		
			Matcher matcher = pattern.matcher(content);				
			while (matcher.find()){		//filter some bad search results
				count++;
			}
		}
		this.updateContextCountInTotal(Constants.contextCountInTotalFileName, neighborWord, count);
		logger.info("getContextCountInTotal -- neighborWord: {}, contextTotalHits: {}, contextCountInTotal: {}", 
				new Object[]{neighborWord, results.totalHits, count});
		return count;
	}
	
	public void updateWordCountInTotal(String wordMapName, String word, int count){
		redis.hset(wordMapName, word, count+"");
	}
	
	public void updateContextCountInTotal(String contextMapName, String context, int count){
		redis.hset(contextMapName, context, count+"");
	}
	
	public void updateWordRanking(String wordRankingMapName, String word, double ranking){
		redis.hset(wordRankingMapName, word, ranking+"");
	}
	
	public void updateWordCloseness(String closenessMapName, String word, double closeness){
		redis.hset(closenessMapName, word, closeness+"");
	}
	
	public void updateWordUnfitness(String unfitnessMapName, String word, double unfitness){
		redis.hset(unfitnessMapName, word, unfitness+"");
	}
	
	public HashMap<String, Double> getHashMap(String hashMapName){
		logger.info("get hash map from redis with key: {}", hashMapName);
		HashMap<String, Double> contextMap = null;
		Map<String, String> all = redis.hgetAll(hashMapName);
		contextMap = new HashMap<String, Double>(all.size());
		for (Iterator<Entry<String, String>> it = all.entrySet().iterator(); it.hasNext();){
			Entry<String, String> entry = it.next();
			String key = entry.getKey();
			String value = entry.getValue();
			contextMap.put(key, new Double(value));
		}
		
		return contextMap;
	}
	
	/**
	 * bordar sort algorithm
	 * @param seedNeighbors
	 * @param candidateNeighbors
	 * @return
	 */
	public <T extends Number> float evaluateSimilarity(ArrayList<Entry<String, T>> seedNeighbors, ArrayList<Entry<String, T>> candidateNeighbors){
		int topN = seedNeighbors.size()>candidateNeighbors.size()?candidateNeighbors.size():seedNeighbors.size();
		topN = topN>this.NUMBER_OF_NEIGHBORS?this.NUMBER_OF_NEIGHBORS:topN;
		HashMap<String, Integer> rankedSeedNeighbors = new HashMap<String,Integer>(topN);
		HashMap<String, Integer> rankedCandidateNeighbors = new HashMap<String,Integer>(topN);
		for (int i = 0; i < topN; i++){
			rankedSeedNeighbors.put(seedNeighbors.get(i).getKey(), topN - i);
			rankedCandidateNeighbors.put(candidateNeighbors.get(i).getKey(), topN - i);
		}
		int borda1 = 0;
		int borda2 = 0;
		int borda3 = 0;
		for (int i = 0; i < topN; i++){
			Entry<String, ? extends Number> entry = seedNeighbors.get(i);
			Integer rank = rankedCandidateNeighbors.get(entry.getKey());
			if (rank != null){							//both in A and B
				borda1 += rank.intValue() * (topN - i);
				rankedCandidateNeighbors.remove(entry.getKey());
			}else{										//in A, but not in B
				borda2 += topN - i;
			}
		}
		for (Iterator<Entry<String, Integer>> it = rankedCandidateNeighbors.entrySet().iterator(); it.hasNext();){
			Entry<String, Integer> entry = it.next();
			if (rankedSeedNeighbors.get(entry.getKey()) == null){	//in B, but not in A
				borda3 += entry.getValue();
			}
		}
		borda2 = (borda2==0)?1:borda2;	//this rarely happened, but it can be happend.
		borda3 = (borda3==0)?1:borda3;
		return (float)borda1 / (borda2 * borda3);
	}
	
	public double addupIndication(List<Double> indications){
		double totalIndication = 0d;
		for (Double d : indications){
			totalIndication += d;
		}
		return totalIndication;
	}
	//@Autowired private ContentSearcher contentSearcher;
}
