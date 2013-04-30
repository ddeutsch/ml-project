package data;

import java.util.Random;

/**
 * This class represents a multinomial distribution. Specifically,
 * it provides the ability to sample from that distribution.
 * 
 * @author Daniel Deutsch
 */
public class Multinomial 
{
	/** The backing distribution. */
	private double[] distribution;
	
	/** The random seed to use to sample. */
	private Random random = new Random();
	
	/**
	 * The constructor for the Multinomial which performs
	 * the normalization step.
	 * @param distribution The base distribution.
	 */
	public Multinomial(double[] distribution)
	{
		this.distribution = distribution;
		
		// calculate the normalizing constant
		double sum = 0;
		for (int i = 0; i < this.distribution.length; i++)
			sum += this.distribution[i];
		
		// normalize
		for (int i = 0; i < this.distribution.length; i++)
			this.distribution[i] = this.distribution[i] / sum;
	}
	
	/**
	 * Samples a random value from the distribution.
	 * @return The value.
	 */
	public int sample()
	{
		double probability = this.random.nextDouble();
		
		double sum = 0;
		int index = 0;
		while (true)
		{
			sum += this.distribution[index];
			if (sum > probability)
				return index;
			
			index++;
		}
		
	}
	
	public static double[] normalize(double[] distribution)
	{
		double[] newDist = new double[distribution.length];
		
		double sum = 0;
		for (int i = 0; i < distribution.length; i++)
			sum += distribution[i];
		
		// normalize
		for (int i = 0; i < distribution.length; i++)
			newDist[i] = distribution[i] / sum;
		
		return newDist;
	}
}
