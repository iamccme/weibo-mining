package cn.bupt.bnrc.mining.weibo.db;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public interface StatusesDao {

	public List<Map<String, Object>> getNextPageStatuses(Map<String, Object> params);
	
	public List<Map<String, Object>> getFirstPageStatuses(Map<String, Object> params);
}
