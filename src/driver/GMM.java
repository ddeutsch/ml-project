package driver;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;

import data.Cluster;
import data.Multinomial;

public class GMM 
{
	private static String inputFile = "output/theta";
	
	private static ArrayRealVector[] data;
	private static int[] Z = null;
	private static Map<Integer, Cluster> clusters = new HashMap<Integer, Cluster>();
	
	// dimension
	private static int N;
	private static int D;
	private static int K;
	
	public static ArrayRealVector baseMean;
	private static Array2DRowRealMatrix baseCovariance;

	private static double alpha = 0.5;
	
	private static MultivariateNormalDistribution baseDistribution;
	private static int iterations = 1000;
	
	public static void main(String[] args) 
	{
		readData();
		
		//initialize mean to [0,...0]
		baseMean = new ArrayRealVector(D);
		//initialize covariance to identity
		baseCovariance = new Array2DRowRealMatrix(identityMatrix(D)); 
		
		baseDistribution = new MultivariateNormalDistribution(baseMean.getDataRef(),baseCovariance.getData());
		
		// add first cluster
		Cluster c = new Cluster(baseMean, baseCovariance, D);
		clusters.put(c.id, c);
		K = 1;
		
		// add all points to 0th cluster
		for (int i = 0; i < data.length; i++)
			c.add(i, data[i]);
		
		for (int t = 0; t < iterations; t++)
		{
			System.out.println(t + ": " + clusters.size());
			System.out.println(likelihood());
			for (int n = 0; n < N; ++n) {
	 			Cluster curCluster = clusters.get(Z[n]); // SLOW 
				curCluster.remove(n,data[n]); // SLOW
				
				if (curCluster.points.size() == 0)
					clusters.remove(Z[n]);
				
				List<Integer> clusterOrder = new ArrayList<Integer>();
				double[] probs = new double[clusters.size() + 1];
				int i = 0;
				for (int key : clusters.keySet()) {
					clusterOrder.add(key);
					probs[i] = (((double) ( curCluster.points.size() )) / ( alpha + N - 1)) * curCluster.pdf(data[n]); 
				}
				
				//MAP ESTIMATE OF THE PRIOR. NOT EVALUATING PRIOR PREDICTIVE
				probs[clusters.size()] = ( ( double ) alpha / (alpha + N - 1 )) * baseDistribution.density(data[n].getDataRef());
				
				int sample = new Multinomial(probs).sample();
				//new cluster
				if (sample == clusters.size()) {

					Cluster cluster = new Cluster(baseMean, baseCovariance, D);
					cluster.add(n, data[n]);
					clusters.put(cluster.id, cluster);
				} else {
					//old cluster
					int clusterId = clusterOrder.get(sample);
					Cluster newCluster = clusters.get(clusterId);
					newCluster.add(n, data[n]);
				}
			}
		}
	}
	
	private static double likelihood()
	{
		double likelihood = 0;
		
		for (int n = 0; n < data.length; n++)
		{
			Cluster cluster = clusters.get(Z[n]);
			LUDecomposition det = new LUDecomposition(cluster.covariances);
			
			likelihood -= (0.5 * N * Math.log(2.0 * Math.PI) + 0.5 * Math.log(det.getDeterminant()));
			
			ArrayRealVector mean_var = data[n].subtract(cluster.mean); // TODO should compute self.params[self.z[n]]._X.mean(axis=0) less often
			
			likelihood -= 0.5 * mean_var.dotProduct(det.getSolver().getInverse().operate(mean_var));
			// TODO add the influence of n_components
		}
		
		return likelihood;
		
		
		/*
            log_likelihood -= 0.5 * np.dot(np.dot(mean_var, 
                self.params[self.z[n]].inv_covar()), mean_var.transpose())
		 */
	}
	
	public static double[][] identityMatrix(int d) {
		double[][] matrix = new double[d][d];
		for (int i = 0; i < d; ++i) {
			for (int j = 0; j < d; ++j) {
				if (i == j) {
					matrix[i][j] = 1.0;
				} else {
					matrix[i][j] = 0.0;
				}
			}
		}
		
		return matrix;
	}
	
	private static void readData()
	{
		try 
		{
			Scanner scanner = new Scanner(new FileReader(inputFile));
			String[] split = scanner.nextLine().split(" ");
			D = Integer.parseInt(split[1]);
			N = Integer.parseInt(split[0]);
			data = new ArrayRealVector[N];
			int n = 0;
			while (scanner.hasNextLine())
			{
				split = scanner.nextLine().split(" ");
				double[] array = new double[split.length - 1];
				
				for (int i = 0; i < array.length; i++)
					array[i] = Double.parseDouble(split[i + 1]);
				
				data[n] = new ArrayRealVector(array);
				++n;
			}
			
			Z = new int[data.length];
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
	}
}
