package driver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import data.DataInitializer;
import data.DataLoader;
import data.Multinomial;
import data.Tweet;

/**
 * This class runs the Test algorithm on the tweets
 * from start to finish. The tweets should be read in
 * from a Mongo Database.
 * 
 * This class will save the mapping from Tweet ID to the 
 * Tweet's distribution of topics to be read in the the KMeans 
 * clustering algorithm. The first line should be the number
 * of documents followed by the number of topics. Then each following
 * line should look like: 
 * 
 *     ID Topic1 Topic2 ... TopicK
 * 
 * @author Daniel Deutsch
 */
public class Bigram 
{
	private static String input = "data/input-train.txt";
	
	/** The name of the output file for the distributions of theta. */
	private static String thetaFile = "output/theta";
	
	/** The name of the output file for the distributions of phi. */
	private static String phiFile = "output/phi"; 
	
	private static int iterations = 20;
	private static int burnIn = 15;
	
	/** The parameter of the Dirichlet prior on the per-Tweet topic distributions. */
	private static double alpha = 0.1;

	/** The parameter of the Dirichlet prior on the per-topic word distribution. */
	private static double beta = 0.001;

	/** The number of tweets. */
	private static int M;
	
	/** The size of the vocabulary. */
	private static int V;

	/** The number of possible topics. */
	private static int K = 25;

	/** The distribution over the topics for each Tweet. */
	private static double[][] theta = null;
	private static double[][] thetaSum = null;
	
	/** The distribution over the words for each topic. */
	private static SumVector phi = new SumVector();
	private static SumVector phiSum = new SumVector();
	
	private static List<Double> likelihoods = new ArrayList<Double>();
	
	/** The topic that generated each word in each Tweet. */
	private static int[][] z = null;
	
	/** The List of tweets. */
	private static List<Tweet> tweets = null;
	
	/** The number of words in Tweet d assigned to topic k. */
	private static int[][] n_dk = null;
	
	/** The number of words in Tweet d. */
	private static int[] n_d = null;
	
	/** The number of tokens of word type w assigned to topic k. */
	private static int[][] n_kw = null;
	
	/** The number of tokens assigned to topic k. */
	private static int[] n_k = null;
	
	private static CountVector n_jik = new CountVector();
	
	public static void main(String[] args) throws IOException 
	{
		// read in the command line arguments
		Bigram.parseCommandLineArguments(args);
		
		// load the tweets into memory
//		Test.tweets = DataLoader.loadData(input);
		Bigram.tweets = DataLoader.loadMLData(input);
		
		// initialize the variables
		Bigram.initializeVariables();
		Bigram.initializeCounts();
		
		// begin sampling
		for (int t = 0; t < Bigram.iterations; t++)
		{
			System.out.println(t);
			for (int d = 0; d < tweets.size(); d++)
			{
				Tweet tweet = tweets.get(d);
				
				for (int i = 1; i < tweet.size(); i++)
				{
					String word = tweet.get(i);
					String previous = tweet.get(i - 1);
					
					int k = z[d][i];
					int w = Tweet.vocabulary.get(word);
					int j = Tweet.vocabulary.get(previous);
					
					// update counts to exclude word w_{d,i}
					n_dk[d][k]--;
					n_d[d]--;
					n_kw[k][w]--;
					n_k[k]--;
					n_jik.decrement(j, w, k);
					
					// sample for z
					k = Bigram.sampleZ(d, w, j);
					z[d][i] = k;
					
					// update counts to include the new information
					n_dk[d][k]++;
					n_d[d]++;
					n_kw[k][w]++;
					n_k[k]++;
					n_jik.increment(j, w, k);
				}
			}
			
			
			Bigram.calculateTheta(t);
			Bigram.calculatePhi(t);
			likelihoods.add(Bigram.calculateLikelihood());
		}
		
		Bigram.writeThetaToFile();
		Bigram.writePhiToFile();
		Bigram.writeLikelihoodToFile(likelihoods);
	}
	
	/**
	 * Initializes all of the necessary variables for sampling.
	 */
	private static void initializeVariables()
	{
		Bigram.M = tweets.size();
		Bigram.V = Tweet.vocabulary.size();
		Bigram.theta = DataInitializer.initializeTheta(Bigram.M, Bigram.K);
		Bigram.thetaSum = new double[Bigram.M][Bigram.K];
		Bigram.z = DataInitializer.initializeZ(Bigram.tweets, Bigram.K);
	}
	
