package cn.bupt.bnrc.mining.weibo.classify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import cn.bupt.bnrc.mining.weibo.db.StatusesDao;

@Service
public class StatusesConvertor {
	private Logger logger = LoggerFactory.getLogger(getClass());

	public List<String> readStatusesFromDatabase(int num){
		logger.debug(String.format("read statuses from database, num: %d", num));
		
		List<String> resultStatuses = new ArrayList<String>();
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("firstPageSize", num);
		List<Map<String, Object>> statuses = statusesDao.getFirstPageStatuses(params);
		for (Map<String, Object> status : statuses){
			resultStatuses.add((String)status.get("content"));
		}
		return resultStatuses;
	}
	
	public Instances writeARFFFile(String filename, List<String> statuses){
		FastVector atts;
		Instances data;
		atts = new FastVector();
		atts.addElement(new Attribute("isContainSegmentWord"));
		atts.addElement(new Attribute("positiveWordCount"));
		atts.addElement(new Attribute("negativeWordCount"));
		atts.addElement(new Attribute("isContainEmoticon"));
		atts.addElement(new Attribute("positiveEmoticonCount"));
		atts.addElement(new Attribute("negativeEmoticonCount"));
		atts.addElement(new Attribute("isContainADWord"));
		atts.addElement(new Attribute("ADWordCount"));
		atts.addElement(new Attribute("isContianWT"));
		atts.addElement(new Attribute("isContainE"));
		atts.addElement(new Attribute("isContainY"));
		
		data = new Instances("statuses", atts, statuses.size());
		for (String status : statuses){
			int[] attributes = extractor.extractor(status);
			double[] values = new double[attributes.length];
			for (int i = 0, l = attributes.length; i < l; i++) values[i] = attributes[i];
			data.add(new Instance(1.0, values));
		}
		return data;
	}
	
	@Autowired private StatusesDao statusesDao;
	private AttributeExtractor extractor = new AttributeExtractor();
}
