package driver;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
	private static String clustersFile = "output/clusters";
	private static String assignmentsFile = "output/assignments";
	
	private static ArrayRealVector[] data;
	private static int[] Z = null;
	private static Map<Integer, Cluster> clusters = new HashMap<Integer, Cluster>();
	
	// dimension
	private static int N;
	private static int D;
	private static int K;
	
	public static ArrayRealVector baseMean;
	private static Array2DRowRealMatrix baseCovariance;

	private static double alpha = 1.0;
	
	private static MultivariateNormalDistribution baseDistribution;
	private static int iterations = 100;
	
	public static void main(String[] args) 
	{
		parseCommandLineArgs(args);
		readData();
		
		//initialize mean to [0,...0]
		baseMean = new ArrayRealVector(D);
		//initialize covariance to identity
		baseCovariance = new Array2DRowRealMatrix(identityMatrix(D)); 
		baseCovariance = (Array2DRowRealMatrix) baseCovariance.scalarMultiply(1);
		
		baseDistribution = new MultivariateNormalDistribution(baseMean.getDataRef(),baseCovariance.getData());
		
		// add first cluster
		
		boolean nonparametric = false;
		
		K = 10;
		for (int k = 0; k < K; ++k) {
			Cluster c = new Cluster(baseMean, baseCovariance, D, data);
			clusters.put(c.id, c);
		}
		
		Random rand = new Random();
		//Randomly assign points to a cluster
		for (int i = 0; i < data.length; i++) {
		//	System.out.println(data[i]);\
			int k = rand.nextInt(K);
			clusters.get(k).add(i, data[i]);
			Z[i] = k;
		}
		
		for (int k : clusters.keySet()) {
			Cluster c = clusters.get(k);
			System.out.println(c.mean);
		}
		
		if (nonparametric) {
			List<Integer> removable = new LinkedList<Integer>(); // to avoid concurrent modification exception
			for (int k : clusters.keySet()) {
				Cluster c = clusters.get(k);
				if (c.points.size() == 0) {
					removable.add(k);
				}
			}
			for (int k : removable) {
				clusters.remove(k);
			}
		}
		
		
		//System.exit(0);
		for (int t = 0; t < iterations; t++)
		{
			
			
			for (int n = 0; n < N; ++n) {
				
				
				Cluster curCluster = clusters.get(Z[n]); // SLOW 
				curCluster.remove(n,data[n]); // SLOW
				
				if (curCluster.points.size() == 0)  {
					if (nonparametric) {
						clusters.remove(Z[n]);				
					}
				}

				List<Integer> clusterOrder = new ArrayList<Integer>();
				
				double[] probs;
				if (nonparametric) {
					probs = new double[clusters.size() + 1];
				} else {
					probs = new double[clusters.size()];
				}
				
				int i = 0;
				for (int key : clusters.keySet()) {
					Cluster tmp = clusters.get(key);
					clusterOrder.add(key);
					if (nonparametric) {
						probs[i] = (((double) ( tmp.points.size() )) / ( alpha + N - 1)) * tmp.pdf(data[n]); 
					} else {
						
						probs[i] = (((double) ( tmp.points.size()  + ((double) alpha) / K )) / ( alpha + N - 1)) * tmp.pdf(data[n]); 

					}
					++i;
				}

				//MAP ESTIMATE OF THE PRIOR. NOT EVALUATING PRIOR PREDICTIVE
				if (nonparametric ) {
					probs[clusters.size()] = ( ( double ) alpha / (alpha + N - 1 )) * baseDistribution.density(data[n].getDataRef());
				}
					

				int sample = new Multinomial(probs).sample();
				//new cluster
				if (nonparametric && sample == clusters.size()) {

					Cluster cluster = new Cluster(baseMean, baseCovariance, D, data);
					cluster.add(n, data[n]);
					clusters.put(cluster.id, cluster);
					Z[n] = cluster.id;
				} else {
					//old cluster
					int clusterId = clusterOrder.get(sample);
					Cluster newCluster = clusters.get(clusterId);
					

					newCluster.add(n, data[n]);

					Z[n] = clusterId;
				}
			
				
			}
		
			
		}
		
		writeAssignments();

	}
	
	private static double likelihood()
	{
		double likelihood = 0;
		
		for (int n = 0; n < data.length; n++)
		{
			Cluster cluster = clusters.get(Z[n]);
			//System.out.println(cluster.covariances);
			LUDecomposition det = new LUDecomposition(cluster.covariances);
			
			likelihood -= (0.5 * N * Math.log(2.0 * Math.PI) + 0.5 * Math.log(det.getDeterminant()));
			
			ArrayRealVector mean_var = data[n].subtract(cluster.mean); // TODO should compute self.params[self.z[n]]._X.mean(axis=0) less often
			
			likelihood -= 0.5 * mean_var.dotProduct(det.getSolver().getInverse().operate(mean_var));
			// TODO add the influence of n_components
		}
	//	System.out.println();
		
		return likelihood;
		
		
		/*
            log_likelihood -= 0.5 * np.dot(np.dot(mean_var, 
                self.params[self.z[n]].inv_covar()), mean_var.transpose())
		 */
	}
	
	private static void parseCommandLineArgs(String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-input_file"))
				inputFile = args[i + 1];
			else if (args[i].equals("-clusters_file"))
				clustersFile = args[i + 1];
			else if (args[i].equals("-assignments_file"))
				assignmentsFile = args[i + 1];
		}
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
	
	private static void writeAssignments()
	{
		System.out.println("FINAL");
		for (int key : clusters.keySet()) {
			Cluster clus = clusters.get(key);
			System.out.println("Mean\t" + clus.mean);
			System.out.println(clus.points.size());
			System.out.println();
		}
		
		try
		{
			FileWriter writer = new FileWriter(assignmentsFile);
			
			// WE NEED TO FIGURE OUT HOW THE NUMBER OF CLUSTERS CHANGES
			writer.write(clusters.size() + " " + data.length + "\n");
			
			for (int key : clusters.keySet())
			{
				Cluster cluster = clusters.get(key);
				for (Integer id : cluster.points)
					writer.write(id + " " + key + "\n");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
