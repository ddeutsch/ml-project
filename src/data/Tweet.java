package data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tweet 
{
	public static Map<String, Integer> vocabulary = new HashMap<String, Integer>();
	public static Map<Integer, String> reverseIndex = new HashMap<Integer, String>();

	private static int counter = 0;
	
	private int id;
	private String label;
	private String text;
	private List<String> words;
	
	public Tweet(String label, String text)
	{
		this.id = counter++;
		this.label = label;
		this.text = text;
		this.words = Arrays.asList(text.split(" "));
		
		// put the words into the vocabulary
		for (String word : this.words)
		{
			if (!vocabulary.containsKey(word))
			{
				vocabulary.put(word, vocabulary.size());
				reverseIndex.put(reverseIndex.size(), word);
			}
		}
	}
	
	public String label()
	{
		return this.label;
	}
	
	public String text()
	{
		return this.text;
	}
	
	public String get(int i)
	{
		return words.get(i);
	}
	
	public int id()
	{
		return this.id;
	}
	
	public int size()
	{
		return this.words.size();
	}
}
