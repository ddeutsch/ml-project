package data;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * This is a utility class responsible for initializing
 * different parameter vectors, like the distribution of topics
 * per Song, the genres of each Song, etc.
 * 
 * @author Daniel Deutsch
 */
public class DataInitializer 
{
	private static Random random = new Random();
	
	/**
	 * Initializes the vector gamma, the genre for each Song.
	 * @param numberOfSongs The total number of Songs.
	 * @param numberOfGenres The total number of genres.
	 * @return The randomized gamma vector.
	 */
	public static int[] initializeGamma(int numberOfSongs, int numberOfGenres)
	{
		// there is one genre per song
		int[] gamma = new int[numberOfSongs];
		
		// generate a random genre for each song
		for (int i = 0; i < gamma.length; i++)
			gamma[i] = random.nextInt(numberOfGenres);
		
		return gamma;
	}
	
	/**
	 * Initializes the theta array, the distribution over topics for each Song
	 * with a uniform prior.
	 * @param numberOfSongs The total number of Songs.
	 * @param numberOfTopics The total number of topics.
	 * @return The uniform theta array.
	 */
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
	
	/**
	 * Initializes the 2D array of phi to the uniform distribution over
	 * all of the words in the vocabulary.
	 * @param numberOfTopics The number of topics.
	 * @param vocabularySize The size of the vocabulary.
	 * @return The uniform phi array.
	 */
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
	
	/**
	 * Initializes the 2D array, z, the topics that generated each word
	 * in each Tweet.
	 * @param songs The List of tweets.
	 * @param numberOfTopics The number of possible topics.
	 * @return The randomly initialized 2D array, z.
	 */
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
	
	/**
	 * Initializes the cluster centers from 0 to 1.
	 * @param C The number of cluster centers. 
	 * @param K The number of topics (the dimension of each cluster).
	 * @return The randomized vectors.
	 */
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
