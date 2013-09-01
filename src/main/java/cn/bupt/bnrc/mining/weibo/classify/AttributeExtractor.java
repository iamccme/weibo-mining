package cn.bupt.bnrc.mining.weibo.classify;

import java.util.List;

import kevin.zhang.NLPIR;
import cn.bupt.bnrc.mining.weibo.util.NLPIRTools;

/*********************************************************************
 * 
 * Class Name : AttributeExtractor
 * 
 * Description: 属性提取器
 * 
 * Parameters : None Returns : boolean, true:success, false:fail
 * 
 * Author : Cheng chen : 1.create 2013/3/23
 *********************************************************************/


public class AttributeExtractor {

	private Lexicon lexicon = null;
	private NLPIR nlpir = NLPIR.getInstance();
	
	public static int ATTRIBUTE_COUNT = 11;
	
	public static int IS_CONTAIN_SEGMENT_WORD = 0;      //是否包含自定义的词
	public static int POSITIVE_WORD_COUNT = 1;          //积极词语的数目
	public static int NEGATIVE_WORD_COUNT = 2;          //消极词语的数目
	public static int IS_CONTAIN_EMOTICON = 3;          //是否包含表情符号
	public static int POSITIVE_EMOTICON_COUNT = 4;      //积极表情符号数目
	public static int NEGATIVE_EMOTICON_COUNT = 5;      //消极表情符号数目
	public static int IS_CONTAIN_AD_WORD = 6;           //是否包含广告词
	public static int AD_WORD_COUNT = 7;                //广告词数目
	public static int IS_CONTAIN_WT = 8;
	public static int IS_CONTAIN_E = 9;
	public static int IS_CONTAIN_Y = 10;
	
	public AttributeExtractor(){
		lexicon = Lexicon.getInstance();
		initSentimentWords();                                           //初始化词表
	}
	
	public void initSentimentWords(){		
		List<String> positiveWords = lexicon.getPositiveWords(1500);    //得到1500个积极的词
		List<String> negativeWords = lexicon.getNegativeWords(1000);    //得到1000个消极的词
		try{
			for (String positiveWord : positiveWords){
				nlpir.NLPIR_AddUserWord((positiveWord+" qgcp").getBytes(NLPIR.userWordCharset));
			}
			for (String negativeWord : negativeWords){
				nlpir.NLPIR_AddUserWord((negativeWord+" qgcn").getBytes(NLPIR.userWordCharset));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 从微博中提取特征值。
	 * @TODO 这里的返回值有问题，面向对象的实现，返回值最好应该是对象吧。
	 * @param content
	 * @return 属性向量
	 */
	public int[] extractor(String content){
		int[] attributeValues = new int[ATTRIBUTE_COUNT];
		for (int i = 0; i < ATTRIBUTE_COUNT; i++){
			attributeValues[i] = 0;
		}
		
		List<String> segmentedWords = NLPIRTools.segmentSentenceWithPOSTaged(content);  //对微博内容进行切词
		
		for (String segmentedWord : segmentedWords){
			//System.out.println(segmentedWord);
			if (segmentedWord.contains("/qgcp")){
				attributeValues[POSITIVE_WORD_COUNT]++;
			}else if(segmentedWord.contains("/qgcn")){
				attributeValues[NEGATIVE_WORD_COUNT]++;
			}else if(segmentedWord.contains("/a")){
				attributeValues[AD_WORD_COUNT]++;
			}else if(segmentedWord.contains("/e")){
				attributeValues[IS_CONTAIN_E] = 1;
			}else if(segmentedWord.contains("/wt")){
				attributeValues[IS_CONTAIN_WT] = 1;
			}
		}
		if (attributeValues[POSITIVE_WORD_COUNT] != 0 || attributeValues[NEGATIVE_WORD_COUNT] != 0){
			attributeValues[IS_CONTAIN_SEGMENT_WORD] = 1;
		}
		if (attributeValues[AD_WORD_COUNT] != 0){
			attributeValues[IS_CONTAIN_AD_WORD] = 1;
		}
		
		List<String> containedEmoticons = Emoticons.extractEmoticonsFromContent(content);
		if (!containedEmoticons.isEmpty()){
			attributeValues[IS_CONTAIN_EMOTICON] = 1;
			for (String emoticon : containedEmoticons){
				if (Emoticons.isPositiveEmoticon(emoticon)){
					attributeValues[POSITIVE_EMOTICON_COUNT]++;
				}else if (Emoticons.isNegativeEmoticon(emoticon)){
					attributeValues[NEGATIVE_EMOTICON_COUNT]++;
				}
			}
		}
		
		return attributeValues;
	}
}
