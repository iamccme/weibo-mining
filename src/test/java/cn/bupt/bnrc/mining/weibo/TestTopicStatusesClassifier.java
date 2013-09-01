package cn.bupt.bnrc.mining.weibo;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import cn.bupt.bnrc.mining.weibo.classify.StatusesConvertor;
import cn.bupt.bnrc.mining.weibo.classify.TopicStatusesClassifier;

public class TestTopicStatusesClassifier {

	private static ApplicationContext ac;
	private static String[] path={"classpath:/spring/servlet-config.xml",
		"classpath:/mybatis/sqlserver-jdbc-context.xml"};
	
	@Before
	public void init(){
		ac = new FileSystemXmlApplicationContext(path);
	}
	
	@Test
	public void testClassifyTopic(){
		StatusesConvertor convertor = (StatusesConvertor)ac.getBean("statusesConvertor");
		TopicStatusesClassifier classifier = new TopicStatusesClassifier();
		classifier.classifyTopicStatuses(convertor.readStatusesFromDatabase(10000));
	}
}
