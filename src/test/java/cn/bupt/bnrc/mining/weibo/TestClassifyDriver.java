package cn.bupt.bnrc.mining.weibo;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import cn.bupt.bnrc.mining.weibo.classify.ClassifyDriver;

public class TestClassifyDriver {

	private static ApplicationContext ac;
	private static String[] path={"classpath:/spring/servlet-config.xml",
		"classpath:/mybatis/sqlserver-jdbc-context.xml"};
	
	@Before
	public void init(){
		ac = new FileSystemXmlApplicationContext(path);
	}
	
	@Test
	public void testRun(){
		ClassifyDriver driver = (ClassifyDriver)ac.getBean("classifyDriver");
		driver.run();
	}
}
