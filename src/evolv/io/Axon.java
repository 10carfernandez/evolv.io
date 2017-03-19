package evolv.io;

class Axon implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1369840006114199775L;
	final double MUTABILITY_MUTABILITY = 0.7;
	final int mutatePower = 9;
	final double MUTATE_MULTI;

	double weight;
	double mutability;
	final double WEIGHT_LIMIT = 5;
	public Axon(double w, double m) {
		weight = w;
		mutability = m;
		MUTATE_MULTI = Math.pow(0.5, mutatePower);
	}

	public Axon mutateAxon() {
		double mutabilityMutate = Math.pow(0.5, pmRan() * MUTABILITY_MUTABILITY);
		double newWeight = weight + r() * mutability / MUTATE_MULTI;
		if (newWeight < 0){
			newWeight = Math.max(newWeight, -WEIGHT_LIMIT);
		} 
		else {
			newWeight = Math.min(newWeight, WEIGHT_LIMIT);
		}
		return new Axon(newWeight, mutability * mutabilityMutate);
	}

	public double r() {
		return Math.pow(pmRan(), mutatePower);
	}

	public double pmRan() {
		return (Math.random() * 2 - 1);
	}
}