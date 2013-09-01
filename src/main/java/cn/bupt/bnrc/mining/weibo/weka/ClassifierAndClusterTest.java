package cn.bupt.bnrc.mining.weibo.weka;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class ClassifierAndClusterTest {
	public static void main(String[] args){
		ClassifierAndClusterTest ct = new ClassifierAndClusterTest();
		//ct.testCluster();
		ct.testClassifier();
	}
	public void testClassifier(){
		Instances data = getInstances();
		Classifier cModel = (Classifier)new NaiveBayes();
		try {
			cModel.buildClassifier(data);
			Evaluation eTest = new Evaluation(data);
			eTest.evaluateModel(cModel, data);
			String strSummary = eTest.toSummaryString();
			System.out.println(strSummary);

			
			Instance oneInstance = new Instance(5);
			oneInstance.setValue(0, 5);
			oneInstance.setValue(1, 3);
			oneInstance.setValue(2, 1);
			oneInstance.setValue(3, 0.2);
			
			oneInstance.setDataset(data);
			double[] fDistribution = cModel.distributionForInstance(oneInstance);
			for (int i = 0; i < fDistribution.length; i++){
				System.out.print(fDistribution[i]+", ");
			}
			System.out.println();
			
			double label = cModel.classifyInstance(oneInstance);
			System.out.println("label: "+label);
			oneInstance.setClassValue(label);
			System.out.println(oneInstance.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testCluster(){
		Instances data = getInstances();
		EM clusterer = new EM();
		try{
			clusterer.buildClusterer(data);
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(data);
			System.out.println(eval.clusterResultsToString());
			System.out.println(eval.getNumClusters());
			
			Instance oneInstance = new Instance(2);
			oneInstance.setValue(0, 456);
			oneInstance.setValue(1, 532);
			
			System.out.println(clusterer.clusterInstance(oneInstance));
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public Instances getInstances(){
		DataSource dataSource;
		String path = ClassifierAndClusterTest.class.getClassLoader().getResource("").getPath().substring(1) + "/uci-data/";
		try {
			dataSource = new DataSource(path+ "/iris.arff");
			Instances data = dataSource.getDataSet();
			
			if (data.classIndex() == -1){
				data.setClassIndex(data.numAttributes()-1);
			}
			return data;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
