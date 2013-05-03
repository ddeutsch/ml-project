package data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DataLoader 
{
	public static List<Tweet> loadData(String fileName)
	{
		List<Tweet> tweets = new ArrayList<Tweet>();
		
		try 
		{
			Scanner scanner = new Scanner(new FileReader(fileName));
			
			// throw away the first line: "label text"
			scanner.nextLine();
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				int whitespace = line.indexOf("\t");
				
				String label = line.substring(0, whitespace);
				String text = line.substring(whitespace + 1);
				
				tweets.add(new Tweet(label, text));
			}
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		
		return tweets;
	}
	
	public static List<Tweet> loadMLData(String fileName)
	{
		List<Tweet> tweets = new ArrayList<Tweet>();
		
		try
		{
			Scanner scanner = new Scanner(new FileReader(fileName));
			
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				tweets.add(new Tweet(line.charAt(0) + "", line.substring(2)));
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		
		
		
		return tweets;
	}
}






