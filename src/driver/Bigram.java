package driver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import data.DataInitializer;
import data.DataLoader;
import data.Multinomial;
import data.Tweet;
import data.Vector2D;
import data.Vector3D;
import data.Vector3DD;

public class Bigram 
{
	private static String input = "data/input-test.txt";
	
	/** The name of the output file for the distributions of theta. */
	private static String thetaFile = "output/theta";
	
	/** The name of the output file for the distributions of phi. */
	private static String phiFile = "output/phi"; 
	
	private static int iterations = 600;
	private static int burnIn = 500;
	
	/** The parameter of the Dirichlet prior on the per-Tweet topic distributions. */
	private static double alpha = 0.1;

	/** The parameter of the Dirichlet prior on the per-topic word distribution. */
	private static double beta = 0.001;

	/** The number of tweets. */
	private static int M;
	
	/** The vocabulary size. */
	private static int V;

	/** The number of possible topics. */
	private static int K = 15;

	/** The distribution over the topics for each Tweet. */
	private static double[][] theta = null;
	private static double[][] thetaSum = null;
	
	/** The distribution over the words for each topic. */
	private static Vector3DD phi = new Vector3DD();
	private static Vector3DD phiSum = new Vector3DD();
	
	/** The topic that generated each word in each Tweet. */
	private static int[][] z = null;
	
	/** The List of tweets. */
	private static List<Tweet> tweets = null;
	
	/** The number of words in Tweet d assigned to topic k. */
	private static int[][] n_dk = null;
	
	/** The number of words in Tweet d. */
	private static int[] n_d = null;
	
	/** The number of tokens of word type w assigned to topic k. */
	private static Vector2D n_kw = new Vector2D();
	
	/** The number of tokens assigned to topic k. */
	private static int[] n_k = null;
	
	/** The counts of seeing token i assigned to k after token j. */
	private static Vector3D n_jik = new Vector3D();
	
	public static void main(String[] args) throws IOException 
	{
		// read in the command line arguments
		Bigram.parseCommandLineArguments(args);
		
		// load the tweets into memory
//		Bigram.tweets = DataLoader.loadData(input);
		tweets = DataLoader.loadMLData(input);
		
		// initialize the variables
		Bigram.initializeVariables();
		Bigram.initializeCounts();
		
		List<Double> likelihoods = new ArrayList<Double>();

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
					
					int w = Tweet.vocabulary.get(word);
					int k = z[d][i];
					int j = Tweet.vocabulary.get(tweet.get(i - 1));
					
					// update counts to exclude word w_{d,i}
					n_dk[d][k]--;
					n_d[d]--;
					n_kw.decrement(k, w);
					n_k[k]--;

					// bigram counts
					n_jik.decrement(j, w, k);
					
					// sample for z
					k = Bigram.bigramSampleZ(d, w, j);

					z[d][i] = k;
					
					// update counts to include the new information
					n_dk[d][k]++;
					n_d[d]++;
					n_kw.increment(k, w);
					n_k[k]++;
					
					// bigram counts
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
		n_dk = new int[M][K];
		n_d = new int[M];
		n_k = new int[K];
		
		for (int d = 0; d < Bigram.tweets.size(); d++)
		{
			Tweet tweet = tweets.get(d);
			
			for (int i = 1; i < tweet.size(); i++)
			{
				int k = z[d][i];
				int w = Tweet.vocabulary.get(tweet.get(i));
				
				n_dk[d][k]++;
				n_d[d]++;
				n_kw.increment(k, w);
				n_k[k]++;
				
				// update the bigram counts
				int j = Tweet.vocabulary.get(tweet.get(i - 1));
				n_jik.increment(j, w, k);
			}
		}
	}
	
	private static int bigramSampleZ(int d, int i, int j)
	{
		double[] probabilities = new double[K];
		
		for (int k = 0; k < K; k++)
		{
			Integer njik = n_jik.get(j, i, k);
			if (njik == null)
				njik = 0;
			
			probabilities[k] = ((njik + beta) / (n_kw.get(k, i) + V * beta))
							 * ((n_dk[d][k] + alpha) / (n_d[d] + K * alpha));
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
		int W = Tweet.vocabulary.size();
		
		for (String key : n_jik)
		{
			String[] split = key.split(",");
			int j = Integer.parseInt(split[0]);
			int i = Integer.parseInt(split[1]);
			int k = Integer.parseInt(split[2]);
			
			double value = (n_jik.get(j, i, k) + beta) / (n_k[k] + W * beta);
			
			phi.put(j, i, k, value);
			if (t > burnIn)
				phiSum.increment(j, i, k, value);
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

				for (int k = 0; k < K; k++)
					logSum += theta[d][k] * phi.get(j, i, k);
				
				likelihood += Math.log(logSum);
			}
		}
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
		double[][] phi_collapsed = new double[V][K];
		try 
		{
			FileWriter writer = new FileWriter(Bigram.phiFile);
			writer.write(Tweet.vocabulary.size() + " " + Bigram.K + "\n");
			
//			Set<String> cache = new HashSet<String>();
			
			// marginalize over j, the previous word
//			for (String key : phiSum)
//			{
//				String[] split = key.split(",");
//				int j = Integer.parseInt(split[0]);
//				int i = Integer.parseInt(split[1]);
//				int k = Integer.parseInt(split[2]);
//				
//				phi_collapsed[i][k] += phiSum.get(j, i, k);
//
//				
//				
//				
////				if (cache.contains(i + "," + j))
////					continue;
////				else
////					cache.add(i + "," + j);
//				
//				
////				for (int k = 0; k < K; k++)
////					phi_collapsed[i][k] += phi.get(j,i,k);
//			}
//			
//			// print the distributions to the file
//			for (int v = 0; v < V; ++v) 
//			{
//				writer.write(Tweet.reverseIndex.get(v));
//				
//				double[] normalized = Multinomial.normalize(phi_collapsed[v]);
//				
//				if (normalized[0] == Double.NaN)
//					continue;
//				
//				for (int k = 0; k < K; ++k)
//					writer.write("\t" + normalized[k]);
//
//				writer.write("\n");
//			}
			
			
//			Set<String> cache = new HashSet<String>();
//			for (String key : phiSum)
//			{
//				String[] split = key.split(",");
//				int j = Integer.parseInt(split[0]);
//				int i = Integer.parseInt(split[1]);
//
//				if (cache.contains(i + "," + j))
//					continue;
//				else
//					cache.add(i + "," + j);
//				
//				double[] dist = new double[K];
//				for (int k = 0; k < K; k++)
//					dist[k] = phiSum.get(j, i, k);
//				dist = Multinomial.normalize(dist);
//				
//				writer.write(String.format("%s#%s", Tweet.reverseIndex.get(j), Tweet.reverseIndex.get(i)));
//				
//				for (int k = 0; k < K; k++)
//					writer.write("\t" + dist[k]);
//				writer.write("\n");
//			}
			
			
			for (int v = 0; v < V; v++)
			{
				writer.write(Tweet.reverseIndex.get(v));
				
				double[] dist = new double[K];
				for (int k = 0; k < K; k++)
					dist[k] = n_kw.get(k, v);
				
				dist = Multinomial.normalize(dist);
				
				for (int k = 0; k < K; k++)
					writer.write("\t" + dist[k]);
				
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
