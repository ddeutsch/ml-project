package data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Vector3DD implements Iterable<String>
{
	private Map<String, Double> counts = new HashMap<String, Double>();
	
	public Vector3DD()
	{
		
	}

	public void put(int j, int i, int k, double value)
	{
		this.counts.put(this.getKey(j, i, k), value);
	}
	
	public void increment(int j, int i, int k)
	{
		this.increment(j, i, k, 1.0);
	}
	
	public void increment(int j, int i, int k, double value)
	{
		String key = this.getKey(j, i, k);
		Double count = this.counts.get(key);
		
		if (count == null)
			this.counts.put(key, value);
		else
			this.counts.put(key, count + value);
	}
	
	public void decrement(int j, int i, int k)
	{
		String key = this.getKey(j, i, k);
		Double count = this.counts.get(key);
		
		if (count == null || count <= 0)
			throw new NullPointerException("Count is less than 0");
		else
		{
			if (count - 1 == 0)
				this.counts.remove(key);
			else
				this.counts.put(key, count - 1);
		}
	}
	
	public double get(int j, int i, int k)
	{
		return this.get(this.getKey(j, i, k));
	}
	
	public double get(String key)
	{
		Double count = this.counts.get(key);
		if (count == null)
			return 0.0;
		return count;
	}
	
	private String getKey(int j, int i, int k)
	{
		return j + "," + i + "," + k;
	}
	
	public Iterator<String> iterator()
	{
		return this.counts.keySet().iterator();
	}
}
