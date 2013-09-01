package cn.bupt.bnrc.mining.weibo.weka.backup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.search.ContentSearcher;

import com.google.common.io.Files;

/**
 * @TODO
 * add filter to the count of very small context or candidate.
 * another method. indicatingDegree = contextCountInWord / wordCount;
 * @author hsgui
 *
 */

@Service
public class SentimentalWordExpander4 {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ContentSearcher contentSearcher = new ContentSearcher();
	
	private int MAX_SIZE = 5000000;
	
	private int NUMBER_OF_NEIGHBORS = 100;
	
	private String seedContextFileName = "seeds-context-file.txt";
	private String candidateWordFileName = "candidate-word-file.txt";
	private String wordCountInTotalFileName = "word-count.txt";
	private String neighborCountInTotalFileName = "neighbor-word-count.txt";
	
	private String candidateWordsWithinContextDir = "candidateWords";
	private String neighborWordsWithinSeedDir = "neighborWords";
	private String lineSeparator = System.getProperty("line.separator");
	private String suffixOfFile = ".txt";
	private Charset charset = Charset.forName("utf-8");
	
	//private static double ADJUSTMENT = Math.E;
	
	private HashMap<String, Integer> wordsOccurrence = new HashMap<String, Integer>(1000000);
	private HashMap<String, Integer> contextOccurrence = new HashMap<String, Integer>(200000);
	private HashMap<String, Double> contextIndication = new HashMap<String, Double>(200000);
	private HashMap<String, Double> wordIndication = new HashMap<String, Double>(100000);
	
	public static void main(String[] args){
		//System.out.println("【滚~   狗狗的心伤要死~".replaceAll("[^a-zA-Z0-9\u4E00-\u9FA5]", " ").replaceAll(" +", " ").trim());
		//System.out.println("★测试 旧爱结婚时你会伤心吗 http   sinaurl cn hqEkhq  围观".replaceAll("[^a-zA-Z0-9\u4E00-\u9FA5]", " ").replaceAll(" +", " ").trim());
		//System.out.println("★测试 旧爱结婚时你会伤心吗 http   sinaurl cn hqEkhq  围观".replaceAll("\\p{Punct}", " ").replaceAll(" +", " ").trim());
		
		SentimentalWordExpander4 expander = new SentimentalWordExpander4();
		HashSet<String> seeds = new HashSet<String>();
		seeds.add("高兴");
		seeds.add("郁闷");
		seeds.add("烂");
		seeds.add("美好");
		seeds.add("喜欢");
		//System.out.println(expander.extractWordContext("陈翔淘汰 我好伤心啊", "伤心"));
		//System.out.println(expander.extractWordContext("伤心 愤怒 绝望 不意外", "伤心"));
		//System.out.println(expander.extractWordContext("你丢下李总 她很伤心啊", "伤心"));
		//System.out.println(expander.extractWordContext("很伤心 突然间 失望", "伤心"));
		//System.out.println(expander.extractWordContext("貌似没有那么伤心", "伤心"));
		//expander.extractNeighborWords("快乐");
		//expander.extractCandidateWords("很 啊");
		expander.runAll(seeds);
		//expander.getContextCountInTotal("烂 h");
	}
	
	/*
	public void generateSeedsContext(String[] seeds){
		File file = new File(this.seedContextFileName);
		if (file.exists()) {
			logger.info("generateSeedsContextFile -- file: {} already exists!", this.seedContextFileName);
			return;
		}
		HashMap<String, Integer> totalContextList = new HashMap<String, Integer>();
		for (String seed : seeds){
			HashMap<String, Integer> contextList = this.extractNeighborWords(seed);
			this.merge(totalContextList, contextList);
		}
		logger.info("merge all the seeds context -- finished!");
		logger.info("all the seeds context size: {}", totalContextList.size());
		this.hashMapToDisk(totalContextList, file);
		logger.info("write seeds context to file: {} finished!", this.seedContextFileName);
	}
	*/
	
	public String getNeighborWordFileName(String neighborWord){
		return this.candidateWordsWithinContextDir + File.separatorChar + neighborWord + this.suffixOfFile;
	}
	
