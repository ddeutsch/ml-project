package data;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
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
		Psi0 = (Array2DRowRealMatrix) Psi0.scalarMultiply(0.1);
		
		this.sampleMean = new ArrayRealVector(D);
		this.sum = new ArrayRealVector(this.mean.getDimension());
		this.squareSum = new Array2DRowRealMatrix(D,D);
		
		this.F = new MultivariateNormalDistribution(mean.getDataRef(), covariances.getData());
		
		//hyperparameters
		kappa0 = 0.0;
		nu0 = D;
	
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
		
		if (n == 0) 
		{
			mean = GMM.baseMean.copy();
			sum = new ArrayRealVector(D);
			squareSum = new Array2DRowRealMatrix(D,D);
			sampleMean = new ArrayRealVector(D);
		} 
		else 
		{
			
			
			
			// kappa_n = self._kappa_0 + self.n_points
			double kappa_n = kappa0 + this.points.size();
			
			// nu = self._nu_0 + self.n_points 
			double nu = nu0 + this.points.size();
			
			// mu = np.matrix(self._sum) / self.n_points
			RealVector mu = this.sum.mapDivide(this.points.size());
			
			// mu_mu_0 = mu - self._mu_0
			RealVector mu_mu_0 = mu.subtract(GMM.baseMean.copy());
			  
			// C = self._square_sum - self.n_points * (mu.transpose() * mu)
			RealMatrix C = this.squareSum.subtract(mu.outerProduct(mu).scalarMultiply(this.points.size()));
			
			// Psi = (self._Psi_0 + C + (self._kappa_0 * self.n_points * mu_mu_0.transpose() * mu_mu_0) / 
			//		(self._kappa_0 + self.n_points))
			RealMatrix Psi = (mu_mu_0.outerProduct(mu_mu_0)).scalarMultiply(kappa0 * this.points.size());
			Psi = Psi.scalarMultiply(1.0 / (kappa0 + this.points.size()));
			Psi = Psi.add(C).add(Psi0);
			
			// self.mean = ((self._kappa_0 * self._mu_0 + self.n_points * mu) 
			//            / (self._kappa_0 + self.n_points))
			mean =  (ArrayRealVector) ((GMM.baseMean.copy().mapMultiply(kappa0)).add(sampleMean.mapMultiply(n))).mapDivide((kappa0 + this.points.size()));
			
			// self.covar = (Psi * (kappa_n + 1)) / (kappa_n * (nu - self.n_var + 1))
			this.covariances = (Array2DRowRealMatrix) Psi.scalarMultiply(kappa_n + 1).scalarMultiply(1.0 / (kappa_n * (nu - D + 1.0)));
			
	        // assert(np.linalg.det(self.covar) != 0)

			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
//			double kappaN = kappa0 + points.size(); 
//			double nu = nu0 + points.size();
//			
//			ArrayRealVector muXmu = (ArrayRealVector) sampleMean.subtract(GMM2.baseMean.copy());
//			Array2DRowRealMatrix C = (Array2DRowRealMatrix) squareSum.subtract( (sampleMean.outerProduct(sampleMean)).scalarMultiply(n));
//			Array2DRowRealMatrix PsiN = (Array2DRowRealMatrix) Psi0.add(C).add( (muXmu.outerProduct(muXmu)).scalarMultiply( ( ((double )kappa0 * n )) / kappaN));
//
//			C = new Array2DRowRealMatrix(D,D);
//			for (int p : this.points) 
//			{
//				ArrayRealVector tmp = data[p].subtract(sampleMean);
//				C = (Array2DRowRealMatrix) C.add(tmp.outerProduct(tmp));
//			}
//			
//			if (n == 1) 
//				this.covariances = Psi0;
//			else 
//			{
//				C = (Array2DRowRealMatrix) C.scalarMultiply(1.0 / (n));
//				this.covariances = C.add(Psi0); //(Array2DRowRealMatrix) PsiN.scalarMultiply( ((double) (kappaN + 1) ) / (kappaN * (nu - D + 1)));
//			}
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
