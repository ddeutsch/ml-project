package driver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import data.DataInitializer;
import data.DataLoader;
import data.Multinomial;
import data.Tweet;

public class LDA 
{
	private static String input = "data/train";
	
	/** The name of the output file for the distributions of theta. */
	private static String thetaFile = "output/theta";
	
	/** The name of the output file for the distributions of phi. */
	private static String phiFile = "output/phi"; 
	
	private static int iterations = 20;
	private static int burnIn = 10;
	
	/** The parameter of the Dirichlet prior on the per-Tweet topic distributions. */
	private static double alpha = 0.1;

	/** The parameter of the Dirichlet prior on the per-topic word distribution. */
	private static double beta = 0.001;

	/** The number of tweets. */
	private static int M;

	/** The number of possible topics. */
	private static int K = 15;

	/** The distribution over the topics for each Tweet. */
	private static double[][] theta = null;
	private static double[][] thetaSum = null;
	
	/** The distribution over the words for each topic. */
	private static double[][] phi = null;
	private static double[][] phiSum = null;
	
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
	
	public static void main(String[] args) throws IOException 
	{
		// read in the command line arguments
		LDA.parseCommandLineArguments(args);
		
		// load the tweets into memory
		LDA.tweets = DataLoader.loadData(input);
//		LDA.tweets = DataLoader.loadMLData(input);
		
		// initialize the variables
		LDA.initializeVariables();
		LDA.initializeCounts();
		
		List<Double> likelihoods = new ArrayList<Double>();

		// begin sampling
		for (int t = 0; t < LDA.iterations; t++)
		{
			System.out.println(t);
			for (int d = 0; d < tweets.size(); d++)
			{
				Tweet tweet = tweets.get(d);
				
				for (int i = 0; i < tweet.size(); i++)
				{
					String word = tweet.get(i);
					
					int w = Tweet.vocabulary.get(word);
					int k = z[d][i];
					
					// update counts to exclude word w_{d,i}
					n_dk[d][k]--;
					n_d[d]--;
					n_kw[k][w]--;
					n_k[k]--;
					
					// sample for z
					k = LDA.sampleZ(d, w);
					z[d][i] = k;
					
					// update counts to include the new information
					n_dk[d][k]++;
					n_d[d]++;
					n_kw[k][w]++;
					n_k[k]++;
				}
			}
			
			likelihoods.add(LDA.calculateLikelihood());
			
			if (t > burnIn)
			{
				LDA.calculateTheta();
				LDA.calculatePhi();
			}
		}
		
		LDA.writeThetaToFile();
		LDA.writePhiToFile();
		LDA.writeLikelihoodToFile(likelihoods);
	}
	
	/**
	 * Initializes all of the necessary variables for sampling.
	 */
	private static void initializeVariables()
	{
		LDA.M = tweets.size();
		LDA.theta = DataInitializer.initializeTheta(LDA.M, LDA.K);
		LDA.thetaSum = new double[LDA.M][LDA.K];
		LDA.phi = DataInitializer.initializePhi(LDA.K, Tweet.vocabulary.size());
		LDA.phiSum = new double[LDA.K][Tweet.vocabulary.size()];
		LDA.z = DataInitializer.initializeZ(LDA.tweets, LDA.K);
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
				LDA.alpha = Double.parseDouble(args[i + 1]);
			else if (args[i].equals("-beta"))
				LDA.beta = Double.parseDouble(args[i + 1]);
			else if (args[i].equals("-K"))
				LDA.K = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-iterations"))
				LDA.iterations = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-burn_in"))
				LDA.burnIn = Integer.parseInt(args[i + 1]);
			else if (args[i].equals("-theta_file"))
				LDA.thetaFile = args[i + 1];
			else if (args[i].equals("-phi_file"))
				LDA.phiFile = args[i + 1];
			else if (args[i].equals("-input_file"))
				LDA.input = args[i + 1];
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
		n_dk = new int[LDA.tweets.size()][LDA.K];
		n_d = new int[LDA.tweets.size()];
		n_kw = new int[LDA.K][Tweet.vocabulary.size()];
		n_k = new int[LDA.K];
		
		for (int d = 0; d < LDA.tweets.size(); d++)
		{
			Tweet tweet = tweets.get(d);
			
			for (int i = 0; i < tweet.size(); i++)
			{
				String word = tweet.get(i);
				
				int k = z[d][i];
				int w = Tweet.vocabulary.get(word);
				
				n_dk[d][k]++;
				n_d[d]++;
				n_kw[k][w]++;
				n_k[k]++;
			}
		}
	}
	
