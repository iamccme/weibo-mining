package cn.bupt.bnrc.mining.weibo.weka;


public class Constants {

	public static String wordCountInTotalFileName = "word-count.txt";
	public static String contextCountInTotalFileName = "neighbor-word-count.txt";
	
	public static String contextIndicationFileName = "context-indication.txt";
	public static String wordIndicationFileName = "word-indication.txt";
	public static String wordUnfitnessFileName = "word-unfitness.txt";
	
	public static String wordRankingFileName = "word-ranking.txt";
	
	public static String candidateWordsWithinContextDir = "candidateWords";
	public static String neighborWordsWithinSeedDir = "neighborWords";
	
	public static String contextSeedsFileName = "context-seeds.txt";
	
	public static String lineSeparator = System.getProperty("line.separator");
	public static String suffixOfFile = ".txt";
	
	public static String IP = "59.64.156.84";
	public static int PORT = 6379;
	
	public static final double log2 = Math.log(2.0);
	public static double sampleSpaceSize = 4000000.0;
	public static double logSampleSpaceSize = Math.log(sampleSpaceSize)/log2;
	public static final int wordsPerStatus = 140;
}
