package cn.bupt.bnrc.mining.weibo.weka;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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
public class SentimentalWordExpander6 {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ContentSearcher contentSearcher = new ContentSearcher();
	
	private static JedisPool pool;
	private Jedis redis = null;
	
	private int databaseIndex = 10;
	
	private int MAX_SIZE = 5000000;
	
	private int NUMBER_OF_NEIGHBORS = 100;
	
	private int topNContext = 30;		//to rectify
	
	private int baseScore = 50;	//to rectify
	
	private int MAX_SENTIMENTAL_WORD_COUNT = 10000;
	
	private int TOTAL_DOCUMENTS_SIZE;
	private int TOTAL_DOCUMENT_WORD_SIZE;
	private double LOG2_DOCUMENT_SIZE;
	
	public static void main(String[] args){
	}
	
	public void expanding(HashSet<String> seeds, String indexPath, List<Map<String,Object>> statuses, int databaseIndex){
		if (statuses != null){
			new ContentIndexer(indexPath).indexStringList(statuses);
		}
		
		this.initEnvironment(indexPath, databaseIndex);
		if (seeds == null){
			seeds = this.buildSeedSet();
		}
		
		this.runAll(seeds);
	}

	public HashSet<String> buildSeedSet(){
		HashSet<String> seeds = new HashSet<String>();
		seeds.add("高兴");
		seeds.add("郁闷");
		seeds.add("恶心");
		seeds.add("艰难");
		seeds.add("惬意");
		
		return seeds;
	}
	
	public String getNeighborWordFileName(String neighborWord){
		return Constants.candidateWordsWithinContextDir + File.separatorChar + neighborWord + Constants.suffixOfFile;
	}
	
	public String getSeedWordFileName(String seed){
		return Constants.neighborWordsWithinSeedDir + File.separatorChar + seed + Constants.suffixOfFile;
	}
	
