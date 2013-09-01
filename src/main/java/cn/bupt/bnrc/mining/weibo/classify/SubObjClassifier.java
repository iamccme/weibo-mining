package cn.bupt.bnrc.mining.weibo.classify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubObjClassifier {

	public static final String subKey = "1";
	public static final String objKey = "0";
	
	public static final int SUBJECTIVE = 1;
	public static final int OBJECTIVE = 0;
	
	public int classify(String status){
		if (this.isSubjective(status)){
			return SUBJECTIVE;
		}else{
			return OBJECTIVE;
		}
	}
	
	public Map<String, List<String>> classify(List<String> statuses){
		Map<String, List<String>> subObjMap = new HashMap<String, List<String>>(2);
		subObjMap.put(subKey, new ArrayList<String>());
		subObjMap.put(objKey, new ArrayList<String>());
		
		if (statuses == null) return null;
		for (String status : statuses){
			if (this.isSubjective(status)){
				subObjMap.get(subKey).add(status);
			}else{
				subObjMap.get(objKey).add(status);
			}
		}
		
		return subObjMap;
	}
	
	public List<String> filterObjective(List<String> statuses){
		List<String> subjectiveStatuses = new ArrayList<String>(statuses.size() * 3/4);
		for (String status : statuses){
			if (this.isSubjective(status)){
				subjectiveStatuses.add(status);
			}
		}
		return subjectiveStatuses;
	}
	
	public boolean isSubjective(String content){
		int[] attributes = extractor.extractor(content);
		if (attributes[AttributeExtractor.IS_CONTAIN_SEGMENT_WORD] == 0 
				&& attributes[AttributeExtractor.IS_CONTAIN_EMOTICON] == 0){
			return false;
		}
		return true;
	}
	
	private AttributeExtractor extractor = new AttributeExtractor();
}
