package driver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CountVector implements Iterable<String>
{
	private Map<String, Integer> map = new HashMap<String, Integer>();
	
	public CountVector()
	{
		
	}
	
	public void increment(String key)
	{
		Integer count = map.remove(key);
		if (count == null)
			map.put(key, 1);
		else
			map.put(key, count + 1);
	}
	
	public void decrement(String key)
	{
		Integer count = map.remove(key);
		if (count == null)
			throw new NullPointerException("Count is null: " + key);
		
		if (count > 1)
			map.put(key, count - 1);
	}
	
	public int get(String key)
	{
		Integer count = map.get(key);
		if (count == null)
			return 0;
		else
			return count;
	}
	
	public void increment(int a, int b)
	{
		this.increment(this.getKey(a, b));
	}
	
	public void decrement(int a, int b)
	{
		this.decrement(this.getKey(a, b));
	}
	
	public int get(int a, int b)
	{
		return this.get(this.getKey(a, b));
	}
	
	public void increment(int a, int b, int c)
	{
		this.increment(this.getKey(a, b, c));
	}
	
	public void decrement(int a, int b, int c)
	{
		this.decrement(this.getKey(a, b, c));
	}
	
	public int get(int a, int b, int c)
	{
		return this.get(this.getKey(a, b, c));
	}
	
	public Iterator<String> iterator()
	{
		return this.map.keySet().iterator();
	}
	
	private String getKey(int a, int b)
	{
		return a + "," + b;
	}
	
	private String getKey(int a, int b, int c)
	{
		return a + "," + b + "," + c;
	}
}
