package cn.bupt.bnrc.mining.weibo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import cn.bupt.bnrc.mining.weibo.db.UserDao;
import cn.bupt.bnrc.mining.weibo.mysqldb.SentimentWordDao;
import cn.bupt.bnrc.mining.weibo.search.ContentIndexer;
import cn.bupt.bnrc.mining.weibo.search.ContentIndexer2;
import cn.bupt.bnrc.mining.weibo.search.ContentSearcher;
import cn.bupt.bnrc.mining.weibo.search.EmoticonContentIndexer;
import cn.bupt.bnrc.mining.weibo.sentiment.test.TestPageRankInSentimentWord;
import cn.bupt.bnrc.mining.weibo.tagging.DataPreprocessor;
import cn.bupt.bnrc.mining.weibo.weka.backup.SentimentalWordExpander2;

public class DBTest {

	private static ApplicationContext ac;
	private static String[] path={"classpath:/spring/servlet-config.xml",
		"classpath:/mybatis/sqlserver-jdbc-context.xml"};
	
	@Before
	public void init(){
		ac = new FileSystemXmlApplicationContext(path);
	}
	
	@Test
	public void testAddStatuses(){
		String filePath = DataPreprocessor.class.getClassLoader().getResource("").getPath()
				+"/data/jinzhengri.txt".substring(1);
		DataPreprocessor p = (DataPreprocessor)ac.getBean("dataPreprocessor");
		p.addTopicRelatedStatusesFromFile(filePath);
	}
	
	//@Test
	public void dispatcher(){
		TestPageRankInSentimentWord expander = new TestPageRankInSentimentWord();
		expander.expanding(expander.buildSeedSet(), "index", null, 3);
	}
	
	//@Test
	public void testMySQLConnection(){
		SentimentWordDao wordDao = (SentimentWordDao)ac.getBean("sentimentWordDao");
		List<Map<String, Object>> words = wordDao.getAllSentimentWords();
		System.out.println(words.size() == 0);
	}
	
	//@Test
	public void testDBConnection() {
		UserDao userDao = (UserDao)ac.getBean("userDao");
		Map<String, Object> user = userDao.getUserInfoById(10457);
		System.out.println(user==null);
	}
	
	//@Test
	public void testIndexStatus2(){
		ContentIndexer2 indexer = (ContentIndexer2)ac.getBean("contentIndexer2");
		indexer.indexStatus();
	}
	
	//@Test
	public void testStatisticEmoticons(){
		ContentIndexer2 indexer = (ContentIndexer2)ac.getBean("contentIndexer2");
		indexer.statisticEmoticons();
	}
	
	//@Test
	public void testIndexStatusContainEmoticons(){
		EmoticonContentIndexer indexer = (EmoticonContentIndexer)ac.getBean("emoticonContentIndexer");
		indexer.indexStatusesContainEmoticons();
	}
	
	//@Test
	public void testIndexStatus(){
		ContentIndexer indexer = (ContentIndexer)ac.getBean("contentIndexer");
		indexer.indexStatus(4000000);
	}
	
	//@Test
    public void testAll(){                                                                                                                                                      
        SentimentalWordExpander2 expander = new SentimentalWordExpander2();
        HashSet<String> seeds = new HashSet<String>();
        seeds.add("高兴");
        seeds.add("郁闷");
        seeds.add("烂");
        seeds.add("美好");
        seeds.add("喜欢");
        expander.runAll(seeds);
    }

	//@Test
	public void testGetOccurrence(){
		ContentSearcher searcher = (ContentSearcher)ac.getBean("contentSearcher");
		System.out.println(searcher.getCooccurrence("微", "博", 1));
	}
}