	/**
	 * Sample for z based on the count statistics that are being kept.
	 * @param d The index of the Tweet.
	 * @param w The index of the corresponding word in the vocabulary.
	 * @return The sampled value for z.
	 */
	private static int sampleZ(int d, int w)
	{
		int V = Tweet.vocabulary.size();
		double[] probabilities = new double[LDA.K];
		
		for (int k = 0; k < LDA.K; k++)
		{
			probabilities[k] = ((n_dk[d][k] + alpha) / (n_d[d] + K * alpha)) 
							 * ((n_kw[k][w] + beta) / (n_k[k] + V * beta));
		}
		
		// sample
		return new Multinomial(probabilities).sample();
	}
	
	/**
	 * Calculates the values for theta given the
	 * current counts.
	 */
	private static void calculateTheta()
	{
		for (int d = 0; d < LDA.tweets.size(); d++)
		{
			for (int k = 0; k < LDA.K; k++)
			{
				LDA.theta[d][k] = (n_dk[d][k] + alpha) / (n_d[k] + K * alpha);
				LDA.thetaSum[d][k] += LDA.theta[d][k];
			}
		}
	}
	
	/**
	 * Calculates the values for phi given the
	 * current counts
	 */
	private static void calculatePhi()
	{
		int W = Tweet.vocabulary.size();
		
		for (int k = 0; k < LDA.K; k++)
		{
			for (int w = 0; w < W; w++)
			{
				LDA.phi[k][w] = (n_kw[k][w] + beta) / (n_k[k] + W * beta);
				LDA.phiSum[k][w] += LDA.phi[k][w];
			}
		}
	}
	
	/**
	 * Computes the current likelihood of the model.
	 * @return The likelihood.
	 */
	private static double calculateLikelihood()
	{
		double likelihood = 0;
		for (int d = 0; d < LDA.tweets.size(); d++)
		{
			Tweet tweet = LDA.tweets.get(d);
			for (int w = 0; w < tweet.size(); w++)
			{
				// get the index of the word in the vocabulary
				int v = Tweet.vocabulary.get(tweet.get(w));
				double logSum = 0;

				for (int k = 0; k < LDA.K; k++)
					logSum += theta[d][k] * phi[k][v];
				
				likelihood += Math.log(logSum);
			}
		}
		
		return likelihood;
	}
	
	/**
	 * Writes the theta distributions to the file. The first
	 * line is the number of documents then the number of topics.
	 */
	private static void writeThetaToFile()
	{
		// compute the average theta
		for (int d = 0; d < LDA.tweets.size(); d++)
			theta[d] = Multinomial.normalize(thetaSum[d]);
//		for (int d = 0; d < LDA.tweets.size(); d++)
//		{
//			for (int k = 0; k < LDA.K; k++)
//				theta[d][k] = thetaSum[d][k] / (iterations - burnIn);
//		}
		
		try
		{
			FileWriter writer = new FileWriter(LDA.thetaFile);
			writer.write(LDA.tweets.size() + " " + LDA.K + "\n");
			
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
		// compute the average phi
		for (int k = 0; k < LDA.K; k++)
			phi[k] = Multinomial.normalize(phiSum[k]);
//		for (int k = 0; k < LDA.K; k++)
//		{
//			for (int w = 0; w < LDA.tweets.size(); w++)
//				phi[k][w] = phiSum[k][w] / (iterations - burnIn);
//		}
		
		try 
		{
			FileWriter writer = new FileWriter(LDA.phiFile);
			writer.write(Tweet.vocabulary.size() + " " + LDA.K + "\n");
			
			for (String word : Tweet.vocabulary.keySet())
			{
				int v = Tweet.vocabulary.get(word);
				StringBuilder builder = new StringBuilder(word);
				
				for (int k = 0; k < LDA.K; k++)
					builder.append(String.format(" %.13f", phi[k][v]));
				
				writer.write(builder.toString() + "\n");
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
