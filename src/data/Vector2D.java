package data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Vector2D implements Iterable<String>
{
	private Map<String, Integer> counts = new HashMap<String, Integer>();
	
	public Vector2D()
	{
		
	}
	
	public void increment(int i, int j)
	{
		String key = this.getKey(i, j);
		Integer count = this.counts.get(key);
		
		if (count == null)
			this.counts.put(key, 1);
		else
			this.counts.put(key, count + 1);
	}
	
	public void decrement(int i, int j)
	{
		String key = this.getKey(i, j);
		Integer count = this.counts.get(key);
		
		if (count == null || count <= 0)
			throw new NullPointerException("Count is less than 0");
		else
			this.counts.put(key, count - 1);
	}
	
	public int get(int i, int j)
	{
		Integer count = this.counts.get(this.getKey(i, j));
		if (count == null)
			return 0;
		return count;
	}
	
	private String getKey(int i, int j)
	{
		return i + "," + j;
	}
	
	public Iterator<String> iterator()
	{
		return this.counts.keySet().iterator();
	}
}
