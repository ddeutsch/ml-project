package data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Vector3D implements Iterable<String>
{
	private Map<String, Integer> counts = new HashMap<String, Integer>();
	
	public Vector3D()
	{
		
	}
	
	public void increment(int j, int i, int k)
	{
		String key = this.getKey(j, i, k);
		Integer count = this.counts.get(key);
		
		if (count == null)
			this.counts.put(key, 1);
		else
			this.counts.put(key, count + 1);
	}
	
	public void decrement(int j, int i, int k)
	{
		String key = this.getKey(j, i, k);
		Integer count = this.counts.get(key);
		
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
	
	public int get(int j, int i, int k)
	{
		Integer count = this.counts.get(this.getKey(j, i, k));
		if (count == null)
			return 0;
		return count;
	}
	
	public Iterator<String> iterator()
	{
		return this.counts.keySet().iterator();
	}
	
	private String getKey(int j, int i, int k)
	{
		return j + "," + i + "," + k;
	}
}
