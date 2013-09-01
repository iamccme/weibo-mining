package cn.bupt.bnrc.mining.weibo.classify;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;

@Service
public class ClassifyDriver {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public void run(){
		List<String> statuses = statusesConvertor.readStatusesFromDatabase(10000);
		statuses = subObjClassifier.filterObjective(statuses);
		Instances data = statusesConvertor.writeARFFFile("data", statuses);
		SimpleKMeans cluster = new SimpleKMeans();

		try {
			cluster.setNumClusters(5);
			cluster.buildClusterer(data);
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(cluster);
			eval.evaluateClusterer(data);
			System.out.println(eval.clusterResultsToString());
			for (String status : statuses){
				int[] attributes = extractor.extractor(status);
				double[] values = new double[attributes.length];
				for (int i = 0, l = attributes.length; i < l; i++) values[i] = attributes[i];
				Instance instance = new Instance(1.0, values);
				int clusterId = cluster.clusterInstance(instance);
				logger.info(String.format("%s$$$$$%s&&&&&%d", status, instance.toString(), clusterId));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private AttributeExtractor extractor = new AttributeExtractor();
	private SubObjClassifier subObjClassifier = new SubObjClassifier();
	@Autowired private StatusesConvertor statusesConvertor;
}
