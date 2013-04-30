package driver;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import data.DataInitializer;
import data.DataLoader;
import data.Tweet;

/**
 * This class is responsible for reading in the topic
 * distributions for each Song from a file, then running
 * a K-Means clustering algorithm on the distributions.
 * It should output a file in the format:
 * 
 *     ClusterID|Song1ID|Song2ID|...|SongNID
 *     
 * Depending on the size of the data, we may have to merge
 * some data points together and simply get rid of some
 * songs. The unique Song IDs should still remain the
 * same.
 * 
 * @author Daniel Deutsch
 */
public class KMeans 
{
	private static String tweetsFile = "data/train";
	
	/** The file which contains each Song's topic distributions. */
	private static String distributionFile = "output/theta";
	
	/** The file to write the clusters to. */
	private static String clustersFile = "output/clusters";
	
	/** The file to write the Song assignments to. */
	private static String assignmentFile = "output/assignments";
	
	/** The number of clusters to use. */
	private static int C = 4;
	
	/** The centroids of the clusters. */
	private static double[][] clusters = null;
	
	/** The thetas of the songs, mapping from ID to distribution. */
	private static Map<Integer, double[]> songs = new HashMap<Integer, double[]>();
	
	private static List<List<Integer>> assignments = new ArrayList<List<Integer>>();
	
	/** The number of EM iterations to do. */
	private static int iterations = 1000;
	
	/** The number of Documents. */
	private static int D;
	
	/** The number of topics. */
	private static int K;
	
	/**
	 * This method should run the K-Means clustering algorithm
	 * on the Songs from the Song file based on their respective
	 * distributions in the distribution file.
	 * 
	 * @param args The arguments will be the location of
	 * the topic distributions file, the desired output file name, 
	 * and then K, the number of clusters.
	 */
	public static void main(String[] args) 
	{
		// parse the command line arguments
		KMeans.parseCommandLineArgs(args);
		
		// load the data
		List<Tweet> tweets = DataLoader.loadData(tweetsFile);
		
		try 
		{
			Scanner scanner = new Scanner(new FileReader(KMeans.distributionFile));
			
			// first line has number of Songs then number of topics
			String[] split = scanner.nextLine().split(" ");
			KMeans.D = Integer.parseInt(split[0]);
			KMeans.K = Integer.parseInt(split[1]);
			
			// set up by initializing clusters and randomly assigning documents
			KMeans.readSongs(scanner);
			KMeans.clusters = DataInitializer.initializeClusters(C, K, KMeans.songs);
			
			for (int iteration = 0; iteration < iterations; iteration++)
			{
				KMeans.assignToClusters();
				KMeans.computeCenters();
			}
			
			KMeans.writeClusters();
			KMeans.writeAssignments();
			
			Map<String, Integer> labelIds = new HashMap<String, Integer>();
			int[][] counts = new int[C][C];
			for (int c = 0; c < C; c++)
			{
				List<Integer> ids = assignments.get(c);
				for (Integer id : ids)
				{
					String label = tweets.get(id).label();
					int index = -1;
					if (labelIds.containsKey(label))
						index = labelIds.get(label);
					else
					{
						index = labelIds.size();
						labelIds.put(label, index);
					}
					counts[c][index]++;
				}
			}
			
			for (int c = 0; c < C; c++)
			{
				for (int i = 0; i < C; i++)
					System.out.print(counts[c][i] + " ");
				System.out.println();
			}
			
			for (String label : labelIds.keySet())
			{
				System.out.println(label + " : " + labelIds.get(label));
			}
			
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses any command line arguments to the program.
	 * @param args The arguments.
	 */
	private static void parseCommandLineArgs(String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-theta_file"))
				KMeans.distributionFile = args[i + 1];
			else if (args[i].equals("-clusters_file"))
				KMeans.clustersFile = args[i + 1];
			else if (args[i].equals("-assignments_file"))
				KMeans.assignmentFile = args[i + 1];
			else if (args[i].equals("-C"))
				KMeans.C = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-iterations"))
				KMeans.iterations = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-tweets_file"))
				KMeans.tweetsFile = args[i + 1];
		}
	}
	
	private static void readSongs(Scanner scanner)
	{
		while (scanner.hasNextLine())
		{
			String[] split = scanner.nextLine().split(" ");
			
			int id = Integer.parseInt(split[0]);
			double[] distribution = new double[K];
			
			for (int k = 1; k < split.length; k++)
				distribution[k - 1] = Double.parseDouble(split[k]);
			
			KMeans.songs.put(id, distribution);
		}
	}
	
	private static void assignToClusters()
	{
		// re-initialize the assignments
		KMeans.assignments = new ArrayList<List<Integer>>();
		for (int c = 0; c < C; c++)
			KMeans.assignments.add(new ArrayList<Integer>());
		
		// assign Songs to the closest clusters
		for (Integer id : KMeans.songs.keySet())
		{
			double[] distribution = KMeans.songs.get(id);
			
			double minimumDistance = Double.POSITIVE_INFINITY;
			int minimumLabel = -1;
			
			for (int c = 0; c < C; c++)
			{
				double[] cluster = KMeans.clusters[c];
				double distance = KMeans.distance(distribution, cluster);
				
				if (distance < minimumDistance)
				{
					minimumDistance = distance;
					minimumLabel = c;
				}
			}
			
			KMeans.assignments.get(minimumLabel).add(id);
		}
	}
	
	private static void computeCenters()
	{
		for (int c = 0; c < C; c++)
		{
			List<Integer> ids = KMeans.assignments.get(c);
			double[] average = new double[K];
			
			// compute the sum for each Song
			for (Integer id : ids)
			{
				double[] song = KMeans.songs.get(id);
				
				for (int k = 0; k < K; k++)
					average[k] += song[k]; 
			}
			
			// divide to get the average
			if (ids.size() > 0)
			{
				for (int k = 0; k < K; k++)
					average[k] = average[k] / ids.size();
			}
			
			// update the cluster center
			KMeans.clusters[c] = average;
		}
	}
	
	private static void writeClusters()
	{
		try 
		{
			FileWriter writer = new FileWriter(KMeans.clustersFile);
			writer.write(C + " " + K + "\n");
			
			for (int c = 0; c < C; c++)
			{
				for (int k = 0; k < K; k++)
					writer.write(KMeans.clusters[c][k] + " ");
				writer.write("\n");
			}
			
			writer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private static void writeAssignments()
	{
		try 
		{
			FileWriter writer = new FileWriter(KMeans.assignmentFile);
			writer.write(D + " " + C + "\n");
			
			for (int c = 0; c < C; c++)
			{
				List<Integer> ids = KMeans.assignments.get(c);
				
				for (Integer id : ids)
					writer.write(id + " " + c + "\n");
			}
			
			writer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Calculates the Euclidean distance between two vectors.
	 * @param array1 The first vector.
	 * @param array2 The second vector.
	 * @return The Euclidean distance.
	 */
	public static double distance(double[] array1, double[] array2)
	{
		double distance = 0;
		for (int i = 0; i < array1.length; i++)
			distance += Math.pow(array1[i] - array2[i], 2);
		
		return distance;
	}
}
