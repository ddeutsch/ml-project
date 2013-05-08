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
		tweets = DataLoader.loadData(dataFile);
		readAssignments();
		
		for (Integer cluster : clusters.keySet())
		{
			List<Integer> labels = clusters.get(cluster);
			System.out.println(purity(labels));
		}
	}
	
	private static double purity(List<Integer> labels)
	{
		int[] counts = new int[Tweet.labels.size()];
		for (Integer id : labels)
		{
			Tweet tweet = tweets.get(id);
			counts[Tweet.labels.indexOf(tweet.label())]++;
		}
		
		int maxCount = -1;
		for (int i = 0; i < counts.length; i++)
			maxCount = Math.max(maxCount, counts[i]);

		return ((double) maxCount / labels.size());
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
