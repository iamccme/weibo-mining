package cn.bupt.bnrc.mining.weibo.sentiment.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import cn.bupt.bnrc.mining.weibo.sentiment.SentimentWordExpander;
import cn.bupt.bnrc.mining.weibo.util.Utils;
import cn.bupt.bnrc.mining.weibo.weka.Constants;

public class TestPageRankInSentimentWord extends SentimentWordExpander{

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public void runAll(HashSet<String> seeds){
		//this.getSeedsContext(seeds);
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
		candidateWordSize = Math.min(candidateWordSize, 100);
		
		double[][] cs = new double[array.size()][candidateWordSize];
		double[][] csT = new double[candidateWordSize][array.size()];
		for (int contextIndex = 0; contextIndex < array.size(); contextIndex++){
			for (int wordIndex = 0; wordIndex < candidateWordSize; wordIndex++){
				String neighborWord = array.get(contextIndex).getKey();
				String word = candidateWords.get(wordIndex).getKey();
				int contextCountInTotal = this.getContextCountInTotal(Constants.contextCountInTotalFileName, neighborWord);
				int wordCountInTotal = this.getWordCountInTotal(Constants.wordCountInTotalFileName, word);
				int wordAndContextCountInTotal = this.contentSearcher.searchWord(neighborWord.charAt(0)+word+neighborWord.charAt(2), 1000).totalHits;
				wordAndContextCountInTotal = wordAndContextCountInTotal==0?1:wordAndContextCountInTotal;
				cs[contextIndex][wordIndex] = this.indicationBetweenWordAndContext(wordCountInTotal, wordAndContextCountInTotal, contextCountInTotal);
				csT[wordIndex][contextIndex] = cs[contextIndex][wordIndex];
			}
		}
		
		for (int wordIndex = 0; wordIndex < candidateWordSize; wordIndex++){
			double sum = 0;
			for (int contextIndex = 0; contextIndex < array.size(); contextIndex++){
				sum += cs[contextIndex][wordIndex];
			}
			for (int contextIndex = 0; contextIndex < array.size(); contextIndex++){
				cs[contextIndex][wordIndex] /= sum;
			}
		}
		
		for (int contextIndex = 0; contextIndex < array.size(); contextIndex++){
			double sum = 0;
			for (int wordIndex = 0; wordIndex < candidateWordSize; wordIndex++){
				sum += csT[wordIndex][contextIndex];
			}
			for (int wordIndex = 0; wordIndex < candidateWordSize; wordIndex++){
				csT[wordIndex][contextIndex] /= sum;
			}
		}

		File sFile = new File("s");
		File cFile = new File("c");;
		File csFile = new File("cs");
		File csTFile = new File("csT");
		try{
			sFile.createNewFile();
			cFile.createNewFile();
			csFile.createNewFile();
			csTFile.createNewFile();
			
			for (int i = 0; i < array.size(); i++){
				Files.append(array.get(i).getKey(), cFile, Charset.forName("utf-8"));
			}
			
			for (int i = 0; i < candidateWordSize; i++){
				Files.append(candidateWords.get(i).getKey(), sFile, Charset.forName("utf-8"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		printMatrix(cs, array.size(), candidateWordSize, csFile);
		printMatrix(csT, candidateWordSize, array.size(), csTFile);
		double[] s = new double[candidateWordSize];
		for (int i = 0; i < candidateWordSize; i++){
			s[i] = 1/(double)candidateWordSize;
		}
		double [] c = new double[array.size()];
		for (int i = 0; i< 10; i++){
			matrixMulVector(cs, s, c);
			matrixMulVector(csT,c,s);
			printVector(c,cFile);
			printVector(s,sFile);
		}
	}
	
	public void printVector(double[] vector, File file){
		try{
			Files.append("\nvector is\n", file, Charset.forName("utf-8"));
			for (int i=0;i<vector.length;i++){
				Files.append(vector[i]+" ", file, Charset.forName("utf-8"));
			}
			Files.append("\n", file, Charset.forName("utf-8"));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void matrixMulVector(double[][] matrix, double[] vector, double[] result){
		for (int i = 0; i < result.length; i++){
			double sum = 0;
			for (int j = 0; j < vector.length; j++){
				sum += matrix[i][j]*vector[j];
			}
			result[i] = sum;
		}
	}
	public void printMatrix(double[][] matrix, int hang, int lie, File file){
		try {
			Files.append("\nmatrix is:\n", file, Charset.forName("utf-8"));
			for (int i = 0; i < hang; i++){
				for (int j = 0; j < lie; j++){
					Files.append(matrix[i][j]+" ", file, Charset.forName("utf-8"));
				}
				Files.append("\n", file, Charset.forName("utf-8"));
			}
			Files.append("\n", file, Charset.forName("utf-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