	public void initEnvironment(String indexPath, int databaseIndex){
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
			
			this.databaseIndex = databaseIndex;
			this.initRedis(databaseIndex);
			
			contentSearcher = new ContentSearcher(indexPath);
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
	
	public void removeIndicationAndRanking(){
		if (redis == null){
			logger.error("create new redis instance!");
			return;
		}
		logger.info("remove indication and ranking from redis");
		redis.del(Constants.contextIndicationFileName);
		redis.del(Constants.wordIndicationFileName);
		redis.del(Constants.wordRankingFileName);
	}
	
	public void storeIndicationAndRankingToFile(){
		logger.info("store indication and ranking to files");
		this.redisKeyToFile(Constants.contextIndicationFileName);
		this.redisKeyToFile(Constants.wordIndicationFileName);
		this.redisKeyToFile(Constants.wordRankingFileName);
	}
	
	/**
	 * init the hashMap: contextIndication
	 * @param seeds
	 */
	public void initContextList(HashSet<String> seeds){
		logger.info("start initContextList...");
		
		Date startTime = new Date();
		for (String seed : seeds){
			logger.info("start extract neighbor words of seed: {}", seed);
			HashMap<String, Integer> contextList = this.extractNeighborWords(seed);		//get context for one seed.
			int wordCountInTotal = this.getWordCountInTotal(Constants.wordCountInTotalFileName, seed);	//this should return very fastly
			if(wordCountInTotal == 0){		//the seed count in the total statuses is also computed in the extractNeighborWords
				logger.info("word count is zero! this should not be happend!!! -- seed: ", seed);
				return;
			}
			
			logger.info("start update context indication with seed: {}", seed);
			for (Iterator<Entry<String, Integer>> it = contextList.entrySet().iterator();it.hasNext();){	//for every context.
				Entry<String, Integer> entry = it.next();
				String neighborWord = entry.getKey();
				int contextCountInWord = entry.getValue();
				//if (contextCountInWord == 1) continue;	//if the context count is 1, then, we should not consider this context.	
				int contextCountInTotal = getContextCountInTotal(Constants.contextCountInTotalFileName, neighborWord);	//compute the count of the neighbor ocurrence in the total statuses
				if (contextCountInTotal < 2){	//for special context, the count is zero or one, which means the context is illegal
					logger.info("initContext exception -- contextCountInTotal is less than 2. neighborWord: {}", neighborWord);
					continue;
				}
				logger.info("record-1--seed:{},neighborWord:{},wordCountInTotal:{},contextCountInWord:{},contextCountInTotal:{}",
						new Object[]{seed, neighborWord, wordCountInTotal, contextCountInWord, contextCountInTotal});
				this.updateContextIndication(Constants.contextIndicationFileName, 
						neighborWord, wordCountInTotal, contextCountInWord, contextCountInTotal);	//update the neighbor word indication
			}
			logger.info("extracting neighbor words of seed: {} completes", seed);
		}
		Date endTime = new Date();
		logger.info("initContext completes! -- cost time: {} milliseconds. ", endTime.getTime() - startTime.getTime());
		return;
	}

	/**
	 * this method assigns the fraction to context which is extracted from the statuses that contain seeds
	 * @param neighborWord
	 * @param wordCountInTotal
	 * @param contextCountInWord
	 * @param contextCountInTotal
	 */
	public void updateContextIndication(String contextMapName, String neighborWord, 
			int wordCountInTotal, int contextCountInWord, int contextCountInTotal){
		String value = redis.hget(contextMapName, neighborWord);
		double deltaIndication = this.indicationBetweenWordAndContext(wordCountInTotal, contextCountInWord, contextCountInTotal) + this.baseScore;
		double newValue = deltaIndication;
		if (value != null){
			newValue += new Double(value);
		}
		redis.hset(contextMapName, neighborWord, newValue+"");

		logger.info("updateContextIndication -- neighborWord: {}, wordCountInTotal: {}, contextCountInWord: {}, contextCountInTotal: {}, deltaIndication: {}, newValue; {}", 
				new Object[]{neighborWord, wordCountInTotal, contextCountInWord, contextCountInTotal, deltaIndication, newValue});
	}
	
	/**
	 * this method must assure the existence of the contextIndiction of neighborWord
	 * assign the fraction to the candidate word which is extracted from the context. 
	 * @param word
	 * @param neighborWord
	 * @param contextCountInTotal
	 * @param wordCountInContext
	 * @param wordCountInTotal
	 */
	public void updateWordIndication(String wordMapName, String word, String neighborWord, 
			int contextCountInTotal, int wordCountInContext, int wordCountInTotal){
		double contextFraction = new Double(redis.hget(Constants.contextIndicationFileName, neighborWord));
		double deltaIndication = this.indicationBetweenWordAndContext(contextCountInTotal, wordCountInContext, wordCountInTotal) + this.baseScore;
		String value = redis.hget(wordMapName, word);
		double newValue = deltaIndication*contextFraction;
		if (value != null){
			newValue += new Double(value);
		}
		redis.hset(wordMapName, word, newValue+"");

		logger.info("updateWordIndication -- word: {}, neighborWord: {}, contextCountInTotal: {}, wordCountInContext: {}, wordCountInTotal: {}, deltaIndication: {}, newValue: {}",
				new Object[]{word, neighborWord, contextCountInTotal, wordCountInContext, wordCountInTotal, deltaIndication, newValue});
	}
	
	public void runAll(HashSet<String> seeds){
		this.initContextList(seeds);
		HashMap<String, Double> allContext = this.getHashMap(Constants.contextIndicationFileName);
		allContext = Utils.selectTopN(allContext, this.topNContext);
		ArrayList<Entry<String, Double>> array = Utils.sortHashMap(allContext);

		int contextListSize = array.size();
		for(int contextIndex = 0; contextIndex < contextListSize; contextIndex++){		//for every context,
			Entry<String, Double> entry = array.get(contextIndex);
			String neighborWord = entry.getKey();
			ArrayList<Entry<String, Integer>> candidateWordList = Utils.sortHashMap(extractCandidateWords(neighborWord));
			logger.info("runAll -- contextIndex: {}, contextListSize: {}, context: {}, candidateWordsSize: {}", 
					new Object[]{contextIndex, contextListSize, neighborWord, candidateWordList.size()});
			int contextCountInTotal = this.getContextCountInTotal(Constants.contextCountInTotalFileName, neighborWord);	//this should return very fast
			if (contextCountInTotal == 0){
				logger.info("runAll this should not happen -- extract candidate word -- contextCount is zero -- neighborWord: {}", neighborWord);
				continue;
			}
			int candidateWordListSize = candidateWordList.size();
			for (int candidateIndex = 0; candidateIndex < candidateWordListSize; candidateIndex++){				
				Entry<String, Integer> candidateEntry = candidateWordList.get(candidateIndex);
				String candidateWord = candidateEntry.getKey();
				logger.info("runAll -- candidateIndex: {}, totalCandidateWordSize: {}, word: {}", 
						new Object[]{candidateIndex, candidateWordListSize, candidateWord});
				
				Integer wordCountInContext = candidateEntry.getValue();
				int wordCountInTotal = this.getWordCountInTotal(Constants.wordCountInTotalFileName, candidateWord);
				if (wordCountInTotal == 0){
					logger.info("runAll exception -- wordCountInTotal is zero -- candidate word: {}", wordCountInTotal);
					continue;
				}
				logger.info("record-2--neighborWord:{},candidateWord:{},contextCountInTotal:{},wordCountInContext:{},wordCountInTotal:{}",
						new Object[]{neighborWord, candidateWord, contextCountInTotal, wordCountInContext, wordCountInTotal});
				this.updateWordIndication(Constants.wordIndicationFileName, candidateWord, neighborWord, 
						contextCountInTotal, wordCountInContext, wordCountInTotal);
			}			
		}
		
		ArrayList<Entry<String, Double>> countArray = Utils.sortHashMap(this.getHashMap(Constants.wordIndicationFileName));
		logger.info("getting candidate words completes!-- totalCandidateWordsCount size: {}", countArray.size());
		
		int countSentimentWord = Math.min(this.MAX_SENTIMENTAL_WORD_COUNT, countArray.size());
		for (int i = 0; i < countSentimentWord; i++){
			Entry<String, Double> entry = countArray.get(i);
			String candidateWord = entry.getKey();
			double value = entry.getValue();

			if (!seeds.contains(candidateWord)){
				logger.info("start computing the ranking of candidate word: {}", candidateWord);
				double candidateWordUnfitness = 1;
				ArrayList<Entry<String, Integer>> candidateWordContextList = Utils.sortHashMap(this.extractNeighborWords(candidateWord));
				int wordCountInTotal = this.getWordCountInTotal(Constants.wordCountInTotalFileName, candidateWord);
				int number = candidateWordContextList.size();
				for (int candidateWordContextIndex = 0; candidateWordContextIndex < number; candidateWordContextIndex++){
					Entry<String, Integer> tempEntry = candidateWordContextList.get(candidateWordContextIndex);
					String neighborWord = tempEntry.getKey();
					if (!allContext.containsKey(neighborWord)){		//not in the contexts of seeds.
						int contextCountInWord = tempEntry.getValue();
						if (contextCountInWord == 1) continue;
						int contextCountInTotal = this.getContextCountInTotal(Constants.contextCountInTotalFileName, neighborWord);
						if (contextCountInTotal == 0){
							logger.info("runAll -- extract neighborWord of candidateWord. neighborWord: {}, contextCountInTotal is zero!", neighborWord);
							continue;
						}
						//TODO this is not compatible with the updateWordIndication, maybe need to add this.DEFAULT_BASE_SCORE
						logger.info("record-3--candidateWord:{},neighborWord:{},wordCountInTotal:{},contextCountInWord:{},contextCountInTotal:{}",
								new Object[]{candidateWord, neighborWord, wordCountInTotal, contextCountInWord, contextCountInTotal});
						candidateWordUnfitness += this.indicationBetweenWordAndContext(wordCountInTotal, contextCountInWord, contextCountInTotal);
					}
				}
				logger.info("updateCandidateWordRanking -- candidateWord: {}, wordCountInTotal: {}, closeness: {}, unfitness: {}",
						new Object[]{candidateWord, wordCountInTotal, value, candidateWordUnfitness});
				this.updateWordRanking(Constants.wordRankingFileName, candidateWord, value / candidateWordUnfitness);
			}			
		}
		
		logger.info("computing the ranking of candidate words completes!");
	}

	public HashMap<String, Integer> extractNeighborWords(String word){
		HashMap<String, Integer> contextList = this.restoreContextsOfWord(word);
		if (contextList != null) return contextList;
		
		int count = 0;
		
		Date start = new Date();
		TopDocs results = contentSearcher.searchWord(word, MAX_SIZE);
		
		int number = Math.min(MAX_SIZE, results.totalHits);
		contextList = new HashMap<String, Integer>(number);
		for (int i = 0; i < number; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);
			List<String> contexts = Utils.extractWordNeigbors(content, word);
			for (String context : contexts){
				Integer previous = contextList.get(context.toString());
				previous = previous==null?1:(previous+1);
				contextList.put(context.toString(), previous);
				count++;	//count the word occurrences
			}
		}
		this.updateWordCountInTotal(Constants.wordCountInTotalFileName, word, count);

		Date end = new Date();
		logger.info("extractNeighborwords word: {}, totalHits: {}, wordCountInTotal: {}, contextNum: {}, cost time: {} total milliseconds", 
				new Object[]{word, results.totalHits, count, contextList.size(), end.getTime() - start.getTime()});
		
		this.storeContextsOfWord(word, contextList);
		
		return contextList;
	}
	
