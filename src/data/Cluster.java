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
	
	public Cluster(ArrayRealVector mean, Array2DRowRealMatrix covariance, int D)
	{
		this.id = counter++;
		this.D = D;
		
		this.mean = mean;
		this.covariances = covariance;
		
		Psi0 = new Array2DRowRealMatrix(GMM.identityMatrix(D));
		Psi0 = (Array2DRowRealMatrix) Psi0.scalarMultiply(10);
		
		this.F = new MultivariateNormalDistribution(mean.getDataRef(), covariances.getData());
		
		this.sampleMean = new ArrayRealVector(D);
		this.sum = new ArrayRealVector(this.mean.getDimension());
		this.squareSum = new Array2DRowRealMatrix(D,D);
	}
	
	public void add(int n, ArrayRealVector x) {
		sampleMean = (ArrayRealVector) sampleMean.mapMultiplyToSelf(this.points.size()).add(x).mapDivideToSelf(this.points.size() + 1);
		squareSum = (Array2DRowRealMatrix) squareSum.add(x.outerProduct(x));
		sum = sum.add(x);
		this.points.add(n);
	}
	
	private void recomputeSS()
	{
		
		mean = (ArrayRealVector) ( GMM.baseMean.mapMultiplyToSelf(kappa0).add(sampleMean.mapMultiplyToSelf(this.points.size())))
				.mapDivideToSelf((kappa0 + this.points.size()));
		double kappaN = kappa0 + points.size(); 
		
		double nu = nu0 + points.size();
		/*
		 * 
		 * C = self._square_sum - self.n_points * (mu.transpose() * mu)
        Psi = (self._Psi_0 + C + self._kappa_0 * self.n_points
             * mu_mu_0.transpose() * mu_mu_0 / (self._kappa_0 + self.n_points))

        self.mean = ((self._kappa_0 * self._mu_0 + self.n_points * mu) 
                    / (self._kappa_0 + self.n_points))
        self.covar = (Psi * (kappa_n + 1)) / (kappa_n * (nu - self.n_var + 1))
		 */
		
		ArrayRealVector xMu  = sampleMean.subtract(GMM.baseMean);
		Array2DRowRealMatrix C = (Array2DRowRealMatrix) squareSum.subtract( GMM.baseMean.outerProduct(GMM.baseMean).scalarAdd(this.points.size()));
		Array2DRowRealMatrix PsiN = (Array2DRowRealMatrix) xMu.outerProduct(xMu).scalarMultiply((kappaN + 1) / ( kappaN));
				
		PsiN = PsiN.add(Psi0.add(C));
		
		this.covariances = (Array2DRowRealMatrix) PsiN.scalarMultiply((kappaN + 1) / (kappaN * (nu - D + 1)));
	}
	
	public void remove(int n, ArrayRealVector x) {
		sampleMean = (ArrayRealVector) sampleMean.mapMultiplyToSelf(this.points.size()).subtract(x).mapDivideToSelf(this.points.size() - 1);
		squareSum = (Array2DRowRealMatrix) squareSum.subtract(x.outerProduct(x));
		sum = sum.subtract(x);
		this.points.remove(n);
	}
	
	public double pdf(ArrayRealVector x) {
		return this.F.density(x.getDataRef());
	}
}