	public String getSeedWordFileName(String seed){
		return this.neighborWordsWithinSeedDir + File.separatorChar + seed + this.suffixOfFile;
	}
	
	public <T extends Number> void arrayToDisk(ArrayList<Entry<String, T>> array, File file){
		if (file.exists()){
			try {
				Files.move(file, new File(file.getName()+new Date().toString()));
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("arrayToDisk -- write to file: {} -- start", file.getName());
		try{
			for (Entry<String, T> entry : array){
				Files.append(this.entryToString(entry), file, this.charset);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.info("arrayToDisk -- write to file: {}, size: {} -- complete", file.getName(), array.size());
	}
	
	/**
	 * write to file, maybe serialization.
	 * I use the Generics, so I can write all the Number classes.
	 * @param hashMap
	 * @param file
	 */
	public <T extends Number> void hashMapToDisk(HashMap<String, T> hashMap, File file){
		if (file.exists()){
			try {
				Files.move(file, new File(file.getName()+new Date().toString()));
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("hashMapToDisk -- write to file: {} -- start", file.getName());
		ArrayList<Entry<String, T>> array = this.sortHashMap(hashMap);
		try{
			for (Entry<String, T> entry : array){
				Files.append(this.entryToString(entry), file, this.charset);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.info("hashMapToDisk -- write to file: {} -- complete", file.getName());
	}
	
	/**
	 * when read data from the file, all the number will be regarded as Double default.
	 * so, don't use it if you don't known about it.
	 * @param file
	 * @return
	 */
	public HashMap<String, Double> diskToHashMap(File file){
		logger.info("diskToHashMap -- read file: {} -- start", file.getName());
		HashMap<String, Double> map = new HashMap<String, Double>();
		try {
			List<String> lines = Files.readLines(file, charset);
			for (String entry : lines){
				String[] keyValuePair = entry.split(",");
				map.put(keyValuePair[0], new Double(keyValuePair[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("diskToHashMap -- read file: {}, map size: {} -- finish", file.getName(), map.size());
		
		return map;
	}
	
	public <T extends Number> String entryToString(Entry<String, T> entry){
		return entry.getKey()+","+entry.getValue()+this.lineSeparator;
	}
	
	public void initEnvironment(){
		try{
			File f1 = new File(this.neighborCountInTotalFileName);
			if (f1.exists()){
				
			}else{
				f1.createNewFile();
			}
			f1 = new File(this.wordCountInTotalFileName);
			if (f1.exists()){
				
			}else{
				f1.createNewFile();
			}
			f1 = new File(this.candidateWordsWithinContextDir);
			if (f1.exists()){
				
			}else{
				f1.mkdir();
			}
			
			f1 = new File(this.neighborWordsWithinSeedDir);
			if (f1.exists()){
				
			}else{
				f1.mkdir();
			}
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	/**
	 * init the hashMap: contextIndication
	 * @param seeds
	 */
	public void initContextList(HashSet<String> seeds){
		logger.info("start initContextList...");
		
		Date startTime = new Date();
		for (String seed : seeds){
			logger.info("runAll -- extractNeighborWords of seed: {}", seed);
			HashMap<String, Integer> contextList = this.extractNeighborWords(seed);		//get context for one seed.
			if(this.wordsOccurrence.get(seed) == null){		//the seed count in the total statuses is also computed in the extractNeighborWords
				logger.info("this should not be happend!!! -- seed: ", seed);
				return;
			}
			
			int wordCount = this.getWordCountInTotal(seed);	//this should return very fastly
			logger.info("runAll -- updateContextIndication");
			for (Iterator<Entry<String, Integer>> it = contextList.entrySet().iterator();it.hasNext();){	//for every context.
				Entry<String, Integer> entry = it.next();
				String neighborWord = entry.getKey();
				int contextCountInWord = entry.getValue();
				if (contextCountInWord == 1) continue;	//if the context count is 1, then, we should not consider this context.				
				this.updateContextIndication(this.contextIndication, neighborWord, wordCount, contextCountInWord, -1);	//update the neighbor word indication
			}
		}
		Date endTime = new Date();
		logger.info("initContext completes! -- context size: {}, cost time: {} milliseconds. ",	this.contextIndication.size(), endTime.getTime() - startTime.getTime());
		return;
	}
	
	public void saveScene(){
		
	}
	
	/**
	 * this method assigns the fraction to context which is extracted from the statuses that contain seeds
	 * @param neighborWord
	 * @param wordCount
	 * @param contextCountInWord
	 * @param contextCountInTotal
	 */
	public void updateContextIndication(HashMap<String, Double> contextIndication, String neighborWord, int wordCount, int contextCountInWord, int contextCountInTotal){
		Double value = contextIndication.get(neighborWord);
		double deltaIndication = contextCountInWord / (double)wordCount;
		double newValue = deltaIndication;
		if (value != null){
			newValue += value;
		}
		contextIndication.put(neighborWord, newValue);

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
	public void updateWordIndication(HashMap<String, Double> wordIndication, String word, String neighborWord, int contextCount, int wordCountInContext, int wordCountInTotal){
		double contextFraction = this.contextIndication.get(neighborWord);
		double deltaIndication = wordCountInContext / (double)contextCount;
		Double value = wordIndication.get(word);
		double newValue = deltaIndication*contextFraction;
		if (value != null){
			newValue += value;
		}
		wordIndication.put(word, newValue);

		logger.info("updateWordIndication -- word: {}, neighborWord: {}, contextCount: {}, wordCountInContext: {}, wordCountInTotal: {}, deltaIndication: {}",
				new Object[]{word, neighborWord, contextCount, wordCountInContext, wordCountInTotal, deltaIndication});
	}
	
	public void runAll(HashSet<String> seeds){
		this.initEnvironment();
		this.initContextList(seeds);
		ArrayList<Entry<String, Double>> array = sortHashMap(this.contextIndication);
		this.arrayToDisk(array, new File(this.seedContextFileName));
		int contextListSize = array.size();
		for(int contextIndex = 0; contextIndex < contextListSize; contextIndex++){	//for every context
			logger.info("runAll -- contextIndex: {}, totalContextListSize: {}", contextIndex, contextListSize);
			Entry<String, Double> entry = array.get(contextIndex);
			String neighborWord = entry.getKey();
			//if (value < THRESHOLD) break;
			HashMap<String, Integer> candidate = extractCandidateWords(neighborWord);	//extract the candidate words
			ArrayList<Entry<String, Integer>> candidateWordList = this.sortHashMap(candidate);
			int contextCount = this.getContextCountInTotal(neighborWord);	//this should return very fast
			if (contextCount == 0){
				logger.info("runAll this should not happen -- extract candidate word -- contextCount is zero -- neighborWord: {}", neighborWord);
				continue;
			}
			int candidateWordListSize = candidateWordList.size();
			for (int candidateIndex = 0; candidateIndex < candidateWordListSize; candidateIndex++){
				logger.info("runAll -- candidateIndex: {}, totalCandidateWordSize: {}", candidateIndex, candidateWordListSize);
				Entry<String, Integer> candidateEntry = candidateWordList.get(candidateIndex);
				String word = candidateEntry.getKey();
				Integer wordCountInContext = candidateEntry.getValue();
				this.updateWordIndication(this.wordIndication, word, neighborWord, contextCount, wordCountInContext, -1);
			}			
		}
		logger.info("run candidate words -- totalCount size: {}", this.wordIndication.size());
		
		ArrayList<Entry<String, Double>> countArray = sortHashMap(this.wordIndication);
		HashMap<String, Double> candidateWordRanking = new HashMap<String, Double>(this.wordIndication.size());
		this.arrayToDisk(countArray, new File(this.candidateWordFileName));
		for (int i=0; i< countArray.size(); i++){
			Entry<String, Double> entry = countArray.get(i);
			String candidateWord = entry.getKey();
			Double value = entry.getValue();
			//if (value < THRESHOLD) break;
			
			if (!seeds.contains(candidateWord)){
				double candidateWordUnfitness = 0;
				HashMap<String, Integer> candidateWordContextList = this.extractNeighborWords(candidateWord);
				int wordCount = this.getWordCountInTotal(candidateWord);
				for (Iterator<Entry<String, Integer>> it = candidateWordContextList.entrySet().iterator(); it.hasNext();){
					Entry<String, Integer> tempEntry = it.next();
					String neighborWord = tempEntry.getKey();
					if (!this.contextIndication.containsKey(neighborWord)){		//not in the contexts of seeds.
						int contextCountInWord = tempEntry.getValue();
						if (contextCountInWord == 1) continue;
						logger.info("updateCandidateWordIndication -- neighborWord: {}, wordCount: {}, contextCountInWord: {}, contextCountInTotal: {}",
								new Object[]{neighborWord, wordCount, contextCountInWord, -1});
						candidateWordUnfitness += this.indicationBetweenWordAndContext(wordCount, contextCountInWord, -1);
					}
				}
				candidateWordRanking.put(candidateWord, value / candidateWordUnfitness);
			}			
		}
		this.hashMapToDisk(candidateWordRanking, new File("rank.result"));
		this.hashMapToDisk(this.contextOccurrence, new File(this.neighborCountInTotalFileName));
		this.hashMapToDisk(this.wordsOccurrence, new File(this.wordCountInTotalFileName));
	}
	
	public <T extends Number> ArrayList<Entry<String, T>> sortHashMap(HashMap<String, T> hashMap){
		ArrayList<Entry<String, T>> array = new ArrayList<Entry<String, T>>(hashMap.entrySet());
		Collections.sort(array, new Comparator<Entry<String, ? extends Number>>(){
				public int compare(Entry<String, ? extends Number> o1, Entry<String, ? extends Number> o2){
					if (o2.getValue().doubleValue() == o1.getValue().doubleValue()) return 0;
					return (o2.getValue().doubleValue() > o1.getValue().doubleValue())?1:-1;
				}
		});
		return array;
	}
	
	public void merge(HashMap<String, Integer> total, HashMap<String, Integer> part){
		Set<Entry<String, Integer>> entrySet = part.entrySet();
		for (Iterator<Entry<String, Integer>> it = entrySet.iterator();it.hasNext();){
			Entry<String, Integer> entry = it.next();
			String key = entry.getKey();
			Integer value = entry.getValue();
			Integer previous = total.get(key);
			previous = previous==null?value:(previous+value);
			total.put(key, previous);
		}
	}
	
	public WordContext extractWordContext(String content, String word){
		WordContext context = new WordContext();
		int indexOfWord = content.indexOf(word);
		if (indexOfWord < 0) return null;
		int lastPosition = indexOfWord + word.length();
		if (indexOfWord > 0){
			if (content.charAt(indexOfWord - 1) != ' '){
				context.prefixWord = content.substring(indexOfWord - 1, indexOfWord);
			}
		}
		if (lastPosition < content.length()){
			if (content.charAt(lastPosition) != ' '){
				context.suffixWord = content.substring(lastPosition, lastPosition + 1);
			}
		}
		return context;
	}
	
	public boolean isPassedContext(WordContext context){
		return context != null && context.prefixWord != null && context.suffixWord != null;
	}
	
	public HashMap<String, Integer> extractNeighborWords(String word){
		HashMap<String, Integer> contextList = null;
		int count = 0;
		
		Date start = new Date();
		TopDocs results = contentSearcher.searchWord(word, MAX_SIZE);
		
		int number = (results.totalHits>MAX_SIZE)?MAX_SIZE:results.totalHits;
		contextList = new HashMap<String, Integer>(number);
		for (int i = 0; i < number; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);
			WordContext context = this.extractWordContext(content, word);
			if (this.isPassedContext(context)){
				Integer previous = contextList.get(context.toString());
				previous = previous==null?1:(previous+1);
				contextList.put(context.toString(), previous);
				count++;	//count the word occurrences
			}
		}
		if (wordsOccurrence.get(word) == null)	wordsOccurrence.put(word, count);
		Date end = new Date();
		logger.info("extractNeighborwords seed word: {}, size: {}, cost time: {} total milliseconds", new Object[]{word, contextList.size(), end.getTime() - start.getTime()});
		
		return contextList;
	}
	
	public HashMap<String, Integer> extractCandidateWords(String neighborWord){
		//logger.info("extractCandidateWords -- neighborWord: {} -- start...", neighborWord);
		if (neighborWord.trim().length() < 2) return null;
		
		HashMap<String, Integer> candidateWords = null;
		int count = 0;

		Date start = new Date();
		TopDocs results = contentSearcher.searchContext(neighborWord, MAX_SIZE);
		
		String regex = neighborWord.charAt(0) + "([a-zA-Z\u4e00-\u9FA5&&[^"+neighborWord.charAt(0)+"]]{1,4}?)" + neighborWord.charAt(2);
		Pattern pattern = Pattern.compile(regex);
		int number = (results.totalHits>MAX_SIZE)?MAX_SIZE:results.totalHits;
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
		if (contextOccurrence.get(neighborWord) == null) contextOccurrence.put(neighborWord, count);
		
		Date end = new Date();
		logger.info("extractCandidateWords -- neighborWord: {}, candidateWords size: {}, cost time: {} total milliseconds", new Object[]{neighborWord, candidateWords.size(), end.getTime() - start.getTime()});
		return candidateWords;
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
	public int getWordCountInTotal(String word){
		int count = 0;
		if (this.wordsOccurrence.get(word) != null) {
			logger.info("getWordCountInTotal -- already be computed, word: {}, count: {}", word, this.wordsOccurrence.get(word));
			return this.wordsOccurrence.get(word);
		}
		TopDocs results = contentSearcher.searchWord(word, MAX_SIZE);
		logger.info("start getWordCountInTotal -- word: {}, totalHits: {}", word, results.totalHits);
		
		for (int i = 0; i < results.totalHits; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);			
			if (content.indexOf(word) != -1) count++;	//we should filter the search result, for the search engine token the word by character
		}
		this.wordsOccurrence.put(word, count);
		logger.info("getWordCountInTotal -- word: {}, count: {}", word, count);
		return count;
	}
	
	/**
	 * this method is used to compute the neighborWord count in the whole statuses.
	 * The same with wordCountInTotal, for every neighborWord, the compute is runned for once.
	 * @param neighborWord
	 * @return occurrence count
	 */
	public int getContextCountInTotal(String neighborWord){
		int count = 0;
		if (this.contextOccurrence.get(neighborWord) != null) {
			logger.info("getContextCountInTotal -- already be computed, neighborWord: {}, count: {}", neighborWord, this.contextOccurrence.get(neighborWord));
			return this.contextOccurrence.get(neighborWord);
		}
		
		TopDocs results = contentSearcher.searchContext(neighborWord, MAX_SIZE);
		logger.info("start getWordCountInTotal -- neighborWord: {}, totalHits: {}", neighborWord, results.totalHits);
		
		String regex = neighborWord.charAt(0) + "([a-zA-Z\u4e00-\u9FA5&&[^"+neighborWord.charAt(0)+"]]{1,4}?)" + neighborWord.charAt(2);
		Pattern pattern = Pattern.compile(regex);
		for (int i = 0; i < results.totalHits; i++){
			String content = contentSearcher.getDocContent(results.scoreDocs[i].doc).toLowerCase(Locale.ENGLISH);		
			Matcher matcher = pattern.matcher(content);				
			while (matcher.find()){		//filter some bad search results
				count++;
			}
		}
		this.contextOccurrence.put(neighborWord, count);
		logger.info("getContextCountInTotal -- neighborWord: {}, total count: {}", neighborWord, count);
		return count;
	}
	
	public double addupIndication(List<Double> indications){
		double totalIndication = 0d;
		for (Double d : indications){
			totalIndication += d;
		}
		return totalIndication;
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
		indication = contextCountInWord / (double)wordCount;
		//indication += ADJUSTMENT;
		return indication;
	}
	
	//@Autowired private ContentSearcher contentSearcher;
}
