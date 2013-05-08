package driver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SumVector implements Iterable<String>
{
	private Map<String, Double> map = new HashMap<String, Double>();
	
	public SumVector()
	{
		
	}
	
	public void increment(String key, double value)
	{
		Double sum = this.map.get(key);
		if (sum == null)
			this.map.put(key, value);
		else
			this.map.put(key, sum + value);
	}
	
	public double get(String key)
	{
		Double sum = this.map.get(key);
		if (sum == null)
			return 0.0;
		else
			return sum;
	}
	
	public void put(String key, double value)
	{
		this.map.put(key, value);
	}
	
	public void increment(int a, int b, double value)
	{
		this.increment(this.getKey(a, b), value);
	}
	
	public double get(int a, int b)
	{
		return this.get(this.getKey(a, b));
	}
	
	public void put(int a, int b, double value)
	{
		this.put(this.getKey(a, b), value);
	}
	
	public void increment(int a, int b, int c, double value)
	{
		this.increment(this.getKey(a, b, c), value);
	}
	
	public double get(int a, int b, int c)
	{
		return this.get(this.getKey(a, b, c));
	}
	
	public void put(int a, int b, int c, double value)
	{
		this.put(this.getKey(a, b, c), value);
	}
	
	private String getKey(int a, int b)
	{
		return a + "," + b;
	}
	
	private String getKey(int a, int b, int c)
	{
		return a + "," + b + "," + c;
	}
	
	public Iterator<String> iterator()
	{
		return this.map.keySet().iterator();
	}
}
