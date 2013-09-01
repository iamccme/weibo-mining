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

@Service
public class SentimentalWordExpander1 {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ContentSearcher contentSearcher = new ContentSearcher();
	
	private int MAX_SIZE = 1000000;
	private static int THRESHOLD = 100;
	
	private int NUMBER_OF_NEIGHBORS = 100;
	
	private String seedContextFileName = "seeds-context-file.txt";
	private String candidateWordsWithinContextDir = "candidateWords";
	private String neighborWordsWithinSeedDir = "neighborWords";
	private String lineSeparator = System.getProperty("line.separator");
	private String suffixOfFile = ".txt";
	private Charset charset = Charset.forName("utf-8");
	
	public static void main(String[] args){
		//System.out.println("【滚~   狗狗的心伤要死~".replaceAll("[^a-zA-Z0-9\u4E00-\u9FA5]", " ").replaceAll(" +", " ").trim());
		//System.out.println("★测试 旧爱结婚时你会伤心吗 http   sinaurl cn hqEkhq  围观".replaceAll("[^a-zA-Z0-9\u4E00-\u9FA5]", " ").replaceAll(" +", " ").trim());
		//System.out.println("★测试 旧爱结婚时你会伤心吗 http   sinaurl cn hqEkhq  围观".replaceAll("\\p{Punct}", " ").replaceAll(" +", " ").trim());
		
		SentimentalWordExpander1 expander = new SentimentalWordExpander1();
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
	}
	
