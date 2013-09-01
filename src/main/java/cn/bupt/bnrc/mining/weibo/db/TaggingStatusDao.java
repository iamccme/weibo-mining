package cn.bupt.bnrc.mining.weibo.db;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public interface TaggingStatusDao {

	/**
	 * params: {content:xxxx, tagging_flag:1}
	 * @param params
	 */
	public void addUntaggedStatus(Map<String, Object> params);
	
	/**
	 * params: {page_size:1, tagging_flag:0}
	 * @param params
	 * @return
	 */
	public List<Map<String, Object>> getFirstPageStatuses(Map<String, Object> params);
	
	/**
	 * params: {page_size:1, page_offset:100, tagging_flag:0}
	 * @param params
	 * @return
	 */
	public List<Map<String, Object>> getNextPageStatuses(Map<String, Object> params);
	
	/**
	 * params: {id:1, tagging_flag=1}
	 * @param params
	 */
	public void updateStatusFlag(Map<String, Object> params);
	
	public int getTotalCount(Map<String, Object> params);
}
