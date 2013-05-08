package data;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import driver.GMM;

public class Cluster 
{
	private static int counter = 0;
	private static double kappa0 = 0;
	private static double nu0 = 0;
	
	private ArrayRealVector[] data;
	
	public int id;
	public ArrayRealVector mean = null;
	public ArrayRealVector sampleMean = null;
	
	public Array2DRowRealMatrix covariances = null;
	public Set<Integer> points = new HashSet<Integer>();

	private MultivariateNormalDistribution F; 

	
	private ArrayRealVector sum = null;
	private Array2DRowRealMatrix squareSum = new Array2DRowRealMatrix();

	private Array2DRowRealMatrix Psi0 = new Array2DRowRealMatrix();
	
	private static int D;
	
	public Cluster(ArrayRealVector mean, Array2DRowRealMatrix covariance, int D, ArrayRealVector[] data)
	{
	
		this.data = data;
		this.id = counter++;
		this.D = D;
		this.mean = mean;
		this.covariances = covariance;
		
		Psi0 = new Array2DRowRealMatrix(GMM.identityMatrix(D));
		Psi0 = (Array2DRowRealMatrix) Psi0.scalarMultiply(1);
		
		this.sampleMean = new ArrayRealVector(D);
		this.sum = new ArrayRealVector(this.mean.getDimension());
		this.squareSum = new Array2DRowRealMatrix(D,D);
		
		this.F = new MultivariateNormalDistribution(mean.getDataRef(), covariances.getData());
		
	
	}
	
	public void add(int n, ArrayRealVector x) {
	
		sampleMean = (ArrayRealVector) ((sampleMean.mapMultiply(this.points.size())).add(x)).mapDivide(this.points.size() + 1);
		squareSum = (Array2DRowRealMatrix) squareSum.add(x.outerProduct(x));
		sum = sum.add(x);
			
		this.points.add(n);
		recomputeSS();
	}
	
	public void recomputeSS()
	{
		
		int n = points.size();
		
		if (n == 0) {
			mean = GMM.baseMean.copy();
			sum = new ArrayRealVector(D);
			squareSum = new Array2DRowRealMatrix(D,D);
			sampleMean = new ArrayRealVector(D);
		} else {
			
			mean =  (ArrayRealVector) (( GMM.baseMean.copy().mapMultiply(kappa0)).add(sampleMean.mapMultiply(this.points.size()))).mapDivide((kappa0 + this.points.size()));
			
			
			double kappaN = kappa0 + points.size(); 

			double nu = nu0 + points.size();
			
			Array2DRowRealMatrix mu =  (Array2DRowRealMatrix) (sum.outerProduct(sum)).scalarMultiply(n);
			Array2DRowRealMatrix C = (Array2DRowRealMatrix) squareSum.subtract( mu);
			Array2DRowRealMatrix PsiN = Psi0.add(C);

			System.out.println(PsiN);
			this.covariances = PsiN;
		}
			
		this.F = new MultivariateNormalDistribution(mean.getDataRef(), covariances.getData());

		
		
	}
	
	public void remove(int n, ArrayRealVector x) {
	
		sampleMean = (ArrayRealVector) ((sampleMean.mapMultiply(this.points.size())).subtract(x)).mapDivide(this.points.size() - 1);
		squareSum = (Array2DRowRealMatrix) squareSum.subtract(x.outerProduct(x));
		sum = sum.subtract(x);
		this.points.remove(n);
	
		
		recomputeSS();
	}
	
	public double pdf(ArrayRealVector x) {
		return this.F.density(x.getDataRef());
	}
}
