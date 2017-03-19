package evolv.io;

import java.util.ArrayList;

import processing.core.PFont;

class Brain implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4169230432277897483L;
	/**
	 * 
	 */
	Board board;
	// Brain
	final int BRAIN_WIDTH = 3;
	final int MEMORY_COUNT = 1;
	final int EYE_COUNT = Creature.EYE_COUNT; 
	final int BASE_HEIGHT = 3*EYE_COUNT + 3;	// Hue, sat, bright for each eye and the extra 3 are mouthHue, body size, and age (excluded)
	final int BRAIN_HEIGHT = BASE_HEIGHT + MEMORY_COUNT + 1;	// the extra 1 is for the constant
	final double AXON_START_MUTABILITY = 0.0005;
	final double STARTING_AXON_VARIABILITY = 1.0;
	Axon[][][] axons;
	double[][] neurons;

	// labels
	String[] inputLabels = new String[BRAIN_HEIGHT];
	String[] outputLabels = new String[BRAIN_HEIGHT];
	
	// Other
	private static double NUM_ELEMENTS = 1000;
	public static double[] SIGMOID_ARRAY = Brain.sigmoidArray(NUM_ELEMENTS);
	public static double[] TANH_ARRAY = Brain.tanhArray(NUM_ELEMENTS);
	public static double[] TANH2_ARRAY = Brain.tanh2Array(NUM_ELEMENTS);
	//public float scale = (float)(20/BRAIN_HEIGHT);
	public float scale = 15f/((float)BRAIN_HEIGHT+(float)BRAIN_WIDTH);

	public Brain(Board board, Axon[][][] tbrain, double[][] tneurons) {
		// initialize brain
		this.board = board;
		if (tbrain == null) {
			axons = new Axon[BRAIN_WIDTH - 1][BRAIN_HEIGHT][BRAIN_HEIGHT - 1];
			neurons = new double[BRAIN_WIDTH][BRAIN_HEIGHT];
			for (int x = 0; x < BRAIN_WIDTH - 1; x++) {
				for (int y = 0; y < BRAIN_HEIGHT; y++) {
					for (int z = 0; z < BRAIN_HEIGHT - 1; z++) {
						double startingWeight = -1;
						double rn = Math.random();
						if (rn > 0.33 && rn <= 0.66){
							startingWeight = 0;
						}
						else if (rn > 0.66){
							startingWeight = 1;
						}
						axons[x][y][z] = new Axon(startingWeight, AXON_START_MUTABILITY);
					}
				}
			}
			neurons = new double[BRAIN_WIDTH][BRAIN_HEIGHT];
			for (int x = 0; x < BRAIN_WIDTH; x++) {
				for (int y = 0; y < BRAIN_HEIGHT; y++) {
					if (y == BRAIN_HEIGHT - 1) {
						neurons[x][y] = 1;
					} else {
						neurons[x][y] = 0;
					}
				}
			}
		} else {
			axons = tbrain;
			neurons = tneurons;
		}

		// initialize labels
		String baseInput[] = new String[BASE_HEIGHT];
		String baseOutput[] = new String[BASE_HEIGHT];
		
		// prevent null pointer exception errors
		for(int i = 0; i < BASE_HEIGHT; i++){
			baseInput[i] = "";
			baseOutput[i] = "";
		}
		
		for (int i = 0; i < EYE_COUNT*3; i+=3){
			baseInput[i] = Integer.toString(i/3) + "H";
			baseInput[i+1] = Integer.toString(i/3) + "S";
			baseInput[i+2] = Integer.toString(i/3) + "B";
		}
		
		baseInput[EYE_COUNT*3] = "Size";
		baseInput[EYE_COUNT*3 + 1] = "MHue";
		//baseInput[EYE_COUNT*3 + 2] = "Age";
		
		String[] tempBaseOutput = { "BHue", "Accel.", "Turn", "Eat", "Fight", "Birth", "MHue" };
		
		int tempLength = tempBaseOutput.length;
		// Setup the basic labels
		for (int i = 0; i < tempLength; i++){
			baseOutput[i] = tempBaseOutput[i];
		}
		
		// Setup the vision distance and angle labels
		for(int i = 2; i < EYE_COUNT*2; i+=2){
			baseOutput[i + tempLength - 2] = "EA" + Integer.toString(i/2);
			baseOutput[i + tempLength - 1] = "ED" + Integer.toString(i/2);
		}

		for (int i = 0; i < baseInput.length; i++) {
			inputLabels[i] = baseInput[i];
			outputLabels[i] = baseOutput[i];
		}
		for (int i = 0; i < MEMORY_COUNT; i++) {
			inputLabels[i + BASE_HEIGHT] = "mem.";
			outputLabels[i + BASE_HEIGHT] = "mem.";
		}
		//inputLabels[MEMORY_COUNT + BASE_HEIGHT] = "age";
		//outputLabels[MEMORY_COUNT + BASE_HEIGHT] = "";
		inputLabels[BRAIN_HEIGHT - 1] = "const.";
		outputLabels[BRAIN_HEIGHT - 1] = "const.";
	}
	
	public double smallestWeight(){
		double min = Math.abs(this.axons[0][0][0].weight);
		for (int x = 1; x < BRAIN_WIDTH - 1; x++) {
			for (int y = 1; y < BRAIN_HEIGHT; y++) {
				for (int z = 1; z < BRAIN_HEIGHT - 1; z++) {
					if(Math.abs(this.axons[x][y][z].weight) < min){
						min = Math.abs(this.axons[x][y][z].weight);
					}
				}
			}
		}
		return min;
	}
	
	public double biggestWeight(){
		double max = Math.abs(this.axons[0][0][0].weight);
		for (int x = 1; x < BRAIN_WIDTH - 1; x++) {
			for (int y = 1; y < BRAIN_HEIGHT; y++) {
				for (int z = 1; z < BRAIN_HEIGHT - 1; z++) {
					if(Math.abs(this.axons[x][y][z].weight) > max){
						max = Math.abs(this.axons[x][y][z].weight);
					}
				}
			}
		}
		return max;
	}
	
	public double smallestNeuron(){
		double max = this.smallestWeight();
		for (int x = 1; x < BRAIN_WIDTH; x++) {
			for (int y = 0; y < BRAIN_HEIGHT - 1; y++) {
				double total = 0;
				for (int input = 0; input < BRAIN_HEIGHT; input++) {
					total += neurons[x - 1][input] * axons[x - 1][input][y].weight;
				}
				if (total < max){
					max = total;
				}
			}
		}
		return max;
	}
	
	public double biggestNeuron(){
		double max = this.biggestWeight();
		for (int x = 1; x < BRAIN_WIDTH; x++) {
			for (int y = 0; y < BRAIN_HEIGHT - 1; y++) {
				double total = 0;
				for (int input = 0; input < BRAIN_HEIGHT; input++) {
					total += neurons[x - 1][input] * axons[x - 1][input][y].weight;
				}
				if (total > max){
					max = total;
				}
			}
		}
		return max;
	}
	
	public double minNeuron(){
		double max = this.smallestWeight();
		for (int x = 1; x < BRAIN_WIDTH; x++) {
			for (int y = 0; y < BRAIN_HEIGHT - 1; y++) {
				double total = 0;
				for (int input = 0; input < BRAIN_HEIGHT; input++) {
					total += neurons[x - 1][input] * axons[x - 1][input][y].weight;
				}
				if (total < max){
					max = total;
				}
			}
		}
		return max;
	}

	// this would be a static method, but processing doesn't like mixing
	// types
	public Brain evolve(ArrayList<Creature> parents) {
		int parentsTotal = parents.size();
		Axon[][][] newBrain = new Axon[BRAIN_WIDTH - 1][BRAIN_HEIGHT][BRAIN_HEIGHT - 1];
		double[][] newNeurons = new double[BRAIN_WIDTH][BRAIN_HEIGHT];
		float randomParentRotation = board.evolvioColor.random(0, 1);
		for (int x = 0; x < BRAIN_WIDTH - 1; x++) {
			for (int y = 0; y < BRAIN_HEIGHT; y++) {
				for (int z = 0; z < BRAIN_HEIGHT - 1; z++) {
					float axonAngle = EvolvioColor.atan2((y + z) / 2.0f - BRAIN_HEIGHT / 2.0f, x - BRAIN_WIDTH / 2) / (2 * EvolvioColor.PI)
							+ EvolvioColor.PI;
					Brain parentForAxon = parents
							.get((int) (((axonAngle + randomParentRotation) % 1.0f) * parentsTotal)).brain;
					newBrain[x][y][z] = parentForAxon.axons[x][y][z].mutateAxon();
				}
			}
		}
		for (int x = 0; x < BRAIN_WIDTH; x++) {
			for (int y = 0; y < BRAIN_HEIGHT; y++) {
				float axonAngle = EvolvioColor.atan2(y - BRAIN_HEIGHT / 2.0f, x - BRAIN_WIDTH / 2) / (2 * EvolvioColor.PI) + EvolvioColor.PI;
				Brain parentForAxon = parents
						.get((int) (((axonAngle + randomParentRotation) % 1.0f) * parentsTotal)).brain;
				newNeurons[x][y] = parentForAxon.neurons[x][y];
			}
		}
		return new Brain(this.board, newBrain, newNeurons);
	}

	public void draw(PFont font, float scaleUp, int mX, int mY) {
		mX/=scale;
		mY/=scale;
		final float neuronSize = 0.4f*scale;
		board.evolvioColor.noStroke();
		board.evolvioColor.fill(0, 0, 0.4f);
		board.evolvioColor.rect((-1.7f - neuronSize) * scaleUp*scale, -neuronSize * scaleUp*scale, (2.4f + BRAIN_WIDTH + neuronSize * 2) * scaleUp*scale,
				(BRAIN_HEIGHT + neuronSize * 2) * scaleUp*scale);

		board.evolvioColor.ellipseMode(EvolvioColor.RADIUS);
		board.evolvioColor.strokeWeight(2);
		board.evolvioColor.textFont(font, 0.58f * scaleUp*scale);
		board.evolvioColor.fill(0, 0, 1);
		for (int y = 0; y < BRAIN_HEIGHT; y++) {
			board.evolvioColor.textAlign(EvolvioColor.RIGHT);
			board.evolvioColor.text(inputLabels[y], (-neuronSize - 0.1f) * scaleUp, (y + (neuronSize * 0.6f)) * scaleUp*scale);
			board.evolvioColor.textAlign(EvolvioColor.LEFT);
			board.evolvioColor.text(outputLabels[y], (BRAIN_WIDTH - 1 + neuronSize + 0.1f) * scaleUp*scale,
					(y + (neuronSize * 0.6f)) * scaleUp*scale);
		}
		board.evolvioColor.textAlign(EvolvioColor.CENTER);
		for (int x = 0; x < BRAIN_WIDTH; x++) {
			for (int y = 0; y < BRAIN_HEIGHT; y++) {
				board.evolvioColor.noStroke();
				double val = neurons[x][y];
				board.evolvioColor.fill(neuronFillColor(val));
				board.evolvioColor.ellipse(x * scaleUp*scale, y * scaleUp*scale, neuronSize * scaleUp*scale, neuronSize * scaleUp*scale);
				board.evolvioColor.fill(neuronTextColor(val));
				board.evolvioColor.text(EvolvioColor.nf((float) val, 0, 1), x * scaleUp*scale, (y + (neuronSize * 0.6f)) * scaleUp*scale);
			}
		}
		if (mX >= 0 && mX < BRAIN_WIDTH && mY >= 0 && mY < BRAIN_HEIGHT*scale) {
			for (int y = 0; y < BRAIN_HEIGHT; y++) {
				if (mX >= 1 && mY < (BRAIN_HEIGHT - 1)) {
					drawAxon(mX - 1, y, mX, mY, scaleUp*scale);
				}
				if (mX < BRAIN_WIDTH - 1 && y < BRAIN_HEIGHT - 1) {
					drawAxon(mX, mY, mX + 1, y, scaleUp*scale);
				}
			}
		}
	}

	public void input(double[] inputs) {
		int end = BRAIN_WIDTH - 1;
		for (int i = 0; i < BASE_HEIGHT; i++) {
			neurons[0][i] = inputs[i];
		}
		for (int i = 0; i < MEMORY_COUNT; i++) {
			neurons[0][BASE_HEIGHT + i] = neurons[end][BASE_HEIGHT + i];
		}
		neurons[0][BRAIN_HEIGHT - 1] = 1;
		for (int x = 1; x < BRAIN_WIDTH; x++) {
			for (int y = 0; y < BRAIN_HEIGHT - 1; y++) {
				double total = 0;
				for (int input = 0; input < BRAIN_HEIGHT; input++) {
					total += neurons[x - 1][input] * axons[x - 1][input][y].weight;
				}
				if (x == BRAIN_WIDTH - 1) {
					neurons[x][y] = total;
				} else {
					neurons[x][y] = activationFunction(total);
				}
			}
		}
	}

	public double[] outputs() {
		int end = BRAIN_WIDTH - 1;
		double[] output = new double[BASE_HEIGHT];
		for (int i = 0; i < BASE_HEIGHT; i++) {
			output[i] = neurons[end][i];
		}
		return output;
	}

	private void drawAxon(int x1, int y1, int x2, int y2, float scaleUp) {
		board.evolvioColor.stroke(neuronFillColor(axons[x1][y1][y2].weight * neurons[x1][y1]));

		board.evolvioColor.line(x1 * scaleUp*scale, y1 * scaleUp*scale, x2 * scaleUp*scale, y2 * scaleUp*scale);
	}
	
	public static double activationFunction(double input){
		return Sigmoid(input);
		//return getSigmoidValue2(input);
		//return sigmoid(input);
		//return Tanh2(input);
		//return Math.tanh(input);
	}

	public static double sigmoid(double input) {
		return 1.0f / (1.0f + Math.pow(2.71828182846f, -input));
	}
	
	public static double[] sigmoidArray(double numEl){
		double sArr[] = new double[(int)numEl];
		double delta = 10/numEl;	// Interval between values from 0 to 10
		for (int i = 0; i < numEl; i++){
			sArr[i] = sigmoid((double)i*delta);
		}
		return sArr;
	}
	
	public static double[] tanhArray(double numEl){
		double sArr[] = new double[(int)numEl];
		double delta = 5/numEl;	// Interval between values from 0 to 10
		for (int i = 0; i < numEl; i++){
			sArr[i] = Math.tanh((double)i*delta);
		}
		return sArr;
	}
	
	public static double[] tanh2Array(double numEl){
		double sArr[] = new double[(int)numEl*2];
		double delta = 5/numEl;	// Interval between values from 0 to 10
		for (int i = 0; i < numEl*2; i++){
			sArr[i] = Math.tanh(-5+(double)i*delta);
		}
		return sArr;
	}
	
	public static double getSigmoidValue(double input){
		if(input >= 0){
			return SIGMOID_ARRAY[(int)(input*NUM_ELEMENTS/10)];
		}
		else{
			return 1 - SIGMOID_ARRAY[-(int)(input*NUM_ELEMENTS/10)];
		}
	}
	
	public static double getSigmoidValue2(double input){
		if(input >= 0){
			return SIGMOID_ARRAY[Math.min((int)(input*NUM_ELEMENTS/10), SIGMOID_ARRAY.length - 1)];
		}
		else{
			return 1 - SIGMOID_ARRAY[Math.min(-(int)(input*NUM_ELEMENTS/10), SIGMOID_ARRAY.length - 1)];
		}
	}
	
	public static double getTanhValue(double input){
		if(input >= 0){
			return TANH_ARRAY[(int)(input*NUM_ELEMENTS/5)];
		}
		else{
			return -TANH_ARRAY[Math.abs((int)(input*NUM_ELEMENTS/5))];
		}
	}
	
	public static double getTanh2Value(double input){
		return TANH2_ARRAY[100+(int)(input*NUM_ELEMENTS/5)];
	}
	
	public static double Sigmoid(double input){
		if (input <= -10){
			return 0;
		}
		else if (input >= 10){
			return 1;
		}
		else return getSigmoidValue(input);
	}
	
	public static double Tanh(double input){
		if (input <= -5){
			return -1;
		}
		else if (input >= 5){
			return 1;
		}
		else return getTanhValue(input);
	}
	
	public static double Tanh2(double input){
		if (input <= -5){
			return -1;
		}
		else if (input >= 5){
			return 1;
		}
		else return getTanh2Value(input);
	}

	private int neuronFillColor(double d) {
		if (d >= 0) {
			return board.evolvioColor.color(0, 0, 1, (float) (d));
		} else {
			return board.evolvioColor.color(0, 0, 0, (float) (-d));
		}
	}

	private int neuronTextColor(double d) {
		if (d >= 0) {
			return board.evolvioColor.color(0, 0, 0);
		} else {
			return board.evolvioColor.color(0, 0, 1);
		}
	}
}