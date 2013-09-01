package cn.bupt.bnrc.mining.weibo.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class StatisticsTopicController {

	@RequestMapping(value="statistic")
	public String statisticTopicSentiment(){
		return "core/statistics";
	}
}
