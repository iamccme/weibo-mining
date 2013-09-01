package cn.bupt.bnrc.mining.weibo.db;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

/**
 * 该类不要使用！！！是一个测试所用的类
 * @author hsgui
 *
 */
@Repository
public interface StatusesContentDao {

	public List<Map<String, Object>> getTopNStatusContent(Map<String, Object> params);
	
	public void deleteTopNStatusContent(Map<String, Object> params);
}
