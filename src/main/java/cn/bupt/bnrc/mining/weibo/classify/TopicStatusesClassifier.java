package cn.bupt.bnrc.mining.weibo.classify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TopicStatusesClassifier {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public Map<String, Object> classifyTopicStatuses(List<String> topicRelatedStatuses){
		int objectiveNum = 0;
		int negativeNum = 0;
		int positiveNum = 0;
		
		List<String> objectiveStatuses = new ArrayList<String>();
		List<String> negativeStatuses = new ArrayList<String>();
		List<String> positiveStatuses = new ArrayList<String>();
		for (String status : topicRelatedStatuses){
			if (subObjClassifier.isSubjective(status)){
				double score = polarityClassifier.classify(status);
				if (score > 0) {
					positiveNum++;
					positiveStatuses.add(status);
					logger.info(String.format("positive: %s---score:%.5f", status, score));
				}
				else {
					negativeNum++;
					negativeStatuses.add(status);
					logger.info(String.format("negative: %s---score:%.5f", status, score));
				}
			}else{
				objectiveNum++;
				objectiveStatuses.add(status);
				logger.info(String.format("objective: %s", status));
			}
		}
		
		Map<String, Object> statistics = new HashMap<String, Object>();
		statistics.put("positiveStatuses", positiveStatuses);
		statistics.put("negativeStatuses", negativeStatuses);
		statistics.put("objectiveStatuses", objectiveStatuses);
		statistics.put("positiveNum", positiveNum);
		statistics.put("negativeNum", negativeNum);
		statistics.put("objectiveNum", objectiveNum);
		statistics.put("totalStatuses", topicRelatedStatuses.size());
		
		return statistics;
	}
	
	public Map<String, Object> classifyTopic(String topicString){
		List<String> statuses = this.getTopicRelatedStatuses(topicString);
		return this.classifyTopicStatuses(statuses);
	}
	
	public List<String> getTopicRelatedStatuses(String topicString){
		return null;
	}
	
	public List<String> getTopicRelatedStatuses(String[] keyWords){
		return null;
	}
	
	private PolarityClassifier polarityClassifier = new PolarityClassifier();
	private SubObjClassifier subObjClassifier = new SubObjClassifier();
}
