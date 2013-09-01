package cn.bupt.bnrc.mining.weibo.weka.backup;

public class WordContext {

	public String prefixWord = null;
	public String suffixWord = null;
	
	public String toString(){
		return (prefixWord==null?" ":prefixWord) + (suffixWord==null?" ":(prefixWord==null?suffixWord:(" " + suffixWord)));
	}
}
