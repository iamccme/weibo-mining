package cn.bupt.bnrc.mining.weibo.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kevin.zhang.NLPIR;

public class NLPIRTools {

	private static NLPIR nlpir = NLPIR.getInstance();
	
	private static List<String> removeLastElement(List<String> original){
		List<String> result = new ArrayList<String>(original);
		int lastIndex = result.size() - 1;
		String lastSegment = result.get(lastIndex);
		if (lastSegment.length()==1 && lastSegment.equals("\0")) 
			result.remove(lastIndex);
		return result;
	}

	public static String convertByNLPIR(String content) {
		if (nlpir != null) {
			try {
				byte[] nativeBytes = nlpir.NLPIR_ParagraphProcess(
						content.getBytes(NLPIR.charset),1);
				String convertedString = new String(nativeBytes, 0,
						nativeBytes.length, NLPIR.charset);
				return convertedString;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	//the last element is invalid, just act as the flag of end of the words
	public static List<String> segmentSentenceWithPOSTaged(String content) {
		if (nlpir != null) {
			try {
				byte[] nativeBytes = nlpir.NLPIR_ParagraphProcess(
						content.getBytes(NLPIR.charset), 1);
				String segment = new String(nativeBytes, 0, nativeBytes.length,
						NLPIR.charset);
				List<String> result = Arrays.asList(segment.split(" +"));
				return NLPIRTools.removeLastElement(result);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	//the last element is invalid, just act as the flag of end of the words
	public static List<String> segmentSentenceWithoutPOSTaged(String content) {
		if (nlpir != null) {
			try {
				byte[] nativeBytes = nlpir.NLPIR_ParagraphProcess(
						content.getBytes(NLPIR.charset), 0);
				String segment = new String(nativeBytes, 0, nativeBytes.length,
						NLPIR.charset);
				List<String> result = Arrays.asList(segment.split(" +"));
				return NLPIRTools.removeLastElement(result);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static void main(String[] args) throws Exception{
		String str = "北邮网络技术研究院宽带网研究中心有多少为老师";

		List<String> result = NLPIRTools.segmentSentenceWithPOSTaged(str);
		System.out.println(result.size());
		for (String s : result){
			System.out.println(s);
		}
	}

	public static class STResult {

		public int start; // start position,词语在句子中的开始位置
		public int length; // length,词语长度
		//public char sPos[8]; //word type, 词性id值，可以快速的获取词性表
		public int iPOS; // POS, 词性id
		public int wordID; // word_ID, 词语id
		public int wordType; // Is the word of the user's dictionary?(0-no,1-yes) 查看词语是否是用户词典中词语
		public int weight; // word weight,词语权重
		
		@Override
		public String toString(){
			return "start: "+start+",length: "+length+",iPos: "+iPOS+",wordId: "+wordID+",wordType: "+wordType+",weight: "+weight;
		}
	}
}
