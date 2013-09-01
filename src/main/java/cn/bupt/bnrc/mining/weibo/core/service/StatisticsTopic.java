package cn.bupt.bnrc.mining.weibo.core.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class StatisticsTopic {

	public Map<String, Object> statisticPosNegPercentBetween(String topic, Date start, Date end){
		Map<String, Object> percents = new HashMap<String, Object>();
		percents.put("positivePercent", 0.3);
		percents.put("negativePercent", 0.7);
		
		return percents;
	}
	
	public Map<String, Object> statisticPosNegNumBetween(String topic, Date start, Date end){
		Map<String, Object> nums = new HashMap<String, Object>();
		nums.put("positiveNum", 300);
		nums.put("negativeNum", 700);
		
		return nums;
	}
	
	/**
	 * 
	 * @param topic
	 * @param start
	 * @param end
	 * @return
	 */
	public List<Map<String, Object>> statisticTotalPosNegNumTrend(String topic, Date start, Date end){
		List<Map<String, Object>> numsTrend = new ArrayList<Map<String, Object>>();
		
		return numsTrend;
	}
}