	public void generateSeedsContextFile(String[] seeds){
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
	
	/**
	 * write to file, maybe serialization.
	 * @param hashMap
	 * @param file
	 */
	public void hashMapToDisk(HashMap<String, Integer> hashMap, File file){
		logger.info("hashMapToDisk -- write to file: {} -- start", file.getName());
		ArrayList<Entry<String, Integer>> array = this.sortHashMap(hashMap);
		try{
			for (Entry<String, Integer> entry : array){
				Files.append(this.entryToString(entry), file, this.charset);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.info("hashMapToDisk -- write to file: {} -- complete", file.getName());
	}
	
	public HashMap<String, Integer> diskToHashMap(File file){
		logger.info("diskToHashMap -- read file: {} -- start", file.getName());
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		try {
			List<String> lines = Files.readLines(file, charset);
			for (String entry : lines){
				String[] keyValuePair = entry.split(",");
				map.put(keyValuePair[0], new Integer(keyValuePair[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("diskToHashMap -- read file: {}, map size: {} -- finish", file.getName(), map.size());
		
		return map;
	}
	
	public String entryToString(Entry<String, Integer> entry){
		return entry.getKey()+","+entry.getValue()+this.lineSeparator;
	}
	
	public HashMap<String, Integer> initContextList(HashSet<String> seeds){
		try {
			File f1 = new File(this.candidateWordsWithinContextDir);
			if (!f1.exists()) f1.mkdir();
			f1 = new File(this.neighborWordsWithinSeedDir);
			if (!f1.exists()) f1.mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.generateSeedsContextFile(seeds.toArray(new String[0]));
		HashMap<String, Integer> contextList = this.diskToHashMap(new File(this.seedContextFileName));
		return contextList;
	}
	
	public void runAll(HashSet<String> seeds){
		HashMap<String, Integer> contextList = this.initContextList(seeds);
		ArrayList<Entry<String, Integer>> array = sortHashMap(contextList);
		
		HashMap<String, Integer> totalCount = new HashMap<String, Integer>();
		for(int i=0;i<array.size();i++){
			Entry<String, Integer> entry = array.get(i);
			String key = entry.getKey();
			Integer value = entry.getValue();
			if (value < THRESHOLD) break;
			HashMap<String, Integer> candidate = extractCandidateWords(key);
			merge(totalCount, candidate);
			logger.info("run merge candidate -- i: {}, value: {}, totalCount size: {}", 
					new Object[]{i, value, totalCount.size()});
		}
		
		ArrayList<Entry<String, Integer>> countArray = sortHashMap(totalCount);
		for (int i=0; i< countArray.size(); i++){
			Entry<String, Integer> entry = countArray.get(i);
			String key = entry.getKey();
			Integer value = entry.getValue();
			if (value < THRESHOLD) break;
			logger.info("run print candidate -- i: {}, key: {}, value: {}", 
					new Object[]{i, key, value});
			
			if (!seeds.contains(key)){
				float similarity = this.evaluateSimilarity(array, this.sortHashMap(this.extractNeighborWords(key)));
				logger.info("run -- similarity between seeds and {} are: {}",
							new Object[]{key, similarity});
			}			
		}
	}
	
	public ArrayList<Entry<String, Integer>> sortHashMap(HashMap<String, Integer> hashMap){
		ArrayList<Entry<String, Integer>> array = new ArrayList<Entry<String, Integer>>(hashMap.entrySet());
		Collections.sort(array, new Comparator<Entry<String, Integer>>(){
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2){
					return o2.getValue() - o1.getValue();
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
		if (context.suffixWord == null && context.prefixWord == null) return null;
		return context;
	}
	
	public boolean isPassedContext(WordContext context){
		return context != null && context.prefixWord != null && context.suffixWord != null;
	}
	
	public HashMap<String, Integer> extractNeighborWords(String word){
		logger.info("extractNeighborWords -- word: {} start...", word);
		File file = new File(this.getSeedWordFileName(word));
		HashMap<String, Integer> contextList = null;
		if (!file.exists()){
			Date start = new Date();
			TopDocs results = contentSearcher.searchWord(word, MAX_SIZE);
			Date currentTime = new Date();
			logger.info("search seed: {}, statuses' count: {}, cost time: {} total milliseconds", 
					new Object[]{word, results.totalHits, currentTime.getTime() - start.getTime()});
			
			int number = (results.totalHits>MAX_SIZE)?MAX_SIZE:results.totalHits;
			contextList = new HashMap<String, Integer>(number);
			for (int i = 0; i < number; i++){
				String content = contentSearcher.getDocContent(results.scoreDocs[i].doc);
				WordContext context = this.extractWordContext(content, word);
				if (this.isPassedContext(context)){
					Integer previous = contextList.get(context.toString());
					previous = previous==null?1:(previous+1);
					contextList.put(context.toString(), previous);
				}
			}
			start = currentTime;
			currentTime = new Date();
			logger.info("extractNeighborwords seed word: {}, size: {}, cost time: {} total milliseconds",
					new Object[]{word, contextList.size(), currentTime.getTime() - start.getTime()});
			try {
				file.createNewFile();
				this.hashMapToDisk(contextList, file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			contextList = this.diskToHashMap(file);
		}
		return contextList;
	}
	
	public String getNeighborWordFileName(String neighborWord){
		return this.candidateWordsWithinContextDir + File.separatorChar + neighborWord + this.suffixOfFile;
	}
	
	public String getSeedWordFileName(String seed){
		return this.neighborWordsWithinSeedDir + File.separatorChar + seed + this.suffixOfFile;
	}
	
	public HashMap<String, Integer> extractCandidateWords(String neighborWord){
		logger.info("extractCandidateWords word: {} -- start...", neighborWord);
		if (neighborWord.trim().length() < 2) return null;
		File file = new File(this.getNeighborWordFileName(neighborWord));
		HashMap<String, Integer> candidateWords = null;
		if (!file.exists()){
			TopDocs results = contentSearcher.searchContext(neighborWord, MAX_SIZE);
			logger.info("search content count: {} -- neighborWord: {}", results.totalHits, neighborWord);
			
			String regex = neighborWord.charAt(0) + "([a-zA-Z\u4e00-\u9FA5&&[^"+neighborWord.charAt(0)+"]]{1,4}?)" + neighborWord.charAt(2);
			Pattern pattern = Pattern.compile(regex);
			int number = (results.totalHits>MAX_SIZE)?MAX_SIZE:results.totalHits;
			candidateWords = new HashMap<String, Integer>(this.MAX_SIZE);
			for (int i = 0; i < number; i++){
				String content = contentSearcher.getDocContent(results.scoreDocs[i].doc);			
				Matcher matcher = pattern.matcher(content);
				while (matcher.find()){
					String candidate = matcher.group(1);
					Integer previous = candidateWords.get(candidate);
					previous = previous==null?1:(previous+1);
					candidateWords.put(candidate, previous);
				}
			}
			logger.info("extract candidateWords -- neighborWord: {}, size: {}", neighborWord, candidateWords.size());
			this.hashMapToDisk(candidateWords, file);
		}else{
			candidateWords = this.diskToHashMap(file);
		}
		return candidateWords;
	}
	
	public float evaluateSimilarity(ArrayList<Entry<String, Integer>> seedNeighbors, ArrayList<Entry<String, Integer>> candidateNeighbors){
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
			Entry<String, Integer> entry = seedNeighbors.get(i);
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
		return (float)borda1 / (borda2 * borda3);
	}
	
	public float evaluateSimilarity(HashMap<String, Integer> seedNeighbors, HashMap<String, Integer> candidateNeighbors){
		return 0;
	}
	
	//@Autowired private ContentSearcher contentSearcher;
}