	/**
	 * Parses the command line arguments to initialize the parameter values.
	 * @param args The command line arguments.
	 */
	private static void parseCommandLineArguments(String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-alpha"))
				Bigram.alpha = Double.parseDouble(args[i + 1]);
			else if (args[i].equals("-beta"))
				Bigram.beta = Double.parseDouble(args[i + 1]);
			else if (args[i].equals("-K"))
				Bigram.K = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-iterations"))
				Bigram.iterations = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-burn_in"))
				Bigram.burnIn = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-theta_file"))
				Bigram.thetaFile = args[i + 1];
			else if (args[i].equals("-phi_file"))
				Bigram.phiFile = args[i + 1];
		}
	}
	
	/**
	 * Initializes all of the counting arrays that need to be maintained
	 * to their proper values by iterating over all of the words
	 * in all of the tweets.
	 */
	private static void initializeCounts()
	{
		// n_dk, n_d, n_kw, n_k
		n_dk = new int[Bigram.tweets.size()][Bigram.K];
		n_d = new int[Bigram.tweets.size()];
		n_kw = new int[Bigram.K][Tweet.vocabulary.size()];
		n_k = new int[Bigram.K];
		
		for (int d = 0; d < Bigram.tweets.size(); d++)
		{
			Tweet tweet = tweets.get(d);
			
			for (int i = 1; i < tweet.size(); i++)
			{
				String word = tweet.get(i);
				String previous = tweet.get(i - 1);
				
				int w = Tweet.vocabulary.get(word);
				int j = Tweet.vocabulary.get(previous);
				int k = z[d][i];
				
				n_dk[d][k]++;
				n_d[d]++;
				n_kw[k][w]++;
				n_k[k]++;
				n_jik.increment(j, w, k);
			}
		}
	}
	
	/**
	 * Sample for z based on the count statistics that are being kept.
	 * @param d The index of the Tweet.
	 * @param w The index of the corresponding word in the vocabulary.
	 * @return The sampled value for z.
	 */
	private static int sampleZ(int d, int i, int j)
	{
		double[] probabilities = new double[Bigram.K];

		for (int k = 0; k < K; k++)
		{
			int njik = n_jik.get(j, i, k);
			
			probabilities[k] = ((n_dk[d][k] + alpha) / (n_d[d] + K * alpha)) 
					 		 * ((njik + beta) / (n_k[k] + V * beta));
		}
		
		return new Multinomial(probabilities).sample();
	}
	
	/**
	 * Calculates the values for theta given the
	 * current counts.
	 */
	private static void calculateTheta(int t)
	{
		for (int d = 0; d < Bigram.tweets.size(); d++)
		{
			for (int k = 0; k < Bigram.K; k++)
			{
				Bigram.theta[d][k] = (n_dk[d][k] + alpha) / (n_d[k] + K * alpha);
				
				if (t > burnIn)
					Bigram.thetaSum[d][k] += Bigram.theta[d][k];
			}
		}
	}
	
	/**
	 * Calculates the values for phi given the
	 * current counts
	 */
	private static void calculatePhi(int t)
	{
		for (String key : n_jik)
		{
			String[] split = key.split(",");
			int j = Integer.parseInt(split[0]);
			int i = Integer.parseInt(split[1]);
			int k = Integer.parseInt(split[2]);
			
			phi.put(j, i, k, (n_jik.get(k, j, i) + beta) / (n_k[k] + V * beta));
			
			if (t > burnIn)
				phiSum.increment(j, i, k, phi.get(j, i, k));
		}
	}
	
	/**
	 * Computes the current likelihood of the model.
	 * @return The likelihood.
	 */
	private static double calculateLikelihood()
	{
		double likelihood = 0;
		for (int d = 0; d < Bigram.tweets.size(); d++)
		{
			Tweet tweet = Bigram.tweets.get(d);
			for (int w = 1; w < tweet.size(); w++)
			{
				// get the index of the word in the vocabulary
				int i = Tweet.vocabulary.get(tweet.get(w));
				int j = Tweet.vocabulary.get(tweet.get(w - 1));
				double logSum = 0;

				for (int k = 0; k < Bigram.K; k++)
					logSum += theta[d][k] * phi.get(j, i, k);
				
				likelihood += Math.log(logSum);
			}
		}
		
		likelihoods.add(likelihood);
		System.out.println(likelihood);
		return likelihood;
	}
	
	/**
	 * Writes the theta distributions to the file. The first
	 * line is the number of documents then the number of topics.
	 */
	private static void writeThetaToFile()
	{
		// compute the average theta
		for (int d = 0; d < Bigram.tweets.size(); d++)
			theta[d] = Multinomial.normalize(thetaSum[d]);
		
		try
		{
			FileWriter writer = new FileWriter(Bigram.thetaFile);
			writer.write(Bigram.tweets.size() + " " + Bigram.K + "\n");
			
			for (int d = 0; d < tweets.size(); d++)
			{
				StringBuilder builder = new StringBuilder("" + tweets.get(d).id());
				for (int k = 0; k < K; k++)
					builder.append(String.format(" %.13f", theta[d][k]));
				
				writer.write(builder.toString() + "\n");
			}
			
			writer.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the phis to file. The first line is the size of the
	 * vocabulary and the number of topics. Then it is each word
	 * and the value of that word in each topic distribution.
	 */
	private static void writePhiToFile()
	{
		// used to store marginalized values
		double[][] phiCollapsed = new double[V][K];
		
		try 
		{
			FileWriter writer = new FileWriter(Bigram.phiFile);
			writer.write(Tweet.vocabulary.size() + " " + Bigram.K + "\n");
			
			for (String key : phiSum)
			{
				String[] split = key.split(",");
				int j = Integer.parseInt(split[0]);
				int i = Integer.parseInt(split[1]);
				int k = Integer.parseInt(split[2]);
				
				phiCollapsed[i][k] += phiSum.get(j, i, k);
			}
			
			for (int w = 0; w < V; w++)
			{
				writer.write(Tweet.reverseIndex.get(w));
				
				double[] normalized = Multinomial.normalize(phiCollapsed[w]);
				if (normalized[0] == Double.NaN)
					continue;
				
				for (int k = 0; k < K; k++)
					writer.write(" " + normalized[k]);
				writer.write("\n");
			}
			
			writer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private static void writeLikelihoodToFile(List<Double> likelihoods)
	{
		try 
		{
			FileWriter writer = new FileWriter("output/likelihood");

			for (Double likelihood : likelihoods)
				writer.write(likelihood + "\n");
			
			writer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
