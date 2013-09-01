package cn.bupt.bnrc.mining.weibo.mysqldb;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public interface SentimentWordDao {
	
	public List<Map<String, Object>> getAllSentimentWords();
}