	public HashMap<String, Integer> extractCandidateWords(String neighborWord){
		if (neighborWord.trim().length() < 2) return null;
		
		HashMap<String, Integer> candidateWords = this.restoreWordsOfContext(neighborWord);
		if (candidateWords != null) return candidateWords;
		
		int count = 0;

		Date start = new Date();
		TopDocs results = contentSearcher.searchContext(neighborWord, MAX_SIZE);
		
		String regex = Utils.contextToRegex(neighborWord);
		Pattern pattern = Pattern.compile(regex);
		int number = Math.min(MAX_SIZE, results.totalHits);
		candidateWords = new HashMap<String, Integer>(this.MAX_SIZE);
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
		
		this.storeWordsOfContext(neighborWord, candidateWords);
		
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
		indication = Math.log(contextCountInWord/((double)wordCount*contextCountInTotal))/Constants.log2 
				+ this.LOG2_DOCUMENT_SIZE;
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
		
		int number = Math.min(MAX_SIZE, results.totalHits);
		for (int i = 0; i < number; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);			
			List<String> contexts = Utils.extractWordNeigbors(content, word);
			count += contexts.size();
		}
		this.updateWordCountInTotal(Constants.wordCountInTotalFileName, word, count);
		logger.info("getWordCountInTotal -- word: {}, wordTotalHits: {}, wordCountInTotal: {}", 
				new Object[]{word, results.totalHits, count});
		
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
			logger.info("getContextCountInTotal -- already be computed, neighborWord: {}, count: {}", 
					neighborWord, totalCount);
			return new Integer(totalCount);
		}
		
		TopDocs results = contentSearcher.searchContext(neighborWord, MAX_SIZE);
		
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
	
	public void setBaseScore(int baseScore){
		this.baseScore = baseScore;
	}
	
	public void setDatabaseIndex(int databaseIndex){
		this.databaseIndex = databaseIndex;
	}
	public int getDatabaseIndex(){
		return this.databaseIndex;
	}
	
	public HashMap<String, Integer> restoreContextsOfWord(String word){
		File file = new File(Constants.neighborWordsWithinSeedDir+"/"+word);
		if (file.exists()){
			return Utils.diskToHashMap(file);
		}else{
			logger.info("contexts of word: {} are not stored!", word);
			return null;
		}
	}
	
	public void storeContextsOfWord(String word, HashMap<String, Integer> map){
		File file = new File(Constants.neighborWordsWithinSeedDir+"/"+word);
		if (file.exists()){
			logger.info("contexts of word: {} are already stored", word);
		}else{
			try{
				file.createNewFile();
				Utils.hashMapToDisk(map, file);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public HashMap<String, Integer> restoreWordsOfContext(String context){
		File file = new File(Constants.candidateWordsWithinContextDir+"/"+context);
		if (file.exists()){
			return Utils.diskToHashMap(file);
		}else{
			logger.info("words of context: {} are not stored!", context);
			return null;
		}
	}
	
	public void storeWordsOfContext(String context, HashMap<String, Integer> map){
		File file = new File(Constants.candidateWordsWithinContextDir+"/"+context);
		if (file.exists()){
			logger.info("words of context: {} are already stored", context);
		}else{
			try{
				file.createNewFile();
				Utils.hashMapToDisk(map, file);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
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
	
	public void redisKeyToFile(String hashMapName){
		HashMap<String, Double> contextMap = this.getHashMap(hashMapName);
		Utils.hashMapToDisk(contextMap, hashMapName);
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
