package data;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DataInitializer 
{
	private static Random random = new Random();
	
	public static int[] initializeGamma(int numberOfSongs, int numberOfGenres)
	{
		// there is one genre per song
		int[] gamma = new int[numberOfSongs];
		
		// generate a random genre for each song
		for (int i = 0; i < gamma.length; i++)
			gamma[i] = random.nextInt(numberOfGenres);
		
		return gamma;
	}
	
	public static double[][] initializeTheta(int numberOfSongs, int numberOfTopics)
	{
		// each song has a distribution over topics
		double[][] theta = new double[numberOfSongs][numberOfTopics];
		
		for (int song = 0; song < numberOfSongs; song++)
		{
			for (int k = 0; k < numberOfTopics; k++)
			{
				// initalize to the uniform prior
				theta[song][k] = 1.0 / numberOfTopics;
			}
		}
		
		return theta;
	}
	
	public static double[][] initializePhi(int numberOfTopics, int vocabularySize)
	{
		// each topic has a distribution over the words
		double[][] phi = new double[numberOfTopics][vocabularySize];
		
		for (int k = 0; k < numberOfTopics; k++)
		{
			for (int v = 0; v < vocabularySize; v++)
				phi[k][v] = 1.0 / vocabularySize;
		}
		
		return phi;
	}
	
	public static int[][] initializeZ(List<Tweet> tweets, int numberOfTopics)
	{
		// create a jagged array for each song
		int[][] z = new int[tweets.size()][0];
		
		for (int s = 0; s < tweets.size(); s++)
		{
			Tweet tweet = tweets.get(s);
			z[s] = new int[tweet.size()];
			
			for (int w = 0; w < z[s].length; w++)
				z[s][w] = random.nextInt(numberOfTopics);
		}
		
		return z;
	}
	
	public static double[][] initializeClusters(int C, int K, Map<Integer, double[]> map)
	{
		double[][] clusters = new double[C][K];
		
		Set<Integer> set = new HashSet<Integer>();
		for (int c = 0; c < C; c++)
		{
			int index = random.nextInt(map.size());
			while (set.contains(index))
				index = random.nextInt(map.size());
			
			clusters[c] = map.get(index);
			set.add(index);
		}
		
		return clusters;
	}
}
