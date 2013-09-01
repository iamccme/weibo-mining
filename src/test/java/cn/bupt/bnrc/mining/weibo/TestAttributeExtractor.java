package cn.bupt.bnrc.mining.weibo;

import org.junit.Test;

import cn.bupt.bnrc.mining.weibo.classify.AttributeExtractor;

public class TestAttributeExtractor {

	@Test
	public void testUserDict() {
		AttributeExtractor extractor = new AttributeExtractor();
		extractor.initSentimentWords();
		int[] vector = extractor.extractor("@落花人间 同学在香港金像奖的作品三：100418.香港金像奖.李宇春.庆功宴 贵小姐，这条表扬你！" +
						"！！在踩着10厘米以上的高跟鞋，站了1天的情况下，还能快速在宴会厅和记者席之间穿梭。。。。乃下次可以做的更好哈。。。。[花] http://sinaurl.cn/h1nUj");
		for (int element : vector){
			System.out.println(element);
		}
	}

}
