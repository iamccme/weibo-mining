package cn.bupt.bnrc.mining.weibo.sentiment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.db.StatusesDao;

@Service
public class WordExpandDispatcher {

	public static int MIN_STATUS_SIZE = 100000;
	
	public static void main(String[] args){
		new WordExpandDispatcher().dispatch();
	}
	
	public void dispatch(){
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("page_offset", 1);
		params.put("page_size", 100000);
		List<Map<String, Object>> statuses = statusesDao.getNextPageStatuses(params);
		new SentimentWordExpander().expanding(null, "index-20121108-1", statuses, 2);
	}
	
	@Autowired
	private StatusesDao statusesDao;
}
