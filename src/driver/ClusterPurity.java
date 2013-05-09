package driver;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import data.DataLoader;
import data.Tweet;

public class ClusterPurity 
{
	private static String dataFile = "data/train";
	private static String assignmentsFile = "output/assignments";
	
	private static List<Tweet> tweets = null;
	private static Map<Integer, List<Integer>> clusters = new HashMap<Integer, List<Integer>>();
	
	private static int N;
	private static int C;
	
	public static void main(String[] args) 
	{
		readCommandLineArgs(args);
		
		tweets = DataLoader.loadData(dataFile);
		readAssignments();
		
		int numCorrect = 0;
		int total = 0;
		
		for (Integer cluster : clusters.keySet())
		{
			List<Integer> labels = clusters.get(cluster);
			double purity = purity(labels);
			
			if (purity > 0.8)
			{
				// now we want to assign labels
				String majorityLabel = Tweet.labels.get(getMajorityLabel(labels));
				
				for (Integer id : labels)
				{
					Tweet tweet = tweets.get(id);
					if (!tweet.isTrain())
					{
						if (tweet.label().equals(majorityLabel))
							numCorrect++;
						total++;
					}
				}
			}
		}
		
		System.out.println(numCorrect + " / " + total + " correct");
		System.out.println((double) numCorrect / total);
	}
	
	private static void readCommandLineArgs(String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-input_file"))
				dataFile = args[i + 1];
			else if (args[i].equals("-assignments_file"))
				assignmentsFile = args[i + 1];
				
		}
	}
	
	private static int getMajorityLabel(List<Integer> labels)
	{
		int[] counts = new int[Tweet.labels.size()];
		
		for (Integer id : labels)
		{
			if (tweets.get(id).isTrain())
			{
				Tweet tweet = tweets.get(id);
				counts[Tweet.labels.indexOf(tweet.label())]++;
			}
		}
		
		int maxCount = -1;
		int maxLabel = -1;
		for (int i = 0; i < counts.length; i++)
		{
			if (counts[i] > maxCount)
			{
				maxCount = counts[i];
				maxLabel = i;
			}
		}
		
		return maxLabel;
	}
	
	private static double purity(List<Integer> labels)
	{
		int[] counts = new int[Tweet.labels.size()];
		int numTraining = 0;
		
		for (Integer id : labels)
		{
			if (tweets.get(id).isTrain())
			{
				Tweet tweet = tweets.get(id);
				counts[Tweet.labels.indexOf(tweet.label())]++;
				numTraining++;
			}
		}
		
		int maxCount = -1;
		for (int i = 0; i < counts.length; i++)
			maxCount = Math.max(maxCount, counts[i]);

		if (numTraining == 0)
			return 0.0;
		
		return ((double) maxCount / numTraining);
	}
	
	private static void readAssignments()
	{
		try
		{
			Scanner scanner = new Scanner(new FileReader(assignmentsFile));
			
			String[] split = scanner.nextLine().split(" ");
			N = Integer.parseInt(split[0]);
			C = Integer.parseInt(split[1]);
			
			while (scanner.hasNextLine())
			{
				split = scanner.nextLine().split(" ");
				int example = Integer.parseInt(split[0]);
				int cluster = Integer.parseInt(split[1]);
				
				List<Integer> list = clusters.get(cluster);
				if (list == null)
					list = new ArrayList<Integer>();
				
				list.add(example);
				clusters.put(cluster, list);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
