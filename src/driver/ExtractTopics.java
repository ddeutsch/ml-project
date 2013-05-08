package driver;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * This class is responsible for extracting the top words
 * from each topic distribution. The input file should contain
 * each word followed by the value for that word in
 * each distribution of topics. The first line of the input
 * should be two integers, the size of the vocabulary and
 * the number of topics.
 * 
 * @author Daniel Deutsch
 */
public class ExtractTopics 
{
	/** The file to find the topic-word distributions. */
	private static String inputFile = "output/phi";
	
	private static String outputFile = "output/topics";
	
	public static void main(String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-phi_file"))
				inputFile = args[i + 1];
			else if (args[i].equals("-topics_file"))
				outputFile = args[i + 1];
		}
		
		try 
		{
			Scanner scanner = new Scanner(new FileReader(inputFile));
			FileWriter writer = new FileWriter(outputFile);
			
			String[] metadata = scanner.nextLine().split(" ");
			int V = Integer.parseInt(metadata[0]);
			int K = Integer.parseInt(metadata[1]);

			// a mapping from vocabulary index to word
			Map<Integer, String> map = new HashMap<Integer, String>();
			double[][] phi = new double[K][V];
			
			// read in the distributions
			for (int v = 0; v < V; v++)
			{
				String split[] = scanner.nextLine().split("\\s");
				
				// extract the word for this line
				String word = split[0];
				map.put(v, word);
				
				// read the value for each topic
				for (int k = 0; k < K; k++)
					phi[k][v] = Double.parseDouble(split[k + 1]);
			}
			
			writer.write(K + " 10\n");
			
			// display the top 10 words for each topic
			for (int k = 0; k < K; k++)
			{
				// we need to sort by index, so keep two copies of the list
				List<Double> indexList = new ArrayList<Double>();
				List<Double> valueList = new ArrayList<Double>();
				
				for (int v = 0; v < V; v++)
				{
					indexList.add(phi[k][v]);
					valueList.add(phi[k][v]);
				}
				
				// sort the value list
				Collections.sort(valueList, Collections.reverseOrder());
				
				writer.write("Topic " + k + ":\n");
				for (int i = 0; i < 10; i++)
				{
					int index = indexList.indexOf(valueList.get(i));
					indexList.set(index, -1.0);
					
					String word = map.get(index);
					
					writer.write(word + "\n");
				}
				writer.write("\n");
			}
			
			writer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
